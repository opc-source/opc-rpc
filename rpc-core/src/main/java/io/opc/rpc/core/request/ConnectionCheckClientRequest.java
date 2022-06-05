package io.opc.rpc.core.request;

import io.opc.rpc.api.request.ClientRequest;
import io.opc.rpc.core.annotation.Internal;

/**
 * ConnectionCheckClientRequest.
 *
 * @author mengyuan
 * @version Id: ConnectionCheckClientRequest.java, v 0.1 2022年06月03日 11:17 mengyuan Exp $
 */
@Internal
public class ConnectionCheckClientRequest extends ClientRequest {

    @Override
    public String toString() {
        return "ConnectionCheckClientRequest{" +
                "requestId='" + requestId + '\'' +
                '}';
    }

}
