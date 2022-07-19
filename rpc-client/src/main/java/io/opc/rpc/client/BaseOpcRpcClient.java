package io.opc.rpc.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor;
import io.grpc.stub.StreamObserver;
import io.opc.rpc.api.Connection;
import io.opc.rpc.api.Endpoint;
import io.opc.rpc.api.OpcRpcClient;
import io.opc.rpc.api.OpcRpcStatus;
import io.opc.rpc.api.RequestCallback;
import io.opc.rpc.api.RequestFuture;
import io.opc.rpc.api.RequestHandler;
import io.opc.rpc.api.constant.Constants;
import io.opc.rpc.api.exception.ExceptionCode;
import io.opc.rpc.api.exception.OpcConnectionException;
import io.opc.rpc.api.request.ClientRequest;
import io.opc.rpc.api.request.Request;
import io.opc.rpc.api.request.ServerRequest;
import io.opc.rpc.api.response.ClientResponse;
import io.opc.rpc.api.response.ErrorResponse;
import io.opc.rpc.api.response.Response;
import io.opc.rpc.api.response.ResponseCode;
import io.opc.rpc.api.response.ServerResponse;
import io.opc.rpc.core.RequestCallbackSupport;
import io.opc.rpc.core.connection.ClientGrpcConnection;
import io.opc.rpc.core.connection.ConnectionManager;
import io.opc.rpc.core.grpc.auto.OpcGrpcServiceGrpc;
import io.opc.rpc.core.grpc.auto.Payload;
import io.opc.rpc.core.handle.BaseRequestHandler;
import io.opc.rpc.core.handle.ClientDetectionRequestHandler;
import io.opc.rpc.core.handle.RequestHandlerSupport;
import io.opc.rpc.core.request.ConnectionInitClientRequest;
import io.opc.rpc.core.request.ConnectionResetServerRequest;
import io.opc.rpc.core.request.ConnectionSetupClientRequest;
import io.opc.rpc.core.request.LoginClientRequest;
import io.opc.rpc.core.request.ServerDetectionClientRequest;
import io.opc.rpc.core.response.ConnectionInitServerResponse;
import io.opc.rpc.core.response.ConnectionResetClientResponse;
import io.opc.rpc.core.response.ConnectionSetupServerResponse;
import io.opc.rpc.core.response.LoginServerResponse;
import io.opc.rpc.core.response.ServerDetectionServerResponse;
import io.opc.rpc.core.util.PayloadClassHelper;
import io.opc.rpc.core.util.PayloadObjectHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BaseOpcRpcClient.
 *
 * @author caihongwen
 * @version Id: BaseOpcRpcClient.java, v 0.1 2022年06月02日 22:38 caihongwen Exp $
 */
public abstract class BaseOpcRpcClient implements OpcRpcClient {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    protected final AtomicReference<OpcRpcStatus> rpcClientStatus = new AtomicReference<>(OpcRpcStatus.WAIT_INIT);

    protected long keepActive = Constants.Client.DEFAULT_OPC_RPC_CLIENT_KEEP_ACTIVE;

    /**
     * Client Name.
     */
    protected String clientName;

    /**
     * Client labels.
     */
    protected Map<String, String> labels = new HashMap<>();

    /**
     * Server Endpoint Set.
     */
    protected Set<Endpoint> endpoints;

    protected ThreadPoolExecutor executor;

    protected ScheduledExecutorService scheduledExecutor;

    protected final BlockingQueue<Endpoint> reconnectionSignal = new ArrayBlockingQueue<>(1);

    protected final AtomicInteger connRetryTimes = new AtomicInteger(0);

    /**
     * connection.
     */
    protected volatile Connection currentConnection;

    protected LoginProxy loginProxy;

