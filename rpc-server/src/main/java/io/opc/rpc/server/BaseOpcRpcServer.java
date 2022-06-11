package io.opc.rpc.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.Attributes;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Grpc;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerTransportFilter;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.grpc.util.MutableHandlerRegistry;
import io.opc.rpc.api.Connection;
import io.opc.rpc.api.Endpoint;
import io.opc.rpc.api.OpcRpcServer;
import io.opc.rpc.api.OpcRpcStatus;
import io.opc.rpc.api.RequestHandler;
import io.opc.rpc.api.constant.OpcConstants;
import io.opc.rpc.api.exception.ExceptionCode;
import io.opc.rpc.api.exception.OpcConnectionException;
import io.opc.rpc.api.exception.OpcRpcRuntimeException;
import io.opc.rpc.api.request.ClientRequest;
import io.opc.rpc.api.request.Request;
import io.opc.rpc.api.response.ClientResponse;
import io.opc.rpc.api.response.ErrorResponse;
import io.opc.rpc.api.response.Response;
import io.opc.rpc.api.response.ResponseCode;
import io.opc.rpc.api.response.ServerResponse;
import io.opc.rpc.core.RequestCallbackSupport;
import io.opc.rpc.core.connection.BaseConnection;
import io.opc.rpc.core.connection.ConnectionManager;
import io.opc.rpc.core.connection.GrpcConnection;
import io.opc.rpc.core.grpc.auto.OpcGrpcServiceGrpc;
import io.opc.rpc.core.grpc.auto.Payload;
import io.opc.rpc.core.handle.BaseRequestHandler;
import io.opc.rpc.core.handle.RequestHandlerSupport;
import io.opc.rpc.core.request.ClientDetectionServerRequest;
import io.opc.rpc.core.request.ConnectionInitClientRequest;
import io.opc.rpc.core.request.ConnectionResetServerRequest;
import io.opc.rpc.core.request.ConnectionSetupClientRequest;
import io.opc.rpc.core.request.ServerDetectionClientRequest;
import io.opc.rpc.core.response.ConnectionInitServerResponse;
import io.opc.rpc.core.response.ConnectionSetupServerResponse;
import io.opc.rpc.core.response.ServerDetectionServerResponse;
import io.opc.rpc.core.util.PayloadClassHelper;
import io.opc.rpc.core.util.PayloadObjectHelper;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BaseOpcRpcServer.
 *
 * @author caihongwen
 * @version Id: BaseOpcRpcServer.java, v 0.1 2022年06月05日 10:35 caihongwen Exp $
 */
public abstract class BaseOpcRpcServer implements OpcRpcServer {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    protected volatile AtomicReference<OpcRpcStatus> rpcServerStatus = new AtomicReference<>(OpcRpcStatus.WAIT_INIT);

    protected long keepActive = OpcConstants.Server.DEFAULT_OPC_RPC_SERVER_KEEP_ACTIVE;

    protected ThreadPoolExecutor executor;

    protected ScheduledThreadPoolExecutor scheduledExecutor;

    protected Server server;

    protected final ConnectionManager connectionManager = new ConnectionManager();

    @Override
    public Collection<Connection> getConnections() {
        return connectionManager.getConnections();
    }

    @Override
    public void init(Properties properties) {
        if (!this.rpcServerStatus.compareAndSet(OpcRpcStatus.WAIT_INIT, OpcRpcStatus.STARTING)) {
            return;
        }

        this.keepActive = (Long) properties.getOrDefault(OpcConstants.Server.KEY_OPC_RPC_SERVER_KEEP_ACTIVE,
                OpcConstants.Server.DEFAULT_OPC_RPC_SERVER_KEEP_ACTIVE);
        final Integer serverPort = (Integer) properties.getOrDefault(OpcConstants.Server.KEY_OPC_RPC_SERVER_PORT,
                OpcConstants.Server.DEFAULT_OPC_RPC_SERVER_PORT);
        this.executor = this.createServerExecutor(serverPort);

        // subclass init
        this.doInit(properties);

        this.server = this.getServer(serverPort);

        try {
            this.server.start();
            this.rpcServerStatus.set(OpcRpcStatus.RUNNING);
        } catch (IOException e) {
            throw new OpcRpcRuntimeException(ExceptionCode.INIT_SERVER_FAIL, e);
        }

        this.scheduledExecutor = new ScheduledThreadPoolExecutor(1,
                r -> new Thread(r, "io.opc.rpc.core.OpcRpcServerScheduler"));
        // keepActive in server
        this.scheduledExecutor.scheduleWithFixedDelay(() -> {
            if (OpcRpcStatus.SHUTDOWN.equals(this.rpcServerStatus.get())) {
                return;
            }
            this.notifyActiveTimeoutConnectionClientDetection();
        }, 1000L, 1000L, TimeUnit.MILLISECONDS);
    }

