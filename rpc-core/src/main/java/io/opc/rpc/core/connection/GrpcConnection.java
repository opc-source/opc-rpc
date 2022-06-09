package io.opc.rpc.core.connection;

import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.opc.rpc.api.response.Response;
import io.opc.rpc.core.RequestCallback;
import io.opc.rpc.core.RequestCallbackSupport;
import io.opc.rpc.core.grpc.auto.Payload;
import io.opc.rpc.core.util.PayloadObjectHelper;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

    /**
     * requestStreamObserver in client.
     * responseStreamObserver in server.
     */
    protected StreamObserver<Payload> biStreamObserver;

    /**
     * @param payload io.opc.rpc.api.Payload
     */
    protected void payloadNoAck(io.opc.rpc.api.Payload payload) {
        final Payload grpcPayload = PayloadObjectHelper.buildGrpcPayload(payload);
        // StreamObserver#onNext() is not thread-safe, synchronized is required to avoid direct memory leak.
        synchronized (biStreamObserver) {
            // maybe connection already closed with throw StatusRuntimeException
            biStreamObserver.onNext(grpcPayload);
        }
    }

    @Override
    public void asyncResponse(@Nonnull Response response) {
        this.payloadNoAck(response);
    }

    @Override
    public void asyncRequest(@Nonnull io.opc.rpc.api.request.Request request,
            @Nullable RequestCallback<? extends Response> requestCallback) {

        // First async listening a Response with requestCallback(if not null).
        if (requestCallback != null) {
            RequestCallbackSupport.addCallback(this.getConnectionId(), request.getRequestId(), requestCallback);
        }

        // Finally do payloadNoAck.
        this.payloadNoAck(request);
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
