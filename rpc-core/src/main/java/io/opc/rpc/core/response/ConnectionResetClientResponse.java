package io.opc.rpc.core.response;

import io.opc.rpc.api.response.ClientResponse;
import io.opc.rpc.core.annotation.Internal;

/**
 * ConnectionResetClientResponse. Connection reset response from Client.
 *
 * @author caihongwen
 * @version Id: ConnectionResetClientResponse.java, v 0.1 2022年06月05日 21:08 caihongwen Exp $
 */
@Internal
public class ConnectionResetClientResponse extends ClientResponse {

    @Override
    public String toString() {
        return "ConnectionResetClientResponse{" +
                "requestId='" + requestId + '\'' +
                '}';
    }

}