    /**
     * doInit on subclass
     *
     * @param properties Properties
     */
    protected abstract void doInit(Properties properties);

    /**
     * Inheritance and then customize grpc Server? It's all up to you.
     */
    protected Server getServer(int serverPort) {
        return ServerBuilder.forPort(serverPort).executor(this.executor)
                .addTransportFilter(new WrapAttributeAndConnectionServerTransportFilter())
                .addService(ServerInterceptors.intercept(new OpcGrpcServiceImpl(), new AttributeToContextServerInterceptor()))
                .fallbackHandlerRegistry(this.getFallbackHandlerRegistry())
                .build();
    }

    /**
     * Inheritance and then customize addService? It's all up to you. You also can hold MutableHandlerRegistry for dynamic add Service.
     */
    protected MutableHandlerRegistry getFallbackHandlerRegistry() {
        return new MutableHandlerRegistry();
    }

    /**
     * last active timeout will do check with ClientDetectionServerRequest.
     */
    private void notifyActiveTimeoutConnectionClientDetection() {
        for (Connection connection : BaseOpcRpcServer.this.connectionManager.getActiveTimeoutConnections(this.keepActive)) {
            try {
                connection.asyncRequest(new ClientDetectionServerRequest());
            } catch (OpcConnectionException connEx) {
                log.warn("[{}]Grpc requestBi ClientDetectionServerRequest connEx", connection.getConnectionId(), connEx);
                BaseOpcRpcServer.this.connectionManager.removeAndClose(connection.getConnectionId());
            } catch (Exception unknownEx) {
                log.error("[{}]Grpc requestBi ClientDetectionServerRequest error", connection.getConnectionId(), unknownEx);
                BaseOpcRpcServer.this.connectionManager.removeAndClose(connection.getConnectionId());
            }
        }
    }

    /**
     * notify all connections do ResetServer.
     */
    protected void notifyAllConnectionResetServer() {
        final ConnectionResetServerRequest connectionResetServerRequest = new ConnectionResetServerRequest();
        log.warn("[{}]Grpc requestBi ConnectionResetServerRequest", "notifyAllConnectionResetServer");
        for (Connection connection : BaseOpcRpcServer.this.connectionManager.getConnections()) {
            try {
                connection.asyncRequest(connectionResetServerRequest);
            } catch (OpcConnectionException connEx) {
                log.warn("[{}]Grpc requestBi ConnectionResetServerRequest connEx", connection.getConnectionId(), connEx);
            } catch (Exception unknownEx) {
                log.error("[{}]Grpc requestBi ConnectionResetServerRequest error", connection.getConnectionId(), unknownEx);
            }
        }
    }

