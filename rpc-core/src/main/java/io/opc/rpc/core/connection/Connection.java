package io.opc.rpc.core.connection;

import io.opc.rpc.api.RequestCallback;
import io.opc.rpc.api.exception.OpcConnectionException;
import io.opc.rpc.api.response.Response;
import io.opc.rpc.core.Endpoint;
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
     */
    void asyncResponse(@Nonnull io.opc.rpc.api.response.Response response) throws OpcConnectionException;

    /**
     * async send Request.
     *
     * @param request Request
     */
    default void asyncRequest(@Nonnull io.opc.rpc.api.request.Request request) throws OpcConnectionException {
        asyncRequest(request, null);
    }

    /**
     * async send Request. async listening a Response with RequestCallback.
     *
     * @param request Request
     * @param requestCallback RequestCallback<R extends Response>, null means do not care about is.
     */
    void asyncRequest(@Nonnull io.opc.rpc.api.request.Request request, @Nullable RequestCallback<? extends Response> requestCallback)
            throws OpcConnectionException;

    /**
     * close.
     */
    void close();

}
