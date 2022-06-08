package io.opc.rpc.core.request;

import io.opc.rpc.api.request.ServerRequest;
import io.opc.rpc.core.Endpoint;
import io.opc.rpc.core.annotation.Internal;
import lombok.Getter;
import lombok.Setter;

/**
 * ConnectionResetServerRequest. Connection reset request from Server.
 *
 * @author caihongwen
 * @version Id: ConnectionResetServerRequest.java, v 0.1 2022年06月09日 00:55 caihongwen Exp $
 */
@Internal
@Getter
@Setter
public class ConnectionResetServerRequest extends ServerRequest {

    /**
     * endpoint can be null.
     */
    private Endpoint endpoint;

    @Override
    public String toString() {
        return "ConnectionResetServerRequest{" +
                "requestId='" + requestId + '\'' +
                ", endpoint=" + endpoint +
                '}';
    }

}
