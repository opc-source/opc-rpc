package io.opc.rpc.api.response;

import io.opc.rpc.api.Payload;

/**
 * Response.
 *
 * @author caihongwen
 * @version Id: Response.java, v 0.1 2022年06月02日 21:42 caihongwen Exp $
 */
public abstract class Response implements Payload {

    protected String requestId;

    /**
     * Getter method for property <tt>requestId</tt>.
     *
     * @return property value of requestId
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Setter method for property <tt>requestId</tt>.
     *
     * @param requestId value to be assigned to property requestId
     */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

}