    @Override
    public void init(@Nonnull Properties properties) {
        if (!this.rpcClientStatus.compareAndSet(OpcRpcStatus.WAIT_INIT, OpcRpcStatus.STARTING)) {
            return;
        }
        this.keepActive = (Long) properties.getOrDefault(Constants.Client.KEY_OPC_RPC_CLIENT_KEEP_ACTIVE,
                Constants.Client.DEFAULT_OPC_RPC_CLIENT_KEEP_ACTIVE);
        // eg: localhost:12345,domain:12344,127.0.0.1:12343
        final String serverAddress = properties.getProperty(Constants.Client.KEY_OPC_RPC_CLIENT_SERVER_ADDRESS);
        Objects.requireNonNull(serverAddress, Constants.Client.KEY_OPC_RPC_CLIENT_SERVER_ADDRESS + " must be set.");
        this.clientName = (String) properties.getOrDefault(Constants.Client.KEY_OPC_RPC_CLIENT_NAME, serverAddress);
        this.endpoints = Endpoint.resolveServerAddress(serverAddress);

        this.executor = createClientExecutor(serverAddress);
        this.scheduledExecutor = new ScheduledThreadPoolExecutor(2, r -> {
            Thread t = new Thread(r);
            t.setName("io.opc.rpc.core.OpcRpcClientScheduler");
            t.setDaemon(true);
            return t;
        });
        this.scheduledExecutor.execute(() -> {
            while (!OpcRpcStatus.SHUTDOWN.equals(this.rpcClientStatus.get())) {
                try {
                    final Endpoint poll = this.reconnectionSignal.poll(this.keepActive, TimeUnit.MICROSECONDS);
                    if (poll != null) {
                        this.reconnect(poll, true);
                    }
                } catch (Exception ignore) {
                    // ignore
                    Thread.currentThread().interrupt();
                }
            }
        });
        // keepActive in client
        this.scheduledExecutor.scheduleWithFixedDelay(() -> {
            if (OpcRpcStatus.SHUTDOWN.equals(this.rpcClientStatus.get())) {
                return;
            }
            // last active timeout will do check with ServerDetectionClientRequest
            if (this.currentConnection != null && ConnectionManager.isActiveTimeout(this.currentConnection, this.keepActive)) {
                try {
                    this.currentConnection.requestAsync(new ServerDetectionClientRequest());
                } catch (OpcConnectionException connEx) {
                    log.warn("[{}]requestBi ServerDetectionClientRequest connEx", this.currentConnection.getConnectionId(), connEx);
                    this.asyncSwitchServerExclude(this.currentConnection.getEndpoint());
                } catch (Exception unknownEx) {
                    log.error("[{}]requestBi ServerDetectionClientRequest error", this.currentConnection.getConnectionId(), unknownEx);
                    this.asyncSwitchServerExclude(this.currentConnection.getEndpoint());
                }
            }
        }, this.keepActive * 2, 1000L, TimeUnit.MILLISECONDS);

        // register ClientDetectionRequestHandler for ClientDetectionServerRequest, tobe a good practice of RequestHandler
        this.registerServerRequestHandler(new ClientDetectionRequestHandler());

        // subclass init
        this.doInit(properties);

        this.loginProxy = new LoginProxy(properties);
        this.scheduleLoginRefreshTask();

        // Finally do connectToServer
        final Endpoint endpoint = Endpoint.randomOne(this.endpoints);
        this.currentConnection = this.connectToServer(endpoint);
        if (this.currentConnection == null) {
            log.error("connect to server failed. do asyncSwitchServerExclude {}.", endpoint.getAddress());
            this.asyncSwitchServerExclude(endpoint);
            this.rpcClientStatus.set(OpcRpcStatus.UNHEALTHY);
        } else {
            this.rpcClientStatus.set(OpcRpcStatus.RUNNING);
            this.doLoginImmediately();
        }
    }

    protected void asyncSwitchServer(@Nullable Endpoint target) {
        if (target != null) {
            this.reconnectionSignal.offer(target);
        } else {
            this.asyncSwitchServerExclude(this.currentConnection.getEndpoint());
        }
    }

