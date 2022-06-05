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
import io.opc.rpc.api.OpcRpcServer;
import io.opc.rpc.api.constant.OpcConstants;
import io.opc.rpc.api.request.ClientRequest;
import io.opc.rpc.api.response.ClientResponse;
import io.opc.rpc.api.response.Response;
import io.opc.rpc.api.response.ServerResponse;
import io.opc.rpc.core.BaseConnection;
import io.opc.rpc.core.Connection;
import io.opc.rpc.core.ConnectionManager;
import io.opc.rpc.core.GrpcConnection;
import io.opc.rpc.core.grpc.auto.OpcGrpcServiceGrpc;
import io.opc.rpc.core.grpc.auto.Payload;
import io.opc.rpc.core.handle.RequestHandlerSupport;
import io.opc.rpc.core.request.ClientDetectionServerRequest;
import io.opc.rpc.core.request.ConnectionCheckClientRequest;
import io.opc.rpc.core.request.ConnectionSetupClientRequest;
import io.opc.rpc.core.response.ConnectionCheckServerResponse;
import io.opc.rpc.core.response.ConnectionSetupServerResponse;
import io.opc.rpc.core.response.ErrorResponse;
import io.opc.rpc.core.util.PayloadObjectHelper;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * BaseOpcRpcServer.
 *
 * @author mengyuan
 * @version Id: BaseOpcRpcServer.java, v 0.1 2022年06月05日 10:35 mengyuan Exp $
 */
@Slf4j
public abstract class BaseOpcRpcServer implements OpcRpcServer {

    protected ThreadPoolExecutor executor;

    protected ScheduledThreadPoolExecutor scheduledExecutor;

    protected Server server;

