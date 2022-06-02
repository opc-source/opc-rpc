package io.opc.rpc.core;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.Any;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.opc.rpc.api.OpcRpcClient;
import io.opc.rpc.api.constant.OpcConstants;
import io.opc.rpc.core.grpc.auto.Metadata;
import io.opc.rpc.core.grpc.auto.OpcGrpcServiceGrpc;
import io.opc.rpc.core.grpc.auto.Payload;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nonnull;

/**
 * BaseOpcRpcClient.
 *
 * @author caihongwen
 * @version Id: BaseOpcRpcClient.java, v 0.1 2022年06月02日 22:38 caihongwen Exp $
 */
public abstract class BaseOpcRpcClient implements OpcRpcClient {

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    protected Endpoint endpoint;

    protected ThreadPoolExecutor grpcExecutor;

    protected ManagedChannel channel;

    protected OpcGrpcServiceGrpc.OpcGrpcServiceFutureStub opcGrpcServiceFutureStub;

    protected OpcGrpcServiceGrpc.OpcGrpcServiceStub opcGrpcServiceStub;

    @Override
    public void init(@Nonnull Properties properties) {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        String serverHost = properties.getProperty(OpcConstants.Server.KEY_OPC_RPC_SERVER_HOST);
        Integer serverPort = (Integer) properties.getOrDefault(OpcConstants.Server.KEY_OPC_RPC_SERVER_PORT,
                OpcConstants.Server.DEFAULT_OPC_RPC_SERVER_PORT);
        this.endpoint = new Endpoint(serverHost, serverPort);

        this.connectToServer(this.endpoint);

        // subclass init
        this.doInit(properties);
    }

    protected void connectToServer(@Nonnull final Endpoint endpoint) {
        if (this.grpcExecutor == null) {
            this.grpcExecutor = createGrpcExecutor(this.endpoint.getAddress());
        }
        // Create a communication channel to the server, known as a Channel. Channels are thread-safe
        // and reusable. It is common to create channels at the beginning of your application and reuse
        // them until the application shuts down.
        this.channel = ManagedChannelBuilder.forTarget(endpoint.getAddress())
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext()
                .executor(grpcExecutor)
                .build();

        // future stub
        this.opcGrpcServiceFutureStub = OpcGrpcServiceGrpc.newFutureStub(this.channel);
        // biStream stub
        this.opcGrpcServiceStub = OpcGrpcServiceGrpc.newStub(this.channel);

        // TODO
        final Payload serverCheckRequest = Payload.newBuilder().setMetadata(Metadata.newBuilder().build()).setBody(Any.newBuilder().build())
                .build();
        final ListenableFuture<Payload> future = this.opcGrpcServiceFutureStub.request(serverCheckRequest);
        try {
            final Payload serverCheckResponse = future.get(3000, TimeUnit.MILLISECONDS);
            if (serverCheckResponse != null) {
                this.opcGrpcServiceStub.requestBiStream(new StreamObserver<Payload>() {

                    @Override
                    public void onNext(Payload value) {
                        // TODO the request from server.
                    }

                    @Override
                    public void onError(Throwable t) {

                        // TODO
                    }

                    @Override
                    public void onCompleted() {
                        // TODO
                    }
                });
            }
        } catch (Exception e) {
            if (this.channel != null && !this.channel.isShutdown()) {
                this.channel.shutdownNow();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * doInit on subclass
     *
     * @param properties Properties
     */
    protected abstract void doInit(Properties properties);

    @Override
    public void destroy() {
        if (this.channel == null || this.channel.isShutdown()) {
            return;
        }

        try {
            this.channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
            // ignore
        }
        // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
        // resources the channel should be shut down when it will no longer be used. If it may be used
        // again leave it running.
        try {
            this.channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
            // ignore
        }
    }

    protected ThreadPoolExecutor createGrpcExecutor(String serverAddress) {
        ThreadPoolExecutor opcExecutor = new ThreadPoolExecutor(
                Math.max(2, Runtime.getRuntime().availableProcessors()),
                Math.max(4, Runtime.getRuntime().availableProcessors() * 2),
                10L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10000),
                new ThreadFactoryBuilder()
                        .setDaemon(true)
                        .setNameFormat("opc-grpc-client-executor-" + serverAddress + "-%d")
                        .build());
        opcExecutor.allowCoreThreadTimeOut(true);
        return opcExecutor;
    }

}
