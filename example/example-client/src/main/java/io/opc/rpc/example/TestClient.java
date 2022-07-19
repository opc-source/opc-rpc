package io.opc.rpc.example;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.opc.rpc.api.OpcRpcClient;
import io.opc.rpc.api.OpcRpcFactory;
import io.opc.rpc.api.RequestCallback;
import io.opc.rpc.api.constant.Constants;
import io.opc.rpc.api.exception.OpcConnectionException;
import io.opc.rpc.api.response.ErrorResponse;
import io.opc.rpc.core.util.PayloadClassHelper;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

/**
 * @author mengyuan
 * @version Id: TestClient.java, v 0.1 2022年06月10日 12:07 mengyuan Exp $
 */
@Slf4j
public class TestClient {

    private static final AtomicBoolean STOP = new AtomicBoolean(false);

    public static void main(String[] args) throws Exception {

        final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.WARN);

        final Properties properties = new Properties();
        properties.setProperty(Constants.Client.KEY_OPC_RPC_CLIENT_NAME, "localTest");
        properties.setProperty(Constants.Client.KEY_OPC_RPC_CLIENT_SERVER_ADDRESS, "localhost,127.0.0.1:6667");
        properties.setProperty(Constants.Client.KEY_OPC_RPC_CLIENT_USERNAME, "localTest");
        properties.setProperty(Constants.Client.KEY_OPC_RPC_CLIENT_PASSWORD, "localTest-passwd");

        final OpcRpcClient rpcClient1 = getOpcRpcClient(properties);
        properties.setProperty(Constants.Client.KEY_OPC_RPC_CLIENT_SERVER_ADDRESS, "localhost:6666,localhost:6667");
        final OpcRpcClient rpcClient2 = getOpcRpcClient(properties);
        properties.setProperty(Constants.Client.KEY_OPC_RPC_CLIENT_SERVER_ADDRESS, "127.0.0.1:6667");
        final OpcRpcClient rpcClient3 = getOpcRpcClient(properties);

        TimeUnit.SECONDS.sleep(ThreadLocalRandom.current().nextInt(15, 30));
        STOP.set(true);
        rpcClient1.close();
        rpcClient2.close();
        rpcClient3.close();
        TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(1, 100));
    }

    private static OpcRpcClient getOpcRpcClient(Properties properties) {
        final OpcRpcClient rpcClient = OpcRpcFactory.createOpcClient(properties);

        PayloadClassHelper.register(ClientTestClientRequest.class, ClientTestServerResponse.class);
        PayloadClassHelper.register(ServerTestServerRequest.class, ServerTestClientResponse.class);
        rpcClient.registerServerRequestHandler(new ServerTestRequestHandler());

        //noinspection AlibabaAvoidManuallyCreateThread
        new Thread(() -> {
            while (!STOP.get()) {
                final ClientTestClientRequest testClientRequest = new ClientTestClientRequest();
                testClientRequest.setTest(String.valueOf(System.currentTimeMillis()));
                try {
                    rpcClient.requestAsync(testClientRequest, new RequestCallback<ClientTestServerResponse>() {
                        @Override
                        public Executor getExecutor() {
                            return ForkJoinPool.commonPool();
                        }

                        @Override
                        public long getTimeout() {
                            return 1000L;
                        }

                        @Override
                        public void onTimeout() {
                            log.error("onTimeout : {}", testClientRequest);
                        }

                        @Override
                        public void onResponse(ClientTestServerResponse response) {
                            log.info("response : {}", response);
                        }

                        @Override
                        public void onError(ErrorResponse errorResponse) {
                            log.error("onError : {}", errorResponse);
                        }
                    });
                } catch (OpcConnectionException exception) {
                    log.error("rpcClient.asyncRequest error", exception);
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException ignore) {
                    // ignore
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
        return rpcClient;
    }

}