    @Override
    public void registerClientRequestHandler(Class<? extends ClientRequest> requestClass,
            RequestHandler<? extends ClientRequest, ? extends ServerResponse> requestHandler) {
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
    public void close() {
        this.rpcServerStatus.set(OpcRpcStatus.SHUTDOWN);
        this.notifyAllConnectionResetServer();

        if (this.server != null) {
            log.info("Shutdown server {}", this.server);
            try {
                this.server.shutdown();
                BaseOpcRpcServer.this.connectionManager.removeAndCloseAll();
                this.server.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
                this.server.shutdownNow();
            } catch (Exception ignore) {
                this.server.shutdownNow();
            }
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

    protected ThreadPoolExecutor createServerExecutor(int port) {
        return new ThreadPoolExecutor(
                Math.max(2, Runtime.getRuntime().availableProcessors()),
                Math.max(4, Runtime.getRuntime().availableProcessors() * 2),
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10000),
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("opc-rpc-server-executor-" + port + "-%d")
                        .build());
    }

    /**
     * Wrap attributes and deal connection ...
     */
    protected class WrapAttributeAndConnectionServerTransportFilter extends ServerTransportFilter {
        @Override
        public Attributes transportReady(Attributes transportAttrs) {
            InetSocketAddress remoteAddress = (InetSocketAddress) transportAttrs.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
            InetSocketAddress localAddress = (InetSocketAddress) transportAttrs.get(Grpc.TRANSPORT_ATTR_LOCAL_ADDR);
            String remoteIp = remoteAddress.getAddress().getHostAddress();
            int remotePort = remoteAddress.getPort();
            String localIp = localAddress.getAddress().getHostAddress();
            int localPort = localAddress.getPort();

            // eg: 1654890127537_127.0.0.1:59146_127.0.0.1:6666
            final String connectionId = System.currentTimeMillis() + "_" + remoteIp + ":" + remotePort + "_" + localIp + ":" + localPort;
            Attributes attrWrapper = transportAttrs.toBuilder()
                    .set(TRANS_KEY_CONN_ID, connectionId)
                    .set(TRANS_KEY_REMOTE_IP, remoteIp)
                    .set(TRANS_KEY_REMOTE_PORT, remotePort)
                    .set(TRANS_KEY_LOCAL_PORT, localPort).build();

            log.info("Connection transportReady,connectionId={}", connectionId);
            return attrWrapper;
        }

        @Override
        public void transportTerminated(Attributes transportAttrs) {
            try {
                String connectionId = transportAttrs.get(TRANS_KEY_CONN_ID);
                log.info("Connection transportTerminated,connectionId={}", connectionId);
                BaseOpcRpcServer.this.connectionManager.removeAndClose(connectionId);
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    /**
     * server interceptor to set connectionId ...
     */
    static class AttributeToContextServerInterceptor implements ServerInterceptor {
        @Override
        public <T, S> ServerCall.Listener<T> interceptCall(ServerCall<T, S> call, Metadata headers,
                ServerCallHandler<T, S> next) {
            // custom attributes from ServerTransportFilter
            Context ctx = Context.current()
                    .withValue(CONTEXT_KEY_CONN_ID, call.getAttributes().get(TRANS_KEY_CONN_ID))
                    .withValue(CONTEXT_KEY_CONN_REMOTE_IP, call.getAttributes().get(TRANS_KEY_REMOTE_IP))
                    .withValue(CONTEXT_KEY_CONN_REMOTE_PORT, call.getAttributes().get(TRANS_KEY_REMOTE_PORT))
                    .withValue(CONTEXT_KEY_CONN_LOCAL_PORT, call.getAttributes().get(TRANS_KEY_LOCAL_PORT));
            return Contexts.interceptCall(ctx, call, headers, next);
        }
    }

    class OpcGrpcServiceImpl extends OpcGrpcServiceGrpc.OpcGrpcServiceImplBase {

        @Override
        public void request(io.opc.rpc.core.grpc.auto.Payload requestPayload,
                io.grpc.stub.StreamObserver<io.opc.rpc.core.grpc.auto.Payload> responseObserver) {

            final String connectionId = CONTEXT_KEY_CONN_ID.get();
            BaseOpcRpcServer.this.connectionManager.refreshActiveTime(connectionId);
            final io.opc.rpc.api.Payload payloadObj;
            try {
                payloadObj = PayloadObjectHelper.buildApiPayload(requestPayload);
            } catch (Exception e) {
                log.error("[{}]Grpc request,payload deserialize error", connectionId, e);
                ErrorResponse errorResponse = ErrorResponse.build(ResponseCode.FAIL.getCode(), e.getMessage());
                responseObserver.onNext(PayloadObjectHelper.buildGrpcPayload(errorResponse));
                responseObserver.onCompleted();
                return;
            }

            // rpcServerStatus not RUNNING
            if (!OpcRpcStatus.RUNNING.equals(BaseOpcRpcServer.this.rpcServerStatus.get())) {
                log.error("[{}]Grpc request,rpcServerStatus not RUNNING,payloadObj={}", connectionId, payloadObj);
                ErrorResponse errorResponse = ErrorResponse.build(ResponseCode.SERVER_UNHEALTHY);
                if (payloadObj instanceof ClientRequest) {
                    errorResponse.setRequestId(((ClientRequest) payloadObj).getRequestId());
                }
                responseObserver.onNext(PayloadObjectHelper.buildGrpcPayload(errorResponse));
                responseObserver.onCompleted();
                return;
            }

            // ConnectionInitClientRequest
            if (payloadObj instanceof ConnectionInitClientRequest) {
                log.info("[{}]Grpc request,receive an ConnectionInitClientRequest,payloadObj={}", connectionId, payloadObj);
                ConnectionInitClientRequest initRequest = (ConnectionInitClientRequest) payloadObj;

                ConnectionInitServerResponse initResponse = new ConnectionInitServerResponse();
                initResponse.setRequestId(initRequest.getRequestId());
                initResponse.setConnectionId(connectionId);

                responseObserver.onNext(PayloadObjectHelper.buildGrpcPayload(initResponse));
                responseObserver.onCompleted();
            }
            // ClientRequest
            else if (payloadObj instanceof ClientRequest) {
                log.info("[{}]Grpc request,receive an ClientRequest,payloadObj={}", connectionId, payloadObj);
                final ClientRequest clientRequest = (ClientRequest) payloadObj;
                Response response = RequestHandlerSupport.handleRequest(clientRequest);
                if (response == null) {
                    response = ErrorResponse.build(ResponseCode.HANDLE_REQUEST_NULL);
                }
                response.setRequestId(clientRequest.getRequestId());
                responseObserver.onNext(PayloadObjectHelper.buildGrpcPayload(response));
                responseObserver.onCompleted();
            }
            // unsupported payload
            else {
                log.warn("[{}]Grpc request,receive unsupported payload,payloadObj={}", connectionId, payloadObj);
                ErrorResponse errorResponse = ErrorResponse.build(ResponseCode.UNSUPPORTED_PAYLOAD);
                responseObserver.onNext(PayloadObjectHelper.buildGrpcPayload(errorResponse));
            }
        }

        @Override
        public io.grpc.stub.StreamObserver<io.opc.rpc.core.grpc.auto.Payload> requestBiStream(
                io.grpc.stub.StreamObserver<io.opc.rpc.core.grpc.auto.Payload> responseObserver) {

            return new StreamObserver<Payload>() {

                final String connectionId = CONTEXT_KEY_CONN_ID.get();

                final Integer localPort = CONTEXT_KEY_CONN_LOCAL_PORT.get();

                final int remotePort = CONTEXT_KEY_CONN_REMOTE_PORT.get();

                final String remoteIp = CONTEXT_KEY_CONN_REMOTE_IP.get();

                @Override
                public void onNext(Payload requestPayload) {

                    BaseOpcRpcServer.this.connectionManager.refreshActiveTime(connectionId);
                    final io.opc.rpc.api.Payload payloadObj;
                    try {
                        payloadObj = PayloadObjectHelper.buildApiPayload(requestPayload);
                    } catch (Exception e) {
                        log.error("[{}]Grpc request bi stream,payload deserialize error", connectionId, e);
                        ErrorResponse errorResponse = ErrorResponse.build(ResponseCode.FAIL.getCode(), e.getMessage());
                        this.doResponseWithConnectionFirst(errorResponse);
                        return;
                    }

                    // rpcServerStatus not RUNNING
                    if (!OpcRpcStatus.RUNNING.equals(BaseOpcRpcServer.this.rpcServerStatus.get())) {
                        log.error("[{}]Grpc request bi stream,rpcServerStatus not RUNNING,payloadObj={}", connectionId, payloadObj);
                        ErrorResponse errorResponse = ErrorResponse.build(ResponseCode.SERVER_UNHEALTHY);
                        if (payloadObj instanceof ClientRequest) {
                            errorResponse.setRequestId(((ClientRequest) payloadObj).getRequestId());
                        }
                        this.doResponseWithConnectionFirst(errorResponse);
                        return;
                    }

                    // ConnectionSetupClientRequest
                    if (payloadObj instanceof ConnectionSetupClientRequest) {
                        log.info("[{}]Grpc request bi stream,receive an ConnectionSetupClientRequest,payloadObj={}",
                                connectionId, payloadObj);
                        ConnectionSetupClientRequest setupRequest = (ConnectionSetupClientRequest) payloadObj;
                        final String clientName = setupRequest.getClientName();
                        final Map<String, String> labels = setupRequest.getLabels();

                        BaseConnection connection = GrpcConnection.builder().biStreamObserver(responseObserver).build();
                        connection.setConnectionId(connectionId);
                        connection.setEndpoint(new Endpoint(remoteIp, remotePort));
                        connection.setClientName(clientName);
                        connection.setLabels(labels);
                        BaseOpcRpcServer.this.connectionManager.holdAndCloseOld(connection);

                        ConnectionSetupServerResponse setupResponse = new ConnectionSetupServerResponse();
                        setupResponse.setRequestId(setupRequest.getRequestId());
                        this.doResponseWithConnectionFirst(setupResponse);
                    }
                    // ServerDetectionClientRequest
                    else if (payloadObj instanceof ServerDetectionClientRequest) {
                        log.debug("[{}]Grpc request,receive an ServerDetectionClientRequest,payloadObj={}", connectionId, payloadObj);
                        ServerDetectionClientRequest detectionRequest = (ServerDetectionClientRequest) payloadObj;

                        ServerDetectionServerResponse detectionResponse = new ServerDetectionServerResponse();
                        detectionResponse.setRequestId(detectionRequest.getRequestId());
                        this.doResponseWithConnectionFirst(detectionResponse);
                    }
                    // ClientRequest
                    else if (payloadObj instanceof ClientRequest) {
                        log.info("[{}]Grpc request bi stream,receive an ClientRequest,payloadObj={}", connectionId, payloadObj);
                        final ClientRequest clientRequest = (ClientRequest) payloadObj;
                        Response response = RequestHandlerSupport.handleRequest(clientRequest);
                        if (response == null) {
                            response = ErrorResponse.build(ResponseCode.HANDLE_REQUEST_NULL);
                        }
                        response.setRequestId(clientRequest.getRequestId());
                        this.doResponseWithConnectionFirst(response);
                    }
                    // ClientResponse
                    else if (payloadObj instanceof ClientResponse) {
                        log.info("[{}]Grpc request bi stream,receive an ClientResponse,payloadObj={}", connectionId, payloadObj);
                        RequestCallbackSupport.notifyCallback(connectionId, (ClientResponse) payloadObj);
                    }
                    // ErrorResponse
                    else if (payloadObj instanceof ErrorResponse) {
                        log.error("[{}]Grpc request bi stream,receive an ErrorResponse,payloadObj={}", connectionId, payloadObj);
                        RequestCallbackSupport.notifyCallback(connectionId, (ErrorResponse) payloadObj);
                    }
                    // unsupported payload
                    else {
                        log.warn("[{}]Grpc request bi stream,receive unsupported payload,payloadObj={}", connectionId, payloadObj);
                        ErrorResponse errorResponse = ErrorResponse.build(ResponseCode.UNSUPPORTED_PAYLOAD);
                        this.doResponseWithConnectionFirst(errorResponse);
                    }
                }

                /**
                 * must have a lock around the streamObserver.onNext(), so use GrpcConnection synchronized do it
                 */
                private void doResponseWithConnectionFirst(Response response) {
                    final Connection connection = BaseOpcRpcServer.this.connectionManager.getConnection(connectionId);
                    if (connection != null) {
                        connection.asyncResponse(response);
                    } else {
                        responseObserver.onNext(PayloadObjectHelper.buildGrpcPayload(response));
                    }
                }

                @Override
                public void onError(Throwable t) {
                    log.warn("[{}]Bi stream on error", connectionId, t);
                    completeResponseObserver(responseObserver);
                }

                @Override
                public void onCompleted() {
                    log.warn("[{}]Bi stream on completed", connectionId);
                    completeResponseObserver(responseObserver);
                }

                private void completeResponseObserver(StreamObserver<Payload> responseObserver) {
                    if (responseObserver instanceof ServerCallStreamObserver) {
                        ServerCallStreamObserver<?> serverCallStreamObserver = ((ServerCallStreamObserver<?>) responseObserver);
                        // isCancelled means client close the stream.
                        if (!serverCallStreamObserver.isCancelled()) {
                            serverCallStreamObserver.onCompleted();
                        }
                    }
                }
            };// new StreamObserver<Payload>()
        }// requestBiStream
    }// class OpcGrpcServiceImpl

    static final Attributes.Key<String> TRANS_KEY_CONN_ID = Attributes.Key.create("conn_id");

    static final Attributes.Key<String> TRANS_KEY_REMOTE_IP = Attributes.Key.create("remote_ip");

    static final Attributes.Key<Integer> TRANS_KEY_REMOTE_PORT = Attributes.Key.create("remote_port");

    static final Attributes.Key<Integer> TRANS_KEY_LOCAL_PORT = Attributes.Key.create("local_port");

    static final Context.Key<String> CONTEXT_KEY_CONN_ID = Context.key("conn_id");

    static final Context.Key<String> CONTEXT_KEY_CONN_REMOTE_IP = Context.key("remote_ip");

    static final Context.Key<Integer> CONTEXT_KEY_CONN_REMOTE_PORT = Context.key("remote_port");

    static final Context.Key<Integer> CONTEXT_KEY_CONN_LOCAL_PORT = Context.key("local_port");

}