    protected void asyncSwitchServerExclude(@Nonnull Endpoint exclude) {
        // switch one endpoint from list(endpoints)
        this.reconnectionSignal.offer(Endpoint.randomOneExclude(this.endpoints, exclude));
    }

    protected void reconnect(@Nonnull final Endpoint endpoint, boolean retryOnFailed) {
        Connection connection = this.connectToServer(endpoint);
        if (connection == null) {
            if (!retryOnFailed) {
                throw new OpcConnectionException();
            }
            try {
                // sleep x milliseconds to switch next server. first round delay 100ms, second round delay 200ms; max delay 5s.
                Thread.sleep(Math.min(this.connRetryTimes.incrementAndGet() * 100L, this.keepActive));
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
            log.error("reconnect to server failed. do asyncSwitchServerExclude {}.", endpoint.getAddress());
            this.asyncSwitchServerExclude(endpoint);
        } else {
            final Connection oldConn = this.currentConnection;
            if (oldConn != null) {
                log.info("reconnect new connection {}, async close old connection {}", connection, oldConn);
                this.scheduledExecutor.execute(oldConn::close);
            }
            this.currentConnection = connection;
            this.connRetryTimes.set(0);
            this.rpcClientStatus.set(OpcRpcStatus.RUNNING);
            this.doLoginImmediately();
        }
    }

    protected Connection connectToServer(@Nonnull final Endpoint endpoint) {
        // Create a communication channel to the server, known as a Channel. Channels are thread-safe
        // and reusable. It is common to create channels at the beginning of your application and reuse
        // them until the application shuts down.
        ManagedChannel channel = ManagedChannelBuilder.forTarget(endpoint.getAddress())
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext()
                .executor(this.executor)
                .intercept(new ClientInterceptor() {
                    @Override
                    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                            CallOptions callOptions, Channel next) {
                        // refresh connection activeTime for client,
                        // but not be called in requestBiStreamObserver.onNext (which not completed)!!
                        ConnectionManager.refreshActiveTime(BaseOpcRpcClient.this.currentConnection);
                        return next.newCall(method, callOptions);
                    }
                })
                //.keepAliveWithoutCalls() replace ServerDetectionClientRequest ?
                .build();
        // future stub
        OpcGrpcServiceGrpc.OpcGrpcServiceFutureStub opcGrpcServiceFutureStub = OpcGrpcServiceGrpc.newFutureStub(channel);
        // biStream stub
        OpcGrpcServiceGrpc.OpcGrpcServiceStub opcGrpcServiceStub = OpcGrpcServiceGrpc.newStub(channel);

        // ConnectionCheck
        final ConnectionInitClientRequest connectionInitRequest = new ConnectionInitClientRequest();
        final ConnectionInitServerResponse connectionInitResponse;
        try {
            ListenableFuture<Payload> future = opcGrpcServiceFutureStub.request(
                    PayloadObjectHelper.buildGrpcPayload(connectionInitRequest));
            connectionInitResponse = PayloadObjectHelper.buildApiPayload(future.get(this.keepActive - 200, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            log.error("connectionInitRequest get error,requestId={}", connectionInitRequest.getRequestId(), e);
            shutdownChanel(channel);
            return null;
        }

        final ClientGrpcConnection grpcConnection = new ClientGrpcConnection(channel, null);
        grpcConnection.setConnectionId(connectionInitResponse.getConnectionId());
        grpcConnection.setClientName(this.clientName);
        grpcConnection.setEndpoint(endpoint);

        final StreamObserver<Payload> responseBiStreamObserver = new ResponseBiStreamObserver(grpcConnection);
        final StreamObserver<Payload> requestBiStreamObserver = opcGrpcServiceStub.requestBiStream(responseBiStreamObserver);
        grpcConnection.setBiStreamObserver(requestBiStreamObserver);

        // do ConnectionSetup
        final ConnectionSetupClientRequest setupClientRequest = ConnectionSetupClientRequest.builder()
                .clientName(this.clientName)
                .labels(this.labels).build();
        grpcConnection.requestAsync(setupClientRequest);

        log.info("connect to server success,connection={} with serverAddress={}", grpcConnection.getConnectionId(), endpoint.getAddress());
        return grpcConnection;
    }

    private void shutdownChanel(ManagedChannel channel) {
        if (channel == null || channel.isShutdown()) {
            return;
        }
        try {
            channel.shutdownNow();
        } catch (Exception ignore) {
            // ignore
        }
    }

    /**
     * doInit on subclass
     *
     * @param properties Properties
     */
    protected abstract void doInit(Properties properties);

    @Override
    public void registerServerRequestHandler(RequestHandler<? extends ServerRequest, ? extends ClientResponse> requestHandler) {

        //noinspection StatementWithEmptyBody
        if (requestHandler instanceof BaseRequestHandler) {
            // noop, because already be registered on construct
        } else {
            java.lang.reflect.ParameterizedType superGenericSuperclass =
                    (java.lang.reflect.ParameterizedType) requestHandler.getClass().getGenericSuperclass();
            //noinspection unchecked
            final Class<? extends Request> requestType = (Class<? extends Request>) superGenericSuperclass.getActualTypeArguments()[0];
            //noinspection unchecked
            final Class<? extends Response> responseType = (Class<? extends Response>) superGenericSuperclass.getActualTypeArguments()[1];

            RequestHandlerSupport.register(requestType, requestHandler);

            PayloadClassHelper.register(requestType, responseType);
        }
    }

    @Override
    public void requestAsync(@Nonnull ClientRequest request, @Nullable RequestCallback<? extends ServerResponse> requestCallback)
            throws OpcConnectionException {
        if (this.currentConnection == null) {
            throw new OpcConnectionException(ExceptionCode.CONNECTION_ERROR);
        } else if (!OpcRpcStatus.RUNNING.equals(this.rpcClientStatus.get())) {
            throw new OpcConnectionException(ExceptionCode.CONNECTION_UNHEALTHY);
        }
        request.putHeaderIfValNonnull(Constants.HEADER_KEY_OPC_ACCESS_TOKEN, this.loginProxy.getAccessToken());
        this.currentConnection.requestAsync(request, requestCallback);
    }

    @Override
    public RequestFuture<ServerResponse> requestFuture(@Nonnull ClientRequest request) throws OpcConnectionException {

        if (this.currentConnection == null) {
            throw new OpcConnectionException(ExceptionCode.CONNECTION_ERROR);
        } else if (!OpcRpcStatus.RUNNING.equals(this.rpcClientStatus.get())) {
            throw new OpcConnectionException(ExceptionCode.CONNECTION_UNHEALTHY);
        }
        request.putHeaderIfValNonnull(Constants.HEADER_KEY_OPC_ACCESS_TOKEN, this.loginProxy.getAccessToken());
        return this.currentConnection.requestFuture(request);
    }

    @Override
    public void close() {
        this.rpcClientStatus.set(OpcRpcStatus.SHUTDOWN);

        if (this.currentConnection != null) {
            log.info("Shutdown Connection {}", this.currentConnection);
            this.currentConnection.close();
        }

        if (this.scheduledExecutor != null && !this.scheduledExecutor.isShutdown()) {
            log.info("Shutdown scheduledExecutor {}", scheduledExecutor);
            this.scheduledExecutor.shutdown();
        }

        // Finally shutdown executor.
        if (this.executor != null && !this.executor.isShutdown()) {
            log.info("Shutdown executor {}", executor);
            this.executor.shutdown();
        }
    }

    protected ThreadPoolExecutor createClientExecutor(String serverAddress) {
        ThreadPoolExecutor opcExecutor = new ThreadPoolExecutor(
                Math.max(2, Runtime.getRuntime().availableProcessors()),
                Math.max(4, Runtime.getRuntime().availableProcessors() * 2),
                10L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10000),
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("opc-rpc-client-executor-" + serverAddress + "-%d")
                        .build());
        opcExecutor.allowCoreThreadTimeOut(true);
        return opcExecutor;
    }

    /**
     * login if username & password not empty
     */
    protected void doLoginImmediately() {
        if (!this.loginProxy.needLogin() || this.currentConnection == null) {
            return;
        }
        this.loginProxy.login();
    }

    /**
     * login if username & password not empty, schedule an async refresh task
     */
    private void scheduleLoginRefreshTask() {
        if (!this.loginProxy.needLogin()) {
            return;
        }
        this.scheduledExecutor.scheduleWithFixedDelay(() -> {
            if (OpcRpcStatus.SHUTDOWN.equals(this.rpcClientStatus.get())) {
                return;
            }
            this.doLoginImmediately();
        }, TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5), TimeUnit.MILLISECONDS);
    }

