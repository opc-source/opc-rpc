package io.opc.rpc.api;

import io.opc.rpc.api.request.Request;
import io.opc.rpc.api.response.Response;
import java.util.Properties;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * OpcRpcClient.
 *
 * @author caihongwen
 * @version Id: OpcRpcClient.java, v 0.1 2022年06月02日 22:01 caihongwen Exp $
 */
public interface OpcRpcClient extends AutoCloseable {

    /**
     * init.
     *
     * @param properties {@link Properties}
     */
    void init(Properties properties);

    /**
     * async send Request. async listening a Response with RequestCallback.
     *
     * @param request Request
     * @param requestCallback RequestCallback<R extends Response>, null means do not care about is.
     */
    void asyncRequest(@Nonnull Request request, @Nullable RequestCallback<? extends Response> requestCallback);

}
