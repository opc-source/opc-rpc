package io.opc.rpc.api;

import io.opc.rpc.api.exception.OpcRpcRuntimeException;
import io.opc.rpc.api.request.ClientRequest;
import io.opc.rpc.api.response.ServerResponse;
import java.util.Properties;

/**
 * OpcRpcServer.
 *
 * @author caihongwen
 * @version Id: OpcRpcServer.java, v 0.1 2022年06月02日 22:01 caihongwen Exp $
 */
public interface OpcRpcServer extends AutoCloseable {

    /**
     * init.
     *
     * @param properties {@link Properties}
     * @throws OpcRpcRuntimeException OpcRpcRuntimeException
     */
    void init(Properties properties) throws OpcRpcRuntimeException;

    /**
     * register a ClientRequestHandler who handle ClientRequest.
     *
     * @param requestClass requestClass
     * @param requestHandler RequestHandler
     */
    void registerClientRequestHandler(Class<? extends ClientRequest> requestClass,
            RequestHandler<? extends ClientRequest, ? extends ServerResponse> requestHandler);

}
