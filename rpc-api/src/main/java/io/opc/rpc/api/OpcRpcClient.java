package io.opc.rpc.api;

import java.util.Properties;

/**
 * OpcRpcClient.
 *
 * @author caihongwen
 * @version Id: OpcRpcClient.java, v 0.1 2022年06月02日 22:01 caihongwen Exp $
 */
public interface OpcRpcClient extends AutoCloseable {

    /**
     * init.
     *
     * @param properties {@link Properties}
     */
    void init(Properties properties);

}
