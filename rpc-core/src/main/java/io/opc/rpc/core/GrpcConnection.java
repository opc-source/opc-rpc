package io.opc.rpc.core;

import io.grpc.ManagedChannel;
import io.grpc.stub.ServerCallStreamObserver;
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
 * @author caihongwen
 * @version Id: GrpcConnection.java, v 0.1 2022年06月03日 11:32 caihongwen Exp $
 */
@Setter
@Getter
@Builder
public class GrpcConnection extends BaseConnection implements Connection {

    protected ManagedChannel channel;

    protected StreamObserver<Payload> biStreamObserver;

    @Override
    public void requestBi(io.opc.rpc.api.Payload request) {
        final Payload requestPayload = PayloadObjectHelper.buildGrpcPayload(request, Collections.emptyMap());
        // StreamObserver#onNext() is not thread-safe, synchronized is required to avoid direct memory leak.
        synchronized (biStreamObserver) {
            // maybe connection already closed with throw StatusRuntimeException
            biStreamObserver.onNext(requestPayload);
        }
    }

    @Override
    public void close() {
        this.closeBiStream();
        this.closeChanel();
    }

    private void closeBiStream() {
        if (this.biStreamObserver != null) {
            try {
                if (this.biStreamObserver instanceof ServerCallStreamObserver) {
                    ServerCallStreamObserver<?> serverCallStreamObserver = ((ServerCallStreamObserver<?>) biStreamObserver);
                    // isCancelled means client close the stream.
                    if (!serverCallStreamObserver.isCancelled()) {
                        serverCallStreamObserver.onCompleted();
                    }
                } else {
                    this.biStreamObserver.onCompleted();
                }
            } catch (Exception ignore) {
                // ignore
            }
        }
    }

    private void closeChanel() {
        if (this.channel == null || this.channel.isShutdown()) {
            return;
        }

        try {
            this.channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignore) {
            // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
            // resources the channel should be shut down when it will no longer be used. If it may be used
            // again leave it running.
            this.channel.shutdownNow();
        }
    }

}
