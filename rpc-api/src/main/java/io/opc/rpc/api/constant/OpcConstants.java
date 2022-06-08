
package io.opc.rpc.api.constant;

/**
 * Opc Constants.
 *
 * @author caihongwen
 * @version Id: OpcConstants.java, v 0.1 2022年06月02日 22:08 caihongwen Exp $
 */
public interface OpcConstants {

    String COLON = ":";

    String COMMA = ",";

    interface Client {

        /**
         * opc.rpc.client.name
         */
        String KEY_OPC_RPC_CLIENT_NAME = "opc.rpc.client.name";

        /**
         * opc.rpc.client.serverAddress=host:port or ip:port
         * <li>eg1 : localhost</li>
         * <li>eg2 : localhost:12345</li>
         * <li>eg3 : localhost:12345,domain:12344,127.0.0.1:12343</li>
         */
        String KEY_OPC_RPC_CLIENT_SERVER_ADDRESS = "opc.rpc.client.serverAddress";

    }

    interface Server {

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
