package io.opc.rpc.server.auth;

/**
 * AccessTokenManager.
 *
 * @author caihongwen
 * @version Id: AccessTokenManager.java, v 0.1 2022年06月17日 16:29 caihongwen Exp $
 */
public interface AccessTokenManager {

    String DEFAULT_AUTH_TYPE = "jwt";

    /**
     * Create token.
     *
     * @param username auth Subject
     * @return token
     */
    String createToken(String username);

    /**
     * validate token.
     *
     * @param token token
     * @throws RuntimeException RuntimeException
     */
    void validateToken(String token) throws RuntimeException;

}
