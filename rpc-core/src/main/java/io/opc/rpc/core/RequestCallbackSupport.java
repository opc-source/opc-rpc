package io.opc.rpc.core;

import io.grpc.netty.shaded.io.netty.util.HashedWheelTimer;
import io.opc.rpc.api.RequestCallback;
import io.opc.rpc.api.exception.ExceptionCode;
import io.opc.rpc.api.exception.OpcRpcRuntimeException;
import io.opc.rpc.api.response.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * RequestCallbackSupport.
 *
 * @author caihongwen
 * @version Id: RequestCallbackSupport.java, v 0.1 2022年06月09日 14:52 caihongwen Exp $
 */
@UtilityClass
@Slf4j
public class RequestCallbackSupport {

    /**
     * connectionId : { requestId : RequestCallback }
     */
    private static final Map<String, Map<String, RequestCallback<?>>> CALLBACK_CONTEXT = new ConcurrentHashMap<>(32);

    private static final HashedWheelTimer HASHED_WHEEL_TIMER = new HashedWheelTimer(r -> {
        Thread t = new Thread(r);
        t.setName("io.opc.rpc.core.RequestCallbackSupportTimer");
        t.setDaemon(true);
        return t;
    }, 1, TimeUnit.MILLISECONDS, 128);

    /**
     * notify callback.
     */
    public static <R extends Response> void notifyCallback(String connectionId, R response) {

        Map<String, RequestCallback<?>> requestCallbackMap = CALLBACK_CONTEXT.get(connectionId);
        if (requestCallbackMap == null) {
            log.warn("Ack receive on a outdated connection,connectionId={},requestId={},response={}",
                    connectionId, response.getRequestId(), response);
            return;
        }

        RequestCallback<?> requestCallback = requestCallbackMap.remove(response.getRequestId());
        if (requestCallback == null) {
            log.warn("Ack receive on a outdated request,connectionId={},requestId={},response={}",
                    connectionId, response.getRequestId(), response);
            return;
        }

        requestCallback.getExecutor().execute(() -> requestCallback.callback(response));
    }

    /**
     * addCallback wait for async notify.
     */
    public static void addCallback(@Nonnull String connectionId, @Nonnull String requestId, @Nonnull RequestCallback<?> requestCallback) {
        Objects.requireNonNull(requestCallback.getExecutor(), "requestCallback.executor can not be null");

        Map<String, RequestCallback<?>> requestCallbackMap = initContextIfNecessary(connectionId);

        RequestCallback<?> requestCallbackOld = requestCallbackMap.putIfAbsent(requestId, requestCallback);
        if (requestCallbackOld != null) {
            throw new OpcRpcRuntimeException(ExceptionCode.REQUEST_ID_CONFLICT.getCode(), "Conflict requestId:" + requestId);
        }

        // register timeout task
        HASHED_WHEEL_TIMER.newTimeout(timeout -> {
            // Skip after (being canceled) or (be notifyCallback and removed).
            RequestCallback<?> requestCallbackOnTimeout;
            if (timeout.isCancelled() || (requestCallbackOnTimeout = clearCallback(connectionId, requestId)) == null) {
                return;
            }

            requestCallbackOnTimeout.getExecutor().execute(requestCallbackOnTimeout::onTimeout);

        }, requestCallback.getTimeout(), TimeUnit.MILLISECONDS);
    }

    /**
     * clear context of connectionId.
     *
     * @param connectionId connectionId
     */
    private static Map<String, RequestCallback<?>> initContextIfNecessary(String connectionId) {

        final Map<String, RequestCallback<?>> requestCallbackMap = CALLBACK_CONTEXT.get(connectionId);
        if (requestCallbackMap != null) {
            return requestCallbackMap;
        }
        return CALLBACK_CONTEXT.computeIfAbsent(connectionId, cid -> new HashMap<>(128));
    }

    /**
     * clear context of requestId.
     *
     * @param connectionId connectionId
     * @param requestId requestId
     */
    public static RequestCallback<?> clearCallback(String connectionId, String requestId) {
        Map<String, RequestCallback<?>> requestCallbackMap = CALLBACK_CONTEXT.get(connectionId);
        if (requestCallbackMap == null || !requestCallbackMap.containsKey(requestId)) {
            return null;
        }
        return requestCallbackMap.remove(requestId);
    }

    /**
     * clear context of connectionId. Maybe on connection close.
     *
     * @param connectionId connectionId
     */
    public static void clearContext(String connectionId) {
        CALLBACK_CONTEXT.remove(connectionId);
    }

}
