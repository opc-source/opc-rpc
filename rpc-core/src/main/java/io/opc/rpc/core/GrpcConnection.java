package io.opc.rpc.core;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import io.opc.rpc.core.grpc.auto.Payload;
import io.opc.rpc.core.util.PayloadObjectHelper;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * GrpcConnection.
 *
 * @author mengyuan
 * @version Id: GrpcConnection.java, v 0.1 2022年06月03日 11:32 mengyuan Exp $
 */
@Setter
@Getter
@Builder
public class GrpcConnection extends BaseConnection implements Connection {

    protected ManagedChannel channel;

    protected StreamObserver<Payload> requestBiStreamObserver;

    @Override
    public void requestBi(io.opc.rpc.api.Payload request) {
        final Payload requestPayload = PayloadObjectHelper.buildGrpcPayload(request, Collections.emptyMap());
        requestBiStreamObserver.onNext(requestPayload);
    }

    @Override
    public void close() {
        if (this.channel == null || this.channel.isShutdown()) {
            return;
        }

        if (this.requestBiStreamObserver != null) {
            this.requestBiStreamObserver.onCompleted();
        }

        try {
            this.channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
            // ignore
        }

        if (!this.channel.isShutdown()) {
            // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
            // resources the channel should be shut down when it will no longer be used. If it may be used
            // again leave it running.
            try {
                this.channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignore) {
                // ignore
            }
        }
    }

}