    public class LoginProxy {

        private final String username;

        private final String password;

        @Getter
        private String accessToken;

        public LoginProxy(Properties properties) {
            this.username = properties.getProperty(Constants.Client.KEY_OPC_RPC_CLIENT_USERNAME);
            this.password = properties.getProperty(Constants.Client.KEY_OPC_RPC_CLIENT_PASSWORD);
        }

        /**
         * login get accessToken.
         *
         * @return accessToken
         */
        public String login() {
            this.accessToken = this.login(this.username, this.password);
            return this.accessToken;
        }

        /**
         * whether we need login.
         */
        public boolean needLogin() {
            return (username != null && !username.isEmpty()) && (password != null && !password.isEmpty());
        }

        /**
         * login get accessToken.
         *
         * @param username username
         * @param password password
         * @return accessToken
         */
        protected String login(String username, String password) {
            final LoginClientRequest loginClientRequest = LoginClientRequest.builder().username(username).password(password).build();
            try {
                final RequestFuture<LoginServerResponse> requestFuture = BaseOpcRpcClient.this.currentConnection
                        .requestFuture(loginClientRequest);
                return requestFuture.get().getAccessToken();
            } catch (OpcConnectionException connEx) {
                log.warn("[{}]requestBi LoginClientRequest connEx", BaseOpcRpcClient.this.currentConnection.getConnectionId(), connEx);
            } catch (Exception unknownEx) {
                log.error("[{}]requestBi LoginClientRequest error", BaseOpcRpcClient.this.currentConnection.getConnectionId(), unknownEx);
            }
            return null;
        }
    }

