package io.opc.rpc.core.response;

import io.opc.rpc.api.response.ServerResponse;
import io.opc.rpc.core.annotation.Internal;
import lombok.Getter;
import lombok.Setter;

/**
 * ConnectionCheckServerResponse.
 *
 * @author mengyuan
 * @version Id: ConnectionCheckServerResponse.java, v 0.1 2022年06月03日 11:52 mengyuan Exp $
 */
@Internal
@Getter
@Setter
public class ConnectionCheckServerResponse extends ServerResponse {

    private String connectionId;

    @Override
    public String toString() {
        return "ConnectionCheckServerResponse{" +
                "connectionId='" + connectionId + '\'' +
                ", requestId='" + requestId + '\'' +
                "} ";
    }

}
