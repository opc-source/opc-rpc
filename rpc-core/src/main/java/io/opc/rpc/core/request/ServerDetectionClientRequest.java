package io.opc.rpc.core.request;

import io.opc.rpc.api.request.ClientRequest;

/**
 * ServerDetectionClientRequest. Detection request from Client.
 *
 * @author caihongwen
 * @version Id: ServerDetectionClientRequest.java, v 0.1 2022年06月05日 21:07 caihongwen Exp $
 */
public class ServerDetectionClientRequest extends ClientRequest {

    @Override
    public String toString() {
        return "ServerDetectionClientRequest{" +
                "requestId='" + requestId + '\'' +
                '}';
    }

}
