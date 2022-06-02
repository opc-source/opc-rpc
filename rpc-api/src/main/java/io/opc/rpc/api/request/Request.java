package io.opc.rpc.api.request;

import io.opc.rpc.api.Payload;

/**
 * Request.
 *
 * @author caihongwen
 * @version Id: Request.java, v 0.1 2022年06月02日 21:42 caihongwen Exp $
 */
public interface Request extends Payload {

    /**
     * Get requestId.
     *
     * @return requestId
     */
    String getRequestId();

}
