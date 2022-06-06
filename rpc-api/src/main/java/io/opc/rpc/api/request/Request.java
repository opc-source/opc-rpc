package io.opc.rpc.api.request;

import io.opc.rpc.api.Payload;

/**
 * Request.
 *
 * @author caihongwen
 * @version Id: Request.java, v 0.1 2022年06月02日 21:42 caihongwen Exp $
 */
public abstract class Request implements Payload {

    {
        requestId = generateRequestId();
    }

    protected abstract String generateRequestId();

    /**
     * RequestId generate on construct.
     */
    protected final String requestId;

    /**
     * Getter method for property <tt>requestId</tt>.
     *
     * @return property value of requestId
     */
    public String getRequestId() {
        return requestId;
    }

}
