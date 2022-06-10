package io.opc.rpc.api;

import io.opc.rpc.api.exception.OpcRpcRuntimeException;
import java.util.Properties;

/**
 * OpcRpcServer.
 *
 * @author caihongwen
 * @version Id: OpcRpcServer.java, v 0.1 2022年06月02日 22:01 caihongwen Exp $
 */
public interface OpcRpcServer extends AutoCloseable{

    /**
     * init.
     *
     * @param properties {@link Properties}
     */
    void init(Properties properties) throws OpcRpcRuntimeException;

}
