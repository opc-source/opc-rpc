package io.opc.rpc.core.request;

import io.opc.rpc.api.request.ServerRequest;
import io.opc.rpc.core.annotation.Internal;

/**
 * ClientDetectionServerRequest. Detection request from Server.
 *
 * @author caihongwen
 * @version Id: ClientDetectionServerRequest.java, v 0.1 2022年06月05日 21:07 caihongwen Exp $
 */
@Internal
public class ClientDetectionServerRequest extends ServerRequest {

    @Override
    public String toString() {
        return "ClientDetectionServerRequest{" +
                "requestId='" + requestId + '\'' +
                '}';
    }

}
