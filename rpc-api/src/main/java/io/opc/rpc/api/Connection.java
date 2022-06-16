package io.opc.rpc.api;

import io.opc.rpc.api.exception.OpcConnectionException;
import io.opc.rpc.api.response.Response;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Connection.
 *
 * @author caihongwen
 * @version Id: Connection.java, v 0.1 2022年06月03日 11:31 caihongwen Exp $
 */
public interface Connection {

    /**
     * getConnectionId.
     *
     * @return connectionId
     */
    String getConnectionId();

    /**
     * getClientName.
     *
     * @return clientName
     */
    String getClientName();

    /**
     * getEndpoint.
     *
     * @return Endpoint
     */
    Endpoint getEndpoint();

    /**
     * async back Response.
     *
     * @param response Response
     * @throws OpcConnectionException OpcConnectionException
     */
    void responseAsync(@Nonnull io.opc.rpc.api.response.Response response) throws OpcConnectionException;

    /**
     * async send Request.
     *
     * @param request Request
     * @throws OpcConnectionException OpcConnectionException
     */
    default void requestAsync(@Nonnull io.opc.rpc.api.request.Request request) throws OpcConnectionException {
        requestAsync(request, null);
    }

    /**
     * async send Request. async listening a Response with RequestCallback.
     *
     * @param request Request
     * @param requestCallback RequestCallback<R extends Response>, null means do not care about is.
     * @throws OpcConnectionException OpcConnectionException
     */
    void requestAsync(@Nonnull io.opc.rpc.api.request.Request request, @Nullable RequestCallback<? extends Response> requestCallback)
            throws OpcConnectionException;

    /**
     * async send Request. waiting a Response.
     *
     * @param request Request
     * @return RequestFuture<T extends Response>
     * @throws OpcConnectionException OpcConnectionException
     */
    <T extends Response> RequestFuture<T> requestFuture(@Nonnull io.opc.rpc.api.request.Request request) throws OpcConnectionException;

    /**
     * close.
     */
    void close();

}
