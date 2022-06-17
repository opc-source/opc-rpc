package io.opc.rpc.server;

import io.opc.rpc.api.OpcRpcServer;
import io.opc.rpc.api.constant.Constants.AUTH;
import io.opc.rpc.server.auth.AccessTokenManager;
import io.opc.rpc.server.auth.jwt.JwtTokenManager;
import io.opc.rpc.server.handle.LoginRequestHandler;
import java.util.Properties;

/**
 * DefaultOpcRpcServer.
 *
 * @author caihongwen
 * @version Id: DefaultOpcRpcServer.java, v 0.1 2022年06月02日 22:38 caihongwen Exp $
 */
public class DefaultOpcRpcServer extends BaseOpcRpcServer implements OpcRpcServer {

    @Override
    protected void doInit(Properties properties) {

        // TODO put it here temporarily
        final boolean enabled = (Boolean) properties.getOrDefault(AUTH.KEY_OPC_RPC_AUTH_ENABLED, true);
        final String authType = (String) properties.getOrDefault(AUTH.KEY_OPC_RPC_AUTH_TYPE, AccessTokenManager.DEFAULT_AUTH_TYPE);
        if (enabled && AccessTokenManager.DEFAULT_AUTH_TYPE.equals(authType)) {
            LoginRequestHandler.setAccessTokenManager(new JwtTokenManager(properties));
        }
    }

}
