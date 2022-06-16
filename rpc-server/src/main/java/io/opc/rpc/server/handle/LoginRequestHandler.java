package io.opc.rpc.server.handle;

import io.opc.rpc.core.handle.BaseRequestHandler;
import io.opc.rpc.core.request.LoginClientRequest;
import io.opc.rpc.core.response.LoginServerResponse;

/**
 * LoginRequestHandler.
 *
 * @author caihongwen
 * @version Id: LoginRequestHandler.java, v 0.1 2022年06月16日 21:40 caihongwen Exp $
 */
public class LoginRequestHandler extends BaseRequestHandler<LoginClientRequest, LoginServerResponse> {

    @Override
    protected LoginServerResponse doReply(LoginClientRequest request) {
        LoginServerResponse response = new LoginServerResponse();
        response.setRequestId(request.getRequestId());
        // TODO to build AccessToken
        response.setAccessToken(request.getUsername() + " TODO");
        return response;
    }

}
