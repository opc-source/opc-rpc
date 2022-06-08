
package io.opc.rpc.api.constant;

import java.util.concurrent.TimeUnit;

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

        /**
         * opc.rpc.client.keepActive=5000L
         */
        String KEY_OPC_RPC_CLIENT_KEEP_ACTIVE = "opc.rpc.client.keepActive";

        /**
         * default 5000L
         */
        long DEFAULT_OPC_RPC_CLIENT_KEEP_ACTIVE = TimeUnit.SECONDS.toMillis(5);

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

        /**
         * opc.rpc.server.keepActive=20000L
         */
        String KEY_OPC_RPC_SERVER_KEEP_ACTIVE = "opc.rpc.server.keepActive";

        /**
         * default 20000L, 4 * times than client' health check server
         */
        long DEFAULT_OPC_RPC_SERVER_KEEP_ACTIVE = 4 * Client.DEFAULT_OPC_RPC_CLIENT_KEEP_ACTIVE;

    }

}
