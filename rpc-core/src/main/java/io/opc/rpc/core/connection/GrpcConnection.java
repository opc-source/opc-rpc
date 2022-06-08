package io.opc.rpc.core.connection;

import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.opc.rpc.core.grpc.auto.Payload;
import io.opc.rpc.core.util.PayloadObjectHelper;
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

    protected StreamObserver<Payload> biStreamObserver;

    @Override
    public void requestBi(io.opc.rpc.api.Payload request) {
        final Payload requestPayload = PayloadObjectHelper.buildGrpcPayload(request);
        // StreamObserver#onNext() is not thread-safe, synchronized is required to avoid direct memory leak.
        synchronized (biStreamObserver) {
            // maybe connection already closed with throw StatusRuntimeException
            biStreamObserver.onNext(requestPayload);
        }
    }

    @Override
    public void close() {
        this.closeBiStream();
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

}
