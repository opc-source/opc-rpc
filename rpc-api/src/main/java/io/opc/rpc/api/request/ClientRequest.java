package io.opc.rpc.api.request;

/**
 * ClientRequest. Request from Client.
 *
 * @author caihongwen
 * @version Id: ClientRequest.java, v 0.1 2022年06月02日 21:44 caihongwen Exp $
 */
public abstract class ClientRequest extends Request {

    /**
     * odd by client.
     */
    private static final RequestIdHelper REQUEST_ID_HELPER = new RequestIdHelper(1, 2);

    protected String generateRequestId() {
        return REQUEST_ID_HELPER.generateRequestId();
    }

}
