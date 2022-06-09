package io.opc.rpc.api;

import io.opc.rpc.api.response.ErrorResponse;
import io.opc.rpc.api.response.Response;
import java.util.concurrent.Executor;

/**
 * Callback for request, who async listening a Response.
 *
 * @author caihongwen
 * @version Id: RequestCallback.java, v 0.1 2022年06月09日 14:05 caihongwen Exp $
 */
public interface RequestCallback<R extends Response> {

    /**
     * executor on callback. must not null.
     *
     * @return executor.
     */
    Executor getExecutor();

    /**
     * get timeout mills.
     *
     * @return timeout.
     */
    long getTimeout();

    /**
     * call on timeout.
     */
    void onTimeout();

    /**
     * call on success.
     *
     * @param response response.
     */
    void onResponse(R response);

    /**
     * call on error.
     *
     * @param errorResponse ErrorResponse.
     */
    void onError(ErrorResponse errorResponse);

    /**
     * please keep default
     *
     * @param response response.
     */
    default void callback(Response response) {
        if (response instanceof ErrorResponse) {
            onError((ErrorResponse) response);
        } else {
            //noinspection unchecked
            onResponse((R) response);
        }
    }

}
