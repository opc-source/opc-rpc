package io.opc.rpc.core.response;

import io.opc.rpc.api.response.ServerResponse;
import io.opc.rpc.core.annotation.Internal;
import lombok.Getter;
import lombok.Setter;

/**
 * ConnectionSetupServerResponse.
 *
 * @author mengyuan
 * @version Id: ConnectionSetupServerResponse.java, v 0.1 2022年06月03日 11:52 mengyuan Exp $
 */
@Internal
@Getter
@Setter
public class ConnectionSetupServerResponse extends ServerResponse {

    @Override
    public String toString() {
        return "ConnectionSetupServerResponse{" +
                "requestId='" + requestId + '\'' +
                '}';
    }
}
