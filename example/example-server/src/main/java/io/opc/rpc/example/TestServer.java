package io.opc.rpc.example;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.opc.rpc.api.Connection;
import io.opc.rpc.api.OpcRpcFactory;
import io.opc.rpc.api.OpcRpcServer;
import io.opc.rpc.api.constant.Constants.Server;
import io.opc.rpc.core.util.PayloadClassHelper;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

/**
 * @author mengyuan
 * @version Id: TestServer.java, v 0.1 2022年06月10日 13:55 mengyuan Exp $
 */
@Slf4j
public class TestServer {

    private static final AtomicBoolean STOP = new AtomicBoolean(false);

    public static void main(String[] args) throws Exception {
        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.WARN);

        final Properties properties1 = new Properties();
        properties1.put(Server.KEY_OPC_RPC_SERVER_PORT, 6666);
        final OpcRpcServer rpcServer1 = getOpcRpcServer(properties1);

        final Properties properties2 = new Properties();
        properties2.put(Server.KEY_OPC_RPC_SERVER_PORT, 6667);
        final OpcRpcServer rpcServer2 = getOpcRpcServer(properties2);

        //noinspection AlibabaAvoidManuallyCreateThread
        new Thread(() -> {
            while (!STOP.get()) {
                for (Connection connection : rpcServer1.getConnections()) {
                    sendServerTestServerRequest(connection);
                }

                for (Connection connection : rpcServer2.getConnections()) {
                    sendServerTestServerRequest(connection);
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException ignore) {
                    // ignore
                    Thread.currentThread().interrupt();
                }
            }
        }).start();

        TimeUnit.SECONDS.sleep(12);
        rpcServer1.close();
        TimeUnit.SECONDS.sleep(120);
        STOP.set(true);
        rpcServer2.close();
        TimeUnit.MILLISECONDS.sleep(100);
    }

    private static void sendServerTestServerRequest(Connection connection) {
        final ServerTestServerRequest testServerRequest = new ServerTestServerRequest();
        testServerRequest.setServer(String.valueOf(System.currentTimeMillis()));
        try {
            connection.requestAsync(testServerRequest);
        } catch (Exception e) {
            // ignore
            log.error("connection.asyncRequest error,{}", connection, e);
        }
    }

    private static OpcRpcServer getOpcRpcServer(Properties properties2) {
        final OpcRpcServer rpcServer = OpcRpcFactory.createOpcServer(properties2);

        PayloadClassHelper.register(ClientTestClientRequest.class, ClientTestServerResponse.class);
        rpcServer.registerClientRequestHandler(new ClientTestRequestHandler());
        PayloadClassHelper.register(ServerTestServerRequest.class, ServerTestClientResponse.class);

        return rpcServer;
    }

}
