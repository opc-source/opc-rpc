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
 * @author hongwen.chw@antgroup.com
 * @version : DefaultOpcRpcServerTest, v0.1 2022-06-05 14:57 mengyuan Exp $
 */
public class DefaultOpcRpcServerTest {

    @Test
    @Ignore
    public void doInit() throws InterruptedException {
        final Properties properties = new Properties();
        final OpcRpcServer rpcServer = OpcRpcFactory.createOpcServer(properties);

        Assert.assertNotNull(rpcServer);
        TimeUnit.SECONDS.sleep(20);
    }

}
