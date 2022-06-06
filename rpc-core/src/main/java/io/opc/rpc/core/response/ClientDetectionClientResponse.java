package io.opc.rpc.core.response;

import io.opc.rpc.api.response.ClientResponse;

/**
 * ClientDetectionClientResponse. Detection response from Server.
 *
 * @author caihongwen
 * @version Id: ClientDetectionClientResponse.java, v 0.1 2022年06月05日 21:08 caihongwen Exp $
 */
public class ClientDetectionClientResponse extends ClientResponse {

    @Override
    public String toString() {
        return "ClientDetectionClientResponse{" +
                "requestId='" + requestId + '\'' +
                '}';
    }

}
