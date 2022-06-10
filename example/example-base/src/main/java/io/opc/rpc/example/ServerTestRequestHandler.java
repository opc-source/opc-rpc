package io.opc.rpc.example;

import io.opc.rpc.core.handle.BaseRequestHandler;

/**
 * @author mengyuan
 * @version Id: ServerTestRequestHandler.java, v 0.1 2022年06月10日 11:58 mengyuan Exp $
 */
public class ServerTestRequestHandler extends BaseRequestHandler<ServerTestServerRequest, ServerTestClientResponse> {

    @Override
    protected ServerTestClientResponse doReply(ServerTestServerRequest request) {
        final ServerTestClientResponse response = new ServerTestClientResponse();
        response.setServer(request.getServer());
        return response ;
    }

}
