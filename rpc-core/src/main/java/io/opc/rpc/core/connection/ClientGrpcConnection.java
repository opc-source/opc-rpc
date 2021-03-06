package io.opc.rpc.core.connection;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import io.opc.rpc.api.RequestCallback;
import io.opc.rpc.api.RequestFuture;
import io.opc.rpc.api.exception.OpcConnectionException;
import io.opc.rpc.api.response.Response;
import io.opc.rpc.core.grpc.auto.Payload;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

/**
 * ClientGrpcConnection. for Client.
 *
 * @author caihongwen
 * @version Id: ClientGrpcConnection.java, v 0.1 2022年06月08日 21:31 caihongwen Exp $
 */
@Setter
@Getter
public class ClientGrpcConnection extends GrpcConnection {

    protected ManagedChannel channel;

    public ClientGrpcConnection(ManagedChannel channel, StreamObserver<Payload> biStreamObserver) {
        super(biStreamObserver);
        this.channel = channel;
    }

    @Override
    public void requestAsync(@Nonnull io.opc.rpc.api.request.Request request,
            @Nullable RequestCallback<? extends Response> requestCallback) throws OpcConnectionException {

        super.requestAsync(request, requestCallback);
        // refreshActiveTime for client
        this.refreshActiveTime();
    }

    @Override
    public <T extends Response> RequestFuture<T> requestFuture(@Nonnull io.opc.rpc.api.request.Request request)
            throws OpcConnectionException {

        final RequestFuture<T> future = super.requestFuture(request);
        // refreshActiveTime for client
        this.refreshActiveTime();
        return future;
    }

    @Override
    public void close() {
        super.close();
        // Finally closeChanel
        this.closeChanel();
    }

    private void closeChanel() {
        if (this.channel == null || this.channel.isShutdown()) {
            return;
        }

        try {
            this.channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
            Thread.currentThread().interrupt();
            // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
            // resources the channel should be shut down when it will no longer be used. If it may be used
            // again leave it running.
            this.channel.shutdownNow();
        }
    }

}