    /**
     * customize responseBiStreamObserver
     */
    public class ResponseBiStreamObserver implements StreamObserver<Payload> {

        protected final ClientGrpcConnection grpcConnection;

        public ResponseBiStreamObserver(ClientGrpcConnection grpcConnection) {
            this.grpcConnection = grpcConnection;
        }

        @Override
        public void onNext(Payload value) {
            // refresh connection activeTime for client
            grpcConnection.refreshActiveTime();

            final io.opc.rpc.api.Payload payloadObj;
            try {
                payloadObj = PayloadObjectHelper.buildApiPayload(value);
            } catch (Exception e) {
                log.error("[{}] responseBiStreamObserver,payload deserialize error", grpcConnection.getConnectionId(), e);
                ErrorResponse errorResponse = ErrorResponse.build(ResponseCode.FAIL.getCode(), e.getMessage());
                grpcConnection.responseAsync(errorResponse);
                return;
            }
            // ConnectionResetServerRequest
            if (payloadObj instanceof ConnectionResetServerRequest) {
                log.warn("[{}] responseBiStreamObserver receive an ConnectionResetServerRequest,payloadObj={}",
                        grpcConnection.getConnectionId(), payloadObj);
                final ConnectionResetServerRequest connectionResetRequest = (ConnectionResetServerRequest) payloadObj;

                BaseOpcRpcClient.this.asyncSwitchServer(connectionResetRequest.getEndpoint());

                final ConnectionResetClientResponse connectionResetResponse = new ConnectionResetClientResponse();
                connectionResetResponse.setRequestId(connectionResetRequest.getRequestId());
                // do response
                grpcConnection.responseAsync(connectionResetResponse);
            }
            // ServerRequest
            else if (payloadObj instanceof ServerRequest) {
                log.info("[{}] responseBiStreamObserver receive an ServerRequest,payloadObj={}",
                        grpcConnection.getConnectionId(), payloadObj);
                final ServerRequest serverRequest = (ServerRequest) payloadObj;
                Response response = RequestHandlerSupport.handleRequest(serverRequest);
                if (response == null) {
                    response = ErrorResponse.build(ResponseCode.HANDLE_REQUEST_NULL);
                }
                response.setRequestId(serverRequest.getRequestId());
                // do response
                grpcConnection.responseAsync(response);
            }
            // ServerDetectionServerResponse
            else if (payloadObj instanceof ServerDetectionServerResponse) {
                log.debug("[{}] responseBiStreamObserver receive an ServerDetectionServerResponse,payloadObj={}",
                        grpcConnection.getConnectionId(), payloadObj);
            }
            // ConnectionSetupServerResponse
            else if (payloadObj instanceof ConnectionSetupServerResponse) {
                log.debug("[{}] responseBiStreamObserver receive an ConnectionSetupServerResponse,payloadObj={}",
                        grpcConnection.getConnectionId(), payloadObj);
            }
            // ServerResponse
            else if (payloadObj instanceof ServerResponse) {
                log.info("[{}] responseBiStreamObserver receive an ServerResponse,payloadObj={}",
                        grpcConnection.getConnectionId(), payloadObj);
                RequestCallbackSupport.notifyCallback(grpcConnection.getConnectionId(), (ServerResponse) payloadObj);
            }
            // ErrorResponse
            else if (payloadObj instanceof ErrorResponse) {
                log.error("[{}] responseBiStreamObserver receive an ErrorResponse,payloadObj={}",
                        grpcConnection.getConnectionId(), payloadObj);
                RequestCallbackSupport.notifyCallback(grpcConnection.getConnectionId(), (ErrorResponse) payloadObj);
            }
            // unsupported payload
            else {
                log.warn("[{}] responseBiStreamObserver receive unsupported payload,payloadObj={}",
                        grpcConnection.getConnectionId(), payloadObj);
            }
        }

