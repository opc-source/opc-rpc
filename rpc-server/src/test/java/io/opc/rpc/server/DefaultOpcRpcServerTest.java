package io.opc.rpc.server;

import io.opc.rpc.api.OpcRpcFactory;
import io.opc.rpc.api.OpcRpcServer;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * DefaultOpcRpcServerTest.
 *
 * @author caihongwen
 * @version : DefaultOpcRpcServerTest, v0.1 2022-06-05 14:57 caihongwen Exp $
 */
public class DefaultOpcRpcServerTest {

    @Test
    @Ignore
    public void doInit() throws Exception {
        final Properties properties = new Properties();
        final OpcRpcServer rpcServer = OpcRpcFactory.createOpcServer(properties);

        Assert.assertNotNull(rpcServer);
        TimeUnit.SECONDS.sleep(20);
        rpcServer.close();
        TimeUnit.MILLISECONDS.sleep(100);
    }

}
