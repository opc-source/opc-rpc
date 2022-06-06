package io.opc.rpc.core.handle;

import io.opc.rpc.core.request.ClientDetectionServerRequest;
import io.opc.rpc.core.response.ClientDetectionClientResponse;

/**
 * ClientDetectionRequestHandler.
 *
 * @author caihongwen
 * @version Id: ClientDetectionRequestHandler.java, v 0.1 2022年06月05日 21:40 caihongwen Exp $
 */
public class ClientDetectionRequestHandler extends BaseRequestHandler<ClientDetectionServerRequest, ClientDetectionClientResponse> {

    @Override
    protected ClientDetectionClientResponse doReply(ClientDetectionServerRequest request) {
        ClientDetectionClientResponse detectionResponse = new ClientDetectionClientResponse();
        detectionResponse.setRequestId(request.getRequestId());
        return detectionResponse;
    }

}