        @Override
        public void onError(Throwable t) {
            log.error("[{}] responseBiStreamObserver on error, do asyncSwitchServerExclude {}.",
                    grpcConnection.getConnectionId(), grpcConnection.getEndpoint().getAddress(), t);
            if (BaseOpcRpcClient.this.rpcClientStatus.compareAndSet(OpcRpcStatus.RUNNING, OpcRpcStatus.UNHEALTHY)) {
                BaseOpcRpcClient.this.asyncSwitchServerExclude(grpcConnection.getEndpoint());
            }
        }

        @Override
        public void onCompleted() {
            if (BaseOpcRpcClient.this.currentConnection == null || BaseOpcRpcClient.this.currentConnection == grpcConnection) {
                log.warn("[{}] responseBiStreamObserver on completed, do asyncSwitchServerExclude {}.",
                        grpcConnection.getConnectionId(), grpcConnection.getEndpoint().getAddress());
                // not normal onCompleted, do asyncSwitchServerExclude
                if (BaseOpcRpcClient.this.rpcClientStatus.compareAndSet(OpcRpcStatus.RUNNING, OpcRpcStatus.UNHEALTHY)) {
                    BaseOpcRpcClient.this.asyncSwitchServerExclude(grpcConnection.getEndpoint());
                }
            } else {
                // normal onCompleted
                log.warn("[{}] responseBiStreamObserver on completed", grpcConnection.getConnectionId());
            }
        }
    }

}
