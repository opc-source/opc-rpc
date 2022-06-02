package io.opc.rpc.api;

import java.util.Properties;

/**
 * OpcRpcClient.
 *
 * @author caihongwen
 * @version Id: OpcRpcClient.java, v 0.1 2022年06月02日 22:01 caihongwen Exp $
 */
public interface OpcRpcClient {

    /**
     * init.
     *
     * @param properties {@link Properties}
     */
    void init(Properties properties);

    /**
     * destroy.
     */
    void destroy();

}
