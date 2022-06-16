package io.opc.rpc.core;

import io.opc.rpc.api.RequestCallback;
import io.opc.rpc.api.RequestFuture;
import io.opc.rpc.api.exception.OpcRpcRuntimeException;
import io.opc.rpc.api.response.ErrorResponse;
import io.opc.rpc.api.response.Response;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;

/**
 * RequestFutureTask. Default impl for RequestFuture.
 *
 * @author caihongwen
 * @version Id: RequestFutureTask.java, v 0.1 2022年06月16日 12:08 caihongwen Exp $
 */
public class RequestFutureTask<R extends Response> extends FutureTask<R> implements RequestFuture<R> {

    protected String connectionId;

    protected String requestId;

    @SuppressWarnings("rawtypes")
    private static final Callable EMPTY = () -> null;

    public RequestFutureTask(@Nonnull String connectionId, @Nonnull String requestId) {
        //noinspection unchecked
        super(EMPTY);
        this.connectionId = connectionId;
        this.requestId = requestId;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        final boolean cancel = super.cancel(mayInterruptIfRunning);
        RequestCallbackSupport.clearCallback(connectionId, requestId);
        return cancel;
    }

    /**
     * Only for Opc Inner.
     */
    public InvokeByRequestCallback<R> buildInvokeByRequestCallback(@Nonnull Executor executor) {
        return new InvokeByRequestCallback<>(executor, this);
    }

    @AllArgsConstructor
    static class InvokeByRequestCallback<R extends Response> implements RequestCallback<R> {

        private Executor executor;

        private RequestFutureTask<R> futureTask;

        @Override
        public Executor getExecutor() {
            return executor;
        }

        @Override
        public long getTimeout() {
            // means permanent
            return TimeUnit.HOURS.toMillis(23);
        }

        @Override
        public void onTimeout() {
            // Should not be triggered.
        }

        @Override
        public void onResponse(R response) {
            futureTask.set(response);
        }

        @Override
        public void onError(ErrorResponse errResp) {
            futureTask.setException(new OpcRpcRuntimeException(errResp.getResultCode(), errResp.getMessage()));
        }
    }

}
