package io.opc.rpc.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.opc.rpc.api.OpcRpcClient;
import io.opc.rpc.api.constant.OpcConstants;
import io.opc.rpc.api.request.ServerRequest;
import io.opc.rpc.api.response.ClientResponse;
import io.opc.rpc.api.response.Response;
import io.opc.rpc.api.response.ServerResponse;
import io.opc.rpc.core.Connection;
import io.opc.rpc.core.Endpoint;
import io.opc.rpc.core.GrpcConnection;
import io.opc.rpc.core.grpc.auto.OpcGrpcServiceGrpc;
import io.opc.rpc.core.grpc.auto.Payload;
import io.opc.rpc.core.handle.RequestHandlerSupport;
import io.opc.rpc.core.request.ConnectionCheckClientRequest;
import io.opc.rpc.core.request.ConnectionSetupClientRequest;
import io.opc.rpc.core.response.ConnectionCheckServerResponse;
import io.opc.rpc.core.response.ErrorResponse;
import io.opc.rpc.core.util.PayloadObjectHelper;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;
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

    /**
     * Client Name.
     */
    protected String clientName;

    /**
     * Client labels.
     */
    protected Map<String, String> labels = new HashMap<>();

    /**
     * Server Endpoint.
     */
    protected Endpoint endpoint;

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

        String serverHost = properties.getProperty(OpcConstants.Server.KEY_OPC_RPC_SERVER_HOST);
        Integer serverPort = (Integer) properties.getOrDefault(OpcConstants.Server.KEY_OPC_RPC_SERVER_PORT,
                OpcConstants.Server.DEFAULT_OPC_RPC_SERVER_PORT);
        this.endpoint = new Endpoint(serverHost, serverPort);

        this.clientName = (String) properties.getOrDefault(OpcConstants.KEY_OPC_RPC_CLIENT_NAME, this.endpoint.getAddress());

        this.currentConnection = this.connectToServer(this.endpoint);
        if (this.currentConnection == null) {
            log.error("connect to server failed. do asyncSwitchServer.");
            this.asyncSwitchServer();
        }

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

        // subclass init
        this.doInit(properties);
    }

    protected void asyncSwitchServer() {
        // TODO switch endpoint from list
        this.reconnectionSignal.offer(this.endpoint);
    }

    protected void reconnect(@Nonnull final Endpoint endpoint) {
        Connection connection = this.connectToServer(endpoint);
        if (connection == null) {
            log.error("reconnect to server failed. do asyncSwitchServer.");
            this.asyncSwitchServer();
        } else {
            this.currentConnection = connection;
        }
    }

    protected Connection connectToServer(@Nonnull final Endpoint endpoint) {
        if (this.executor == null) {
            this.executor = createClientExecutor(this.endpoint.getAddress());
        }
        // Create a communication channel to the server, known as a Channel. Channels are thread-safe
        // and reusable. It is common to create channels at the beginning of your application and reuse
        // them until the application shuts down.
        ManagedChannel channel = ManagedChannelBuilder.forTarget(endpoint.getAddress())
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext()
                .executor(executor)
                .build();
        // future stub
        OpcGrpcServiceGrpc.OpcGrpcServiceFutureStub opcGrpcServiceFutureStub = OpcGrpcServiceGrpc.newFutureStub(channel);
        // biStream stub
        OpcGrpcServiceGrpc.OpcGrpcServiceStub opcGrpcServiceStub = OpcGrpcServiceGrpc.newStub(channel);

        // ConnectionCheck
        final ConnectionCheckClientRequest connectionCheckRequest = new ConnectionCheckClientRequest();
        final Payload connectionCheckRequestPayload
                = PayloadObjectHelper.buildGrpcPayload(connectionCheckRequest, Collections.emptyMap());
        final ListenableFuture<Payload> future = opcGrpcServiceFutureStub.request(connectionCheckRequestPayload);

        final Payload connectionCheckResponsePayload;
        try {
            connectionCheckResponsePayload = future.get(3000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("connectionCheckRequest get error,requestId={}", connectionCheckRequest.getRequestId(), e);
            shutdownChanel(channel);
            return null;
        }
        final ConnectionCheckServerResponse connectionCheckResponse
                = PayloadObjectHelper.buildApiPayload(connectionCheckResponsePayload);

        final GrpcConnection grpcConnection = GrpcConnection.builder().channel(channel).build();
        grpcConnection.setConnectionId(connectionCheckResponse.getConnectionId());
        grpcConnection.setClientName(this.clientName);
        grpcConnection.setEndpoint(endpoint);

        // customize responseBiStreamObserver
        final StreamObserver<Payload> responseBiStreamObserver = new StreamObserver<Payload>() {

            @Override
            public void onNext(Payload value) {
                final io.opc.rpc.api.Payload payloadObj = PayloadObjectHelper.buildApiPayload(value);
                // ServerRequest
                if (payloadObj instanceof ServerRequest) {
                    log.info("[{}] responseBiStreamObserver receive an ServerRequest,payloadObj={}",
                            grpcConnection.getConnectionId(), payloadObj);
                    final ServerRequest serverRequest = (ServerRequest) payloadObj;
                    Response response = RequestHandlerSupport.handleRequest(serverRequest);
                    if (response == null) {
                        response = ErrorResponse.build(501, "handleRequest get null");
                    }
                    response.setRequestId(serverRequest.getRequestId());
                    // do response
                    grpcConnection.requestBi(response);
                }
                // ServerResponse
                else if (payloadObj instanceof ServerResponse) {
                    log.info("[{}] responseBiStreamObserver receive an ServerResponse,payloadObj={}",
                            grpcConnection.getConnectionId(), payloadObj);
                    // TODO deal ServerResponse?
                }
                // ErrorResponse
                else if (payloadObj instanceof ErrorResponse) {
                    log.error("[{}] responseBiStreamObserver receive an ErrorResponse,payloadObj={}",
                            grpcConnection.getConnectionId(), payloadObj);
                }
                // unsupported payload
                else {
                    log.warn("[{}] responseBiStreamObserver receive unsupported payload,payloadObj={}",
                            grpcConnection.getConnectionId(), payloadObj);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("[{}] responseBiStreamObserver on error, switch server async", grpcConnection.getConnectionId(), t);
                // TODO set status=RpcClientStatus.UNHEALTHY ?
                asyncSwitchServer();
            }

            @Override
            public void onCompleted() {
                log.warn("[{}] responseBiStreamObserver on completed", grpcConnection.getConnectionId());
                // TODO maybe same as onError()
                asyncSwitchServer();
            }
        };
        final StreamObserver<Payload> requestBiStreamObserver = opcGrpcServiceStub.requestBiStream(responseBiStreamObserver);
        grpcConnection.setBiStreamObserver(requestBiStreamObserver);

        // do ConnectionSetup
        final ConnectionSetupClientRequest setupClientRequest = ConnectionSetupClientRequest.builder()
                .clientName(this.clientName)
                .labels(this.labels).build();
        grpcConnection.requestBi(setupClientRequest);

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
