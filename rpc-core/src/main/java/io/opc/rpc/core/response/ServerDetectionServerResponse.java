package io.opc.rpc.core.response;

import io.opc.rpc.api.response.ClientResponse;
import io.opc.rpc.core.annotation.Internal;

/**
 * ServerDetectionServerResponse. Detection response from Server.
 *
 * @author caihongwen
 * @version Id: ServerDetectionServerResponse.java, v 0.1 2022年06月05日 21:08 caihongwen Exp $
 */
@Internal
public class ServerDetectionServerResponse extends ClientResponse {

    @Override
    public String toString() {
        return "ServerDetectionServerResponse{" +
                "requestId='" + requestId + '\'' +
                '}';
    }

}
