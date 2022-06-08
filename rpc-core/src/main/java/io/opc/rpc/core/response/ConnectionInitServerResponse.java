package io.opc.rpc.core.response;

import io.opc.rpc.api.response.ServerResponse;
import io.opc.rpc.core.annotation.Internal;
import lombok.Getter;
import lombok.Setter;

/**
 * ConnectionInitServerResponse.
 *
 * @author caihongwen
 * @version Id: ConnectionInitServerResponse.java, v 0.1 2022年06月03日 11:52 caihongwen Exp $
 */
@Internal
@Getter
@Setter
public class ConnectionInitServerResponse extends ServerResponse {

    private String connectionId;

    @Override
    public String toString() {
        return "ConnectionInitServerResponse{" +
                "connectionId='" + connectionId + '\'' +
                ", requestId='" + requestId + '\'' +
                "} ";
    }

}
