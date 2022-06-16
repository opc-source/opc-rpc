package io.opc.rpc.api;

import io.opc.rpc.api.exception.OpcConnectionException;
import io.opc.rpc.api.exception.OpcRpcRuntimeException;
import io.opc.rpc.api.request.ClientRequest;
import io.opc.rpc.api.request.ServerRequest;
import io.opc.rpc.api.response.ClientResponse;
import io.opc.rpc.api.response.ServerResponse;
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
     * @throws OpcRpcRuntimeException OpcRpcRuntimeException
     */
    void init(Properties properties) throws OpcRpcRuntimeException;

    /**
     * register a ServerRequestHandler who handle ServerRequest.
     *
     * @param requestClass requestClass
     * @param requestHandler RequestHandler
     */
    void registerServerRequestHandler(Class<? extends ServerRequest> requestClass,
            RequestHandler<? extends ServerRequest, ? extends ClientResponse> requestHandler);

    /**
     * async send Request. async listening a Response with RequestCallback.
     *
     * @param request Request
     * @param requestCallback RequestCallback<R extends Response>, null means do not care about is.
     * @throws OpcConnectionException OpcConnectionException
     */
    void requestAsync(@Nonnull ClientRequest request, @Nullable RequestCallback<? extends ServerResponse> requestCallback)
            throws OpcConnectionException;

    /**
     * async send Request. waiting a Response.
     *
     * @param request Request
     * @return RequestFuture<R extends ServerResponse>
     * @throws OpcConnectionException OpcConnectionException
     */
    RequestFuture<? extends ServerResponse> requestFuture(@Nonnull ClientRequest request) throws OpcConnectionException;

}
