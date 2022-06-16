package io.opc.rpc.core.request;

import io.opc.rpc.api.request.ClientRequest;
import io.opc.rpc.core.annotation.Internal;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * LoginClientRequest.
 *
 * @author caihongwen
 * @version Id: LoginClientRequest.java, v 0.1 2022年06月16日 11:17 caihongwen Exp $
 */
@Internal
@Setter
@Getter
@Builder
public class LoginClientRequest extends ClientRequest {

    private String username;

    private String password;

    @Override
    public String toString() {
        return "LoginClientRequest{" +
                "requestId='" + requestId + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

}
