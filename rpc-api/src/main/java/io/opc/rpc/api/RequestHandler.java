package io.opc.rpc.api;

import io.opc.rpc.api.request.Request;
import io.opc.rpc.api.response.Response;

/**
 * RequestHandler. process the request.
 *
 * @author caihongwen
 * @version Id: RequestHandler.java, v 0.1 2022年06月05日 17:02 caihongwen Exp $
 */
public interface RequestHandler<Req extends Request, Resp extends Response> {

    /**
     * Handle the request.
     *
     * @param request request
     * @return response.
     */
    Resp requestReply(Req request);

}
