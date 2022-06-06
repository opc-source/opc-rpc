package io.opc.rpc.api.request;

/**
 * ServerRequest. Request from Server.
 *
 * @author caihongwen
 * @version Id: ServerRequest.java, v 0.1 2022年06月02日 21:44 caihongwen Exp $
 */
public abstract class ServerRequest extends Request {

    /**
     * even by server.
     */
    private static final RequestIdHelper REQUEST_ID_HELPER = new RequestIdHelper(0, 2);

    protected String generateRequestId() {
        return REQUEST_ID_HELPER.generateRequestId();
    }

}
