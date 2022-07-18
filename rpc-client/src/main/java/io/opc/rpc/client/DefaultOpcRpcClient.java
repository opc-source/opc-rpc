package io.opc.rpc.client;

import io.opc.rpc.api.Endpoint;
import io.opc.rpc.api.OpcRpcClient;
import io.opc.rpc.api.exception.OpcConnectionException;
import java.util.Properties;

/**
 * DefaultOpcRpcClient.
 *
 * @author caihongwen
 * @version Id: DefaultOpcRpcClient.java, v 0.1 2022年06月02日 22:38 caihongwen Exp $
 */
public class DefaultOpcRpcClient extends BaseOpcRpcClient implements OpcRpcClient {

    @Override
    protected void doInit(Properties properties) {

    }

    /**
     * Reconnect server.
     * <p>Build a new connection and then close the old one.</p>
     */
    public void reconnect() throws OpcConnectionException {
        this.reconnect(Endpoint.randomOneExclude(this.endpoints, this.currentConnection.getEndpoint()), false);
    }

}