    @Override
    public void init(Properties properties) {

        Integer serverPort = (Integer) properties.getOrDefault(OpcConstants.Server.KEY_OPC_RPC_SERVER_PORT,
                OpcConstants.Server.DEFAULT_OPC_RPC_SERVER_PORT);

        // server interceptor to set connection id.
        ServerInterceptor serverInterceptor = new ServerInterceptor() {
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
        };
        final MutableHandlerRegistry handlerRegistry = new MutableHandlerRegistry();
        handlerRegistry.addService(ServerInterceptors.intercept(new OpcGrpcServiceImpl(), serverInterceptor));

        this.executor = createServerExecutor(serverPort);

        this.server = ServerBuilder.forPort(serverPort).executor(this.executor)
                .fallbackHandlerRegistry(handlerRegistry)
                .addTransportFilter(new ServerTransportFilter() {
                    @Override
                    public Attributes transportReady(Attributes transportAttrs) {
                        InetSocketAddress remoteAddress = (InetSocketAddress) transportAttrs.get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
                        InetSocketAddress localAddress = (InetSocketAddress) transportAttrs.get(Grpc.TRANSPORT_ATTR_LOCAL_ADDR);
                        String remoteIp = remoteAddress.getAddress().getHostAddress();
                        int remotePort = remoteAddress.getPort();
                        int localPort = localAddress.getPort();
                        Attributes attrWrapper = transportAttrs.toBuilder()
                                .set(TRANS_KEY_CONN_ID, System.currentTimeMillis() + "_" + remoteIp + ":" + remotePort)
                                .set(TRANS_KEY_REMOTE_IP, remoteIp)
                                .set(TRANS_KEY_REMOTE_PORT, remotePort)
                                .set(TRANS_KEY_LOCAL_PORT, localPort).build();

                        final String connectionId = attrWrapper.get(TRANS_KEY_CONN_ID);
                        log.info("Connection transportReady,connectionId = {} ", connectionId);
                        return attrWrapper;
                    }

                    @Override
                    public void transportTerminated(Attributes transportAttrs) {
                        try {
                            String connectionId = transportAttrs.get(TRANS_KEY_CONN_ID);
                            log.info("Connection transportTerminated,connectionId = {} ", connectionId);
                            ConnectionManager.removeAndCloseConnection(connectionId);
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                }).build();

        try {
            this.server.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // TODO just for test now.
        this.scheduledExecutor = new ScheduledThreadPoolExecutor(1,
                r -> new Thread(r, "io.opc.rpc.core.BaseOpcRpcServerScheduler"));
        this.scheduledExecutor.scheduleWithFixedDelay(() -> {
            for (Connection connection : ConnectionManager.getConnections()) {
                connection.requestBi(new ClientDetectionServerRequest());
            }
        }, 1000L, 3000L, TimeUnit.MILLISECONDS);

        // subclass init
        this.doInit(properties);
    }

    /**
     * doInit on subclass
     *
     * @param properties Properties
     */
    protected abstract void doInit(Properties properties);

    protected ThreadPoolExecutor createServerExecutor(int port) {
        return new ThreadPoolExecutor(
                Math.max(2, Runtime.getRuntime().availableProcessors()),
                Math.max(4, Runtime.getRuntime().availableProcessors() * 2),
                10L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10000),
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("opc-rpc-server-executor-" + port + "-%d")
                        .build());
    }

    static class OpcGrpcServiceImpl extends OpcGrpcServiceGrpc.OpcGrpcServiceImplBase {

        @Override
        public void request(io.opc.rpc.core.grpc.auto.Payload requestPayload,
                io.grpc.stub.StreamObserver<io.opc.rpc.core.grpc.auto.Payload> responseObserver) {

            final String connectionId = CONTEXT_KEY_CONN_ID.get();
            io.opc.rpc.api.Payload payloadObj;
            try {
                payloadObj = PayloadObjectHelper.buildApiPayload(requestPayload);
            } catch (Throwable throwable) {
                log.error("[{}]Grpc request,payload deserialize error", connectionId, throwable);
                ErrorResponse errorResponse = ErrorResponse.build(500, throwable.getMessage());
                responseObserver.onNext(PayloadObjectHelper.buildGrpcPayload(errorResponse, Collections.emptyMap()));
                responseObserver.onCompleted();
                return;
            }

            // ConnectionCheck
            if (payloadObj instanceof ConnectionCheckClientRequest) {
                log.info("[{}]Grpc request,receive an ConnectionCheckClientRequest,payloadObj={}", connectionId, payloadObj);
                ConnectionCheckClientRequest checkRequest = (ConnectionCheckClientRequest) payloadObj;

                ConnectionCheckServerResponse checkResponse = new ConnectionCheckServerResponse();
                checkResponse.setRequestId(checkRequest.getRequestId());
                checkResponse.setConnectionId(connectionId);

                responseObserver.onNext(PayloadObjectHelper.buildGrpcPayload(checkResponse, Collections.emptyMap()));
                responseObserver.onCompleted();
            }
            // ClientRequest
            else if (payloadObj instanceof ClientRequest) {
                log.info("[{}]Grpc request,receive an ClientRequest,payloadObj={}", connectionId, payloadObj);
                final ClientRequest clientRequest = (ClientRequest) payloadObj;
                Response response = RequestHandlerSupport.handleRequest(clientRequest);
                if (response == null) {
                    ErrorResponse errorResponse = ErrorResponse.build(501, "handleRequest get null");
                    errorResponse.setRequestId(clientRequest.getRequestId());
                    response = errorResponse;
                } else if (response instanceof ServerResponse) {
                    ((ServerResponse) response).setRequestId(clientRequest.getRequestId());
                }
                responseObserver.onNext(PayloadObjectHelper.buildGrpcPayload(response, Collections.emptyMap()));
                responseObserver.onCompleted();
            }
            // ErrorResponse
            else if (payloadObj instanceof ErrorResponse) {
                log.error("[{}]Grpc request,receive an ErrorResponse,payloadObj={}", connectionId, payloadObj);
            }
            // unsupported payload
            else {
                log.warn("[{}]Grpc request,receive unsupported payload,payloadObj={}", connectionId, payloadObj);
                ErrorResponse errorResponse = ErrorResponse.build(500, "unsupported payload");
                responseObserver.onNext(PayloadObjectHelper.buildGrpcPayload(errorResponse, Collections.emptyMap()));
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

                    io.opc.rpc.api.Payload payloadObj;
                    try {
                        payloadObj = PayloadObjectHelper.buildApiPayload(requestPayload);
                    } catch (Throwable throwable) {
                        log.error("[{}]Grpc request bi stream,payload deserialize error", connectionId, throwable);
                        ErrorResponse errorResponse = ErrorResponse.build(500, throwable.getMessage());
                        responseObserver.onNext(PayloadObjectHelper.buildGrpcPayload(errorResponse, Collections.emptyMap()));
                        return;
                    }

                    // ConnectionSetupClientRequest
                    if (payloadObj instanceof ConnectionSetupClientRequest) {
                        log.info("[{}]Grpc request bi stream,receive an ConnectionSetupClientRequest,payloadObj={}",
                                connectionId, payloadObj);
                        ConnectionSetupClientRequest setupRequest = (ConnectionSetupClientRequest) payloadObj;
                        final String clientName = setupRequest.getClientName();
                        final Map<String, String> labels = setupRequest.getLabels();

                        BaseConnection connection = GrpcConnection.builder()
                                .channel(null).requestBiStreamObserver(responseObserver).build();
                        connection.setConnectionId(connectionId);
                        connection.setClientName(clientName);
                        connection.setLabels(labels);
                        ConnectionManager.holdAndCloseOld(connection);

                        ConnectionSetupServerResponse setupResponse = new ConnectionSetupServerResponse();
                        setupResponse.setRequestId(setupRequest.getRequestId());
                        responseObserver.onNext(PayloadObjectHelper.buildGrpcPayload(setupResponse, Collections.emptyMap()));
                    }
                    // ClientRequest
                    else if (payloadObj instanceof ClientRequest) {
                        log.info("[{}]Grpc request bi stream,receive an ClientRequest,payloadObj={}", connectionId, payloadObj);
                        final ClientRequest clientRequest = (ClientRequest) payloadObj;
                        Response response = RequestHandlerSupport.handleRequest(clientRequest);
                        if (response == null) {
                            ErrorResponse errorResponse = ErrorResponse.build(501, "handleRequest get null");
                            errorResponse.setRequestId(clientRequest.getRequestId());
                            response = errorResponse;
                        } else if (response instanceof ServerResponse) {
                            ((ServerResponse) response).setRequestId(clientRequest.getRequestId());
                        }
                        responseObserver.onNext(PayloadObjectHelper.buildGrpcPayload(response, Collections.emptyMap()));
                    }
                    // ClientResponse
                    else if (payloadObj instanceof ClientResponse) {
                        log.info("[{}]Grpc request bi stream,receive an ClientResponse,payloadObj={}", connectionId, payloadObj);
                        // TODO deal ServerResponse?
                        ClientResponse response = (ClientResponse) payloadObj;
                        final String requestId = response.getRequestId();
                        //RpcAckCallbackSynchronizer.ackNotify(connectionId, response);
                        //ConnectionManager.refreshActiveTime(connectionId);
                    }
                    // ErrorResponse
                    else if (payloadObj instanceof ErrorResponse) {
                        log.error("[{}]Grpc request bi stream,receive an ErrorResponse,payloadObj={}", connectionId, payloadObj);
                    }
                    // unsupported payload
                    else {
                        log.warn("[{}]Grpc request bi stream,receive unsupported payload,payloadObj={}", connectionId, payloadObj);
                        ErrorResponse errorResponse = ErrorResponse.build(500, "unsupported payload");
                        responseObserver.onNext(PayloadObjectHelper.buildGrpcPayload(errorResponse, Collections.emptyMap()));
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
