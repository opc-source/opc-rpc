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
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.opc.rpc.api.OpcRpcClient;
import io.opc.rpc.api.RequestCallback;
import io.opc.rpc.api.constant.OpcConstants;
import io.opc.rpc.api.request.Request;
import io.opc.rpc.api.request.ServerRequest;
import io.opc.rpc.api.response.Response;
import io.opc.rpc.api.response.ResponseCode;
import io.opc.rpc.api.response.ServerResponse;
import io.opc.rpc.core.Endpoint;
import io.opc.rpc.core.RequestCallbackSupport;
import io.opc.rpc.core.connection.ClientGrpcConnection;
import io.opc.rpc.core.connection.Connection;
import io.opc.rpc.core.connection.ConnectionManager;
import io.opc.rpc.core.grpc.auto.OpcGrpcServiceGrpc;
import io.opc.rpc.core.grpc.auto.Payload;
import io.opc.rpc.core.handle.RequestHandlerSupport;
import io.opc.rpc.core.request.ConnectionInitClientRequest;
import io.opc.rpc.core.request.ConnectionResetServerRequest;
import io.opc.rpc.core.request.ConnectionSetupClientRequest;
import io.opc.rpc.core.request.ServerDetectionClientRequest;
import io.opc.rpc.core.response.ConnectionInitServerResponse;
import io.opc.rpc.core.response.ConnectionResetClientResponse;
import io.opc.rpc.core.response.ConnectionSetupServerResponse;
import io.opc.rpc.api.response.ErrorResponse;
import io.opc.rpc.core.response.ServerDetectionServerResponse;
import io.opc.rpc.core.util.PayloadObjectHelper;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    protected long keepActive = OpcConstants.Client.DEFAULT_OPC_RPC_CLIENT_KEEP_ACTIVE;

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

    /**
     * connection.
     */
    protected volatile Connection currentConnection;

    @Override
    public void init(@Nonnull Properties properties) {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        this.keepActive = (Long) properties.getOrDefault(OpcConstants.Client.KEY_OPC_RPC_CLIENT_KEEP_ACTIVE,
                OpcConstants.Client.DEFAULT_OPC_RPC_CLIENT_KEEP_ACTIVE);
        // eg: localhost:12345,domain:12344,127.0.0.1:12343
        final String serverAddress = properties.getProperty(OpcConstants.Client.KEY_OPC_RPC_CLIENT_SERVER_ADDRESS);
        this.clientName = (String) properties.getOrDefault(OpcConstants.Client.KEY_OPC_RPC_CLIENT_NAME, serverAddress);
        this.endpoints = Endpoint.resolveServerAddress(serverAddress);

        this.executor = createClientExecutor(serverAddress);
        this.scheduledExecutor = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r);
            t.setName("io.opc.rpc.core.OpcRpcClientScheduler");
            t.setDaemon(true);
            return t;
        });
        this.scheduledExecutor.scheduleWithFixedDelay(() -> {
            final Endpoint poll = this.reconnectionSignal.poll();
            if (poll != null) {
                reconnect(poll);
            }
        }, 1000L, 1000L, TimeUnit.MILLISECONDS);
        // keepActive in client
        this.scheduledExecutor.scheduleWithFixedDelay(() -> {
            // last active timeout will do check with ServerDetectionClientRequest
            if (this.currentConnection != null && ConnectionManager.isActiveTimeout(this.currentConnection, this.keepActive)) {
                // TODO or requestBi timeout, maybe server busy(apparent death)
                try {
                    this.currentConnection.asyncRequest(new ServerDetectionClientRequest());
                } catch (StatusRuntimeException statusEx) {
                    log.warn("[{}]requestBi ServerDetectionClientRequest statusEx", this.currentConnection.getConnectionId(), statusEx);
                    this.asyncSwitchServerExclude(this.currentConnection.getEndpoint());
                } catch (Exception unknownEx) {
                    log.error("[{}]requestBi ServerDetectionClientRequest error", this.currentConnection.getConnectionId(), unknownEx);
                    this.asyncSwitchServerExclude(this.currentConnection.getEndpoint());
                }
            }
        }, this.keepActive * 2, 1000L, TimeUnit.MILLISECONDS);

        // subclass init
        this.doInit(properties);

        // Finally do connectToServer
        final Endpoint endpoint = Endpoint.randomOne(this.endpoints);
        this.currentConnection = this.connectToServer(endpoint);
        if (this.currentConnection == null) {
            log.error("connect to server failed. do asyncSwitchServerExclude {}.", endpoint.getAddress());
            this.asyncSwitchServerExclude(endpoint);
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

    protected void reconnect(@Nonnull final Endpoint endpoint) {
        Connection connection = this.connectToServer(endpoint);
        if (connection == null) {
            log.error("reconnect to server failed. do asyncSwitchServerExclude {}.", endpoint.getAddress());
            this.asyncSwitchServerExclude(endpoint);
        } else {
            final Connection oldConn = this.currentConnection;
            if (oldConn != null) {
                log.info("reconnect new connection {}, async close old connection {}", connection, oldConn);
                this.scheduledExecutor.execute(oldConn::close);
            }
            this.currentConnection = connection;
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
            connectionInitResponse = PayloadObjectHelper.buildApiPayload(future.get(3000, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            log.error("connectionInitRequest get error,requestId={}", connectionInitRequest.getRequestId(), e);
            shutdownChanel(channel);
            return null;
        }

        final ClientGrpcConnection grpcConnection = new ClientGrpcConnection(channel, null);
        grpcConnection.setConnectionId(connectionInitResponse.getConnectionId());
        grpcConnection.setClientName(this.clientName);
        grpcConnection.setEndpoint(endpoint);

        // customize responseBiStreamObserver
        final StreamObserver<Payload> responseBiStreamObserver = new StreamObserver<Payload>() {

            @Override
            public void onNext(Payload value) {
                // refresh connection activeTime for client
                grpcConnection.refreshActiveTime();

                final io.opc.rpc.api.Payload payloadObj = PayloadObjectHelper.buildApiPayload(value);
                // ConnectionResetServerRequest
                if (payloadObj instanceof ConnectionResetServerRequest) {
                    log.warn("[{}] responseBiStreamObserver receive an ConnectionResetServerRequest,payloadObj={}",
                            grpcConnection.getConnectionId(), payloadObj);
                    final ConnectionResetServerRequest connectionResetRequest = (ConnectionResetServerRequest) payloadObj;

                    BaseOpcRpcClient.this.asyncSwitchServer(connectionResetRequest.getEndpoint());

                    final ConnectionResetClientResponse connectionResetResponse = new ConnectionResetClientResponse();
                    connectionResetResponse.setRequestId(connectionResetRequest.getRequestId());
                    // do response
                    grpcConnection.asyncResponse(connectionResetResponse);
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
                    grpcConnection.asyncResponse(response);
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
                // TODO set status=RpcClientStatus.UNHEALTHY ?
                BaseOpcRpcClient.this.asyncSwitchServerExclude(grpcConnection.getEndpoint());
            }

            @Override
            public void onCompleted() {
                if (BaseOpcRpcClient.this.currentConnection == null || BaseOpcRpcClient.this.currentConnection == grpcConnection) {
                    log.warn("[{}] responseBiStreamObserver on completed, do asyncSwitchServerExclude {}.",
                            grpcConnection.getConnectionId(), grpcConnection.getEndpoint().getAddress());
                    // not normal onCompleted, do asyncSwitchServerExclude
                    BaseOpcRpcClient.this.asyncSwitchServerExclude(grpcConnection.getEndpoint());
                } else {
                    // normal onCompleted
                    log.warn("[{}] responseBiStreamObserver on completed", grpcConnection.getConnectionId());
                }
            }
        };
        final StreamObserver<Payload> requestBiStreamObserver = opcGrpcServiceStub.requestBiStream(responseBiStreamObserver);
        grpcConnection.setBiStreamObserver(requestBiStreamObserver);

        // do ConnectionSetup
        final ConnectionSetupClientRequest setupClientRequest = ConnectionSetupClientRequest.builder()
                .clientName(this.clientName)
                .labels(this.labels).build();
        grpcConnection.asyncRequest(setupClientRequest);

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
    public void asyncRequest(@Nonnull Request request, @Nullable RequestCallback<? extends Response> requestCallback) {
        this.currentConnection.asyncRequest(request, requestCallback);
    }

    @Override
    public void close() {
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

}
