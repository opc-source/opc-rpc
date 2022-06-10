package io.opc.rpc.example;

import io.opc.rpc.core.handle.BaseRequestHandler;

/**
 * @author mengyuan
 * @version Id: ClientTestRequestHandler.java, v 0.1 2022年06月10日 11:58 mengyuan Exp $
 */
public class ClientTestRequestHandler extends BaseRequestHandler<ClientTestClientRequest, ClientTestServerResponse> {

    @Override
    protected ClientTestServerResponse doReply(ClientTestClientRequest request) {
        final ClientTestServerResponse response = new ClientTestServerResponse();
        response.setTest(request.getTest());
        return response ;
    }

}
