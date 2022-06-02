package io.opc.rpc.api.request;

/**
 * ClientRequest. Request from Client.
 *
 * @author caihongwen
 * @version Id: ClientRequest.java, v 0.1 2022年06月02日 21:44 caihongwen Exp $
 */
public abstract class ClientRequest implements Request {

    /**
     * odd by client.
     */
    private static final RequestIdHelper REQUEST_ID_HELPER = new RequestIdHelper(1, 2);

    /**
     * RequestId generate on construct.
     */
    private String requestId = REQUEST_ID_HELPER.generateRequestId();

    @Override
    public String getRequestId() {
        return requestId;
    }

}