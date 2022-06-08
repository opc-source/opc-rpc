package io.opc.rpc.core.connection;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import io.opc.rpc.core.grpc.auto.Payload;
import java.util.concurrent.TimeUnit;
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
    public void requestBi(io.opc.rpc.api.Payload request) {
        super.requestBi(request);
        // refreshActiveTime for client
        this.refreshActiveTime();
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
