package io.opc.rpc.api;

import io.opc.rpc.api.constant.OpcConstants;
import java.util.Objects;
import java.util.Properties;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * OpcRpcFactory.
 *
 * @author caihongwen
 * @version Id: OpcRpcFactory.java, v 0.1 2022年06月02日 22:00 caihongwen Exp $
 */
@Slf4j
@UtilityClass
public class OpcRpcFactory {

    /**
     * Create OpcClient.
     *
     * @param serverAddress host:port or ip:port, eg: localhost:12345,domain:12344,127.0.0.1:12343
     * @return {@link OpcRpcClient}
     */
    public OpcRpcClient createOpcClient(String serverAddress) {
        Objects.requireNonNull(serverAddress, "serverAddress is null");

        Properties properties = new Properties();
        properties.put(OpcConstants.Client.KEY_OPC_RPC_CLIENT_SERVER_ADDRESS, serverAddress);
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
            final Class<?> clazz = Class.forName("io.opc.rpc.client.DefaultOpcRpcClient");
            final OpcRpcClient opcRpcClient = (OpcRpcClient) clazz.newInstance();

            opcRpcClient.init(properties);
            return opcRpcClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create OpcServer.
     *
     * @param properties {@link Properties}
     * @return {@link OpcRpcServer}
     */
    public OpcRpcServer createOpcServer(Properties properties) {
        Objects.requireNonNull(properties, "properties is null");

        try {
            final Class<?> clazz = Class.forName("io.opc.rpc.server.DefaultOpcRpcServer");
            final OpcRpcServer opcRpcServer = (OpcRpcServer) clazz.newInstance();

            opcRpcServer.init(properties);
            return opcRpcServer;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
