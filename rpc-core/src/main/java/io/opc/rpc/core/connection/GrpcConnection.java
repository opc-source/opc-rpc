package io.opc.rpc.core.connection;

import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.opc.rpc.api.Connection;
import io.opc.rpc.api.RequestCallback;
import io.opc.rpc.api.RequestFuture;
import io.opc.rpc.api.exception.ExceptionCode;
import io.opc.rpc.api.exception.OpcConnectionException;
import io.opc.rpc.api.response.Response;
import io.opc.rpc.core.RequestCallbackSupport;
import io.opc.rpc.core.RequestFutureTask;
import io.opc.rpc.core.grpc.auto.Payload;
import io.opc.rpc.core.util.PayloadObjectHelper;
import java.util.concurrent.Executor;
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
    protected void payloadNoAck(io.opc.rpc.api.Payload payload) throws OpcConnectionException {
        final Payload grpcPayload = PayloadObjectHelper.buildGrpcPayload(payload);
        // StreamObserver#onNext() is not thread-safe, synchronized is required to avoid direct memory leak.
        synchronized (biStreamObserver) {
            // maybe connection already closed with throw StatusRuntimeException
            try {
                biStreamObserver.onNext(grpcPayload);
            } catch (StatusRuntimeException statusEx) {
                throw new OpcConnectionException(ExceptionCode.CONNECTION_ERROR, statusEx);
            }
        }
    }

    @Override
    public void responseAsync(@Nonnull Response response) throws OpcConnectionException {
        this.payloadNoAck(response);
    }

    @Override
    public void requestAsync(@Nonnull io.opc.rpc.api.request.Request request,
            @Nullable RequestCallback<? extends Response> requestCallback) throws OpcConnectionException {

        // First async listening a Response with requestCallback(if not null).
        if (requestCallback != null) {
            RequestCallbackSupport.addCallback(this.getConnectionId(), request.getRequestId(), requestCallback);
        }

        try {
            // Finally do payloadNoAck.
            this.payloadNoAck(request);
        } catch (Exception ex) {
            // Clear callback if exception
            RequestCallbackSupport.clearCallback(this.getConnectionId(), request.getRequestId());
            throw ex;
        }
    }

    @Override
    public <T extends Response> RequestFuture<T> requestFuture(@Nonnull io.opc.rpc.api.request.Request request)
            throws OpcConnectionException {

        final RequestFutureTask<T> future = new RequestFutureTask<>(this.getConnectionId(), request.getRequestId());
        // maybe supply an executor
        final Executor executor = MoreExecutors.directExecutor();
        this.requestAsync(request, future.buildInvokeByRequestCallback(executor));

        return future;
    }

    @Override
    public void close() {
        this.closeBiStream();

        RequestCallbackSupport.clearContext(this.getConnectionId());
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
