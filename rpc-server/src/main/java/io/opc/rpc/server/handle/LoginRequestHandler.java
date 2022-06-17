package io.opc.rpc.server.handle;

import io.opc.rpc.core.handle.BaseRequestHandler;
import io.opc.rpc.core.request.LoginClientRequest;
import io.opc.rpc.core.response.LoginServerResponse;
import io.opc.rpc.server.auth.AccessTokenManager;

/**
 * LoginRequestHandler.
 *
 * @author caihongwen
 * @version Id: LoginRequestHandler.java, v 0.1 2022年06月16日 21:40 caihongwen Exp $
 */
public class LoginRequestHandler extends BaseRequestHandler<LoginClientRequest, LoginServerResponse> {

    private static final String NO_AUTH = "NO-AUTH";

    private static AccessTokenManager accessTokenManager;

    /**
     * public for setAccessTokenManager only once.
     */
    public static void setAccessTokenManager(AccessTokenManager accessTokenManager) {
        if (LoginRequestHandler.accessTokenManager != null) {
            throw new UnsupportedOperationException("accessTokenManager already set.");
        }
        LoginRequestHandler.accessTokenManager = accessTokenManager;
    }

    @Override
    protected LoginServerResponse doReply(LoginClientRequest request) {
        LoginServerResponse response = new LoginServerResponse();
        response.setRequestId(request.getRequestId());
        if (accessTokenManager != null) {
            // TODO login check username & password, pass will build and return accessToken
            response.setAccessToken(accessTokenManager.createToken(request.getUsername()));
        } else {
            response.setAccessToken(NO_AUTH);
        }
        return response;
    }

}
