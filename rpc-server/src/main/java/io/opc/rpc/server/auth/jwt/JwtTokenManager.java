package io.opc.rpc.server.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import io.opc.rpc.api.constant.Constants.AUTH;
import io.opc.rpc.server.auth.AccessTokenManager;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;
import lombok.Getter;

/**
 * JwtTokenManager.
 *
 * @author caihongwen
 * @version Id: JwtTokenManager.java, v 0.1 2022年06月17日 14:29 caihongwen Exp $
 */
public class JwtTokenManager implements AccessTokenManager {

    @Getter
    private final boolean enabled;

    private final long authExpire;

    private final String authSecretKey;

    private byte[] authSecretKeyBytes;

    private JwtParser jwtParser;

    public JwtTokenManager(Properties properties) {
        this.enabled = (Boolean) properties.getOrDefault(AUTH.KEY_OPC_RPC_AUTH_ENABLED, true);
        this.authExpire = (Long) properties.getOrDefault(AUTH.KEY_OPC_RPC_AUTH_EXPIRE, AUTH.DEFAULT_OPC_RPC_AUTH_EXPIRE);
        this.authSecretKey = properties.getProperty(AUTH.KEY_OPC_RPC_AUTH_SECRET_KEY);
        if (this.enabled) {
            Objects.requireNonNull(this.authSecretKey, AUTH.KEY_OPC_RPC_AUTH_SECRET_KEY + " have to set, auth is enabled");
            this.jwtParser = Jwts.parserBuilder().setSigningKey(this.getSecretKeyBytes()).build();
        }
    }

    private byte[] getSecretKeyBytes() {
        if (this.authSecretKeyBytes == null) {
            try {
                this.authSecretKeyBytes = Decoders.BASE64.decode(this.authSecretKey);
            } catch (DecodingException e) {
                this.authSecretKeyBytes = this.authSecretKey.getBytes(StandardCharsets.UTF_8);
            }
        }
        return this.authSecretKeyBytes;
    }

    /**
     * @see AccessTokenManager#createToken(String)
     */
    @Override
    public String createToken(String username) {
        final Date expiration = new Date(System.currentTimeMillis() + this.authExpire);
        final Claims claims = Jwts.claims().setSubject(username);
        return Jwts.builder().setClaims(claims).setExpiration(expiration)
                .signWith(Keys.hmacShaKeyFor(this.getSecretKeyBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * @see AccessTokenManager#validateToken(String)
     */
    @Override
    public void validateToken(String token) throws ExpiredJwtException, UnsupportedJwtException,
            MalformedJwtException, SignatureException, IllegalArgumentException {
        this.jwtParser.parseClaimsJws(token);
    }

}
