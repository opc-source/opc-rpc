package io.opc.rpc.api.request;

/**
 * ServerRequest. Request from Server.
 *
 * @author caihongwen
 * @version Id: ServerRequest.java, v 0.1 2022年06月02日 21:44 caihongwen Exp $
 */
public abstract class ServerRequest implements Request {

    /**
     * even by server.
     */
    private static final RequestIdHelper REQUEST_ID_HELPER = new RequestIdHelper(0, 2);

    /**
     * RequestId generate on construct.
     */
    protected final String requestId = REQUEST_ID_HELPER.generateRequestId();

    @Override
    public String getRequestId() {
        return requestId;
    }

}
