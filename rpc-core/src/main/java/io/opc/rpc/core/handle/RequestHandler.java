package io.opc.rpc.core.handle;

import io.opc.rpc.api.request.Request;
import io.opc.rpc.api.response.Response;

/**
 * RequestHandler. process the request.
 *
 * @author mengyuan
 * @version Id: RequestHandler.java, v 0.1 2022年06月05日 17:02 mengyuan Exp $
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
