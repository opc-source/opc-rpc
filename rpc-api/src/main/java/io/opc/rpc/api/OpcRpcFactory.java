package io.opc.rpc.api;

import io.opc.rpc.api.constant.OpcConstants;
import java.util.Objects;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

/**
 * OpcRpcFactory.
 *
 * @author caihongwen
 * @version Id: OpcRpcFactory.java, v 0.1 2022年06月02日 22:00 caihongwen Exp $
 */
@Slf4j
public class OpcRpcFactory {

    /**
     * Create OpcClient.
     *
     * @param serverHost host or ip
     * @return {@link OpcRpcClient}
     */
    public OpcRpcClient createOpcClient(String serverHost) {
        Objects.requireNonNull(serverHost, "serverHost is null");

        Properties properties = new Properties();
        properties.put(OpcConstants.Server.KEY_OPC_RPC_SERVER_HOST, serverHost);
        properties.put(OpcConstants.Server.KEY_OPC_RPC_SERVER_PORT, OpcConstants.Server.DEFAULT_OPC_RPC_SERVER_PORT);
        return createOpcClient(properties);
    }

    /**
     * Create OpcClient.
     *
     * @param properties {@link Properties}
     * @return {@link OpcRpcClient}
     */
    public OpcRpcClient createOpcClient(Properties properties) {
        Objects.requireNonNull(properties, "properties is null");

        try {
            final Class<?> clazz = Class.forName("io.opc.rpc.core.DefaultOpcRpcClient");
            final OpcRpcClient opcRpcClient = (OpcRpcClient) clazz.newInstance();

            opcRpcClient.init(properties);
            return opcRpcClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
