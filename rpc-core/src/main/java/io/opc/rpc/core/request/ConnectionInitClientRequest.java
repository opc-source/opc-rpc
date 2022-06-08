package io.opc.rpc.core.request;

import io.opc.rpc.api.request.ClientRequest;
import io.opc.rpc.core.annotation.Internal;

/**
 * ConnectionInitClientRequest.
 *
 * @author caihongwen
 * @version Id: ConnectionInitClientRequest.java, v 0.1 2022年06月03日 11:17 caihongwen Exp $
 */
@Internal
public class ConnectionInitClientRequest extends ClientRequest {

    @Override
    public String toString() {
        return "ConnectionInitClientRequest{" +
                "requestId='" + requestId + '\'' +
                '}';
    }

}
