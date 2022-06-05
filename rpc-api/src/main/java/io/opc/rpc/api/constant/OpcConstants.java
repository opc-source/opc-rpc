
package io.opc.rpc.api.constant;

/**
 * Opc Constants.
 *
 * @author caihongwen
 * @version Id: OpcConstants.java, v 0.1 2022年06月02日 22:08 caihongwen Exp $
 */
public interface OpcConstants {

    String COLON = ":";

    /**
     * opc.rpc.client.name
     */
    String KEY_OPC_RPC_CLIENT_NAME = "opc.rpc.client.name";

    interface Server {

        /**
         * opc.server.host
         */
        String KEY_OPC_RPC_SERVER_HOST = "opc.rpc.server.host";

        /**
         * opc.server.port
         */
        String KEY_OPC_RPC_SERVER_PORT = "opc.rpc.server.port";

        /**
         * 6666
         */
        int DEFAULT_OPC_RPC_SERVER_PORT = 6666;

    }

}
