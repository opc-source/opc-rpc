package io.opc.rpc.core.connection;

import io.opc.rpc.api.Payload;
import io.opc.rpc.core.Endpoint;

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
     * requestBi.
     *
     * @param request Payload
     */
    void requestBi(Payload request);

    /**
     * close.
     */
    void close();

}
