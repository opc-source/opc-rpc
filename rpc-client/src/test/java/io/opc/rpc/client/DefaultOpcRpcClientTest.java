package io.opc.rpc.client;

import io.opc.rpc.api.OpcRpcClient;
import io.opc.rpc.api.OpcRpcFactory;
import io.opc.rpc.api.constant.Constants;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * DefaultOpcRpcClientTest.
 *
 * @author caihongwen
 * @version : DefaultOpcRpcClientTest, v0.1 2022-06-05 15:09 caihongwen Exp $
 */
public class DefaultOpcRpcClientTest {

    @Test
    @Ignore
    public void doInit() throws Exception {
        final Properties properties = new Properties();
        properties.setProperty(Constants.Client.KEY_OPC_RPC_CLIENT_NAME, "localTest");
        properties.setProperty(Constants.Client.KEY_OPC_RPC_CLIENT_SERVER_ADDRESS, "localhost,127.0.0.1:6666");

        final OpcRpcClient rpcClient = OpcRpcFactory.createOpcClient(properties);

        Assert.assertNotNull(rpcClient);
        TimeUnit.SECONDS.sleep(15);
        rpcClient.close();
        TimeUnit.MILLISECONDS.sleep(100);
    }

}
