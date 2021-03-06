package io.opc.rpc.api;

import io.opc.rpc.api.constant.Constants;
import io.opc.rpc.api.exception.ExceptionCode;
import io.opc.rpc.api.exception.OpcRpcRuntimeException;
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
    public OpcRpcClient createOpcClient(String serverAddress) throws OpcRpcRuntimeException {
        Objects.requireNonNull(serverAddress, "serverAddress is null");

        Properties properties = new Properties();
        properties.put(Constants.Client.KEY_OPC_RPC_CLIENT_SERVER_ADDRESS, serverAddress);
        return createOpcClient(properties);
    }

    /**
     * Create OpcClient.
     *
     * @param properties {@link Properties}
     * @return {@link OpcRpcClient}
     */
    public OpcRpcClient createOpcClient(Properties properties) throws OpcRpcRuntimeException {
        Objects.requireNonNull(properties, "properties is null");

        try {
            final Class<?> clazz = Class.forName("io.opc.rpc.client.DefaultOpcRpcClient");
            final OpcRpcClient opcRpcClient = (OpcRpcClient) clazz.newInstance();

            opcRpcClient.init(properties);
            return opcRpcClient;
        } catch (OpcRpcRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new OpcRpcRuntimeException(ExceptionCode.INIT_CLIENT_FAIL, e);
        }
    }

    /**
     * Create OpcServer.
     *
     * @param properties {@link Properties}
     * @return {@link OpcRpcServer}
     */
    public OpcRpcServer createOpcServer(Properties properties) throws OpcRpcRuntimeException {
        Objects.requireNonNull(properties, "properties is null");

        try {
            final Class<?> clazz = Class.forName("io.opc.rpc.server.DefaultOpcRpcServer");
            final OpcRpcServer opcRpcServer = (OpcRpcServer) clazz.newInstance();

            opcRpcServer.init(properties);
            return opcRpcServer;
        } catch (OpcRpcRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new OpcRpcRuntimeException(ExceptionCode.INIT_SERVER_FAIL, e);
        }
    }

}
