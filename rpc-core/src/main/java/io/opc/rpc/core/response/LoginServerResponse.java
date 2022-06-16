package io.opc.rpc.core.response;

import io.opc.rpc.api.response.ServerResponse;
import io.opc.rpc.core.annotation.Internal;
import lombok.Getter;
import lombok.Setter;

/**
 * LoginServerResponse.
 *
 * @author caihongwen
 * @version Id: LoginServerResponse.java, v 0.1 2022年06月16日 11:52 caihongwen Exp $
 */
@Internal
@Getter
@Setter
public class LoginServerResponse extends ServerResponse {

    private String accessToken;

    @Override
    public String toString() {
        return "LoginServerResponse{" +
                "requestId='" + requestId + '\'' +
                ", accessToken='" + accessToken + '\'' +
                "} ";
    }

}
