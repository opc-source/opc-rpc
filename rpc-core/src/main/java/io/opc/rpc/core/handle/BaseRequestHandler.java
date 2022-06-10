package io.opc.rpc.core.handle;

import io.opc.rpc.api.RequestHandler;
import io.opc.rpc.api.request.Request;
import io.opc.rpc.api.response.Response;
import io.opc.rpc.core.util.PayloadClassHelper;

/**
 * BaseRequestHandler.
 *
 * @author caihongwen
 * @version Id: BaseRequestHandler.java, v 0.1 2022年06月05日 17:09 caihongwen Exp $
 */
public abstract class BaseRequestHandler<Req extends Request, Resp extends Response> implements RequestHandler<Req, Resp> {

    {
        java.lang.reflect.ParameterizedType superGenericSuperclass =
                (java.lang.reflect.ParameterizedType) this.getClass().getGenericSuperclass();
        //noinspection unchecked
        final Class<? extends Request> requestType = (Class<? extends Request>) superGenericSuperclass.getActualTypeArguments()[0];
        //noinspection unchecked
        final Class<? extends Response> responseType = (Class<? extends Response>) superGenericSuperclass.getActualTypeArguments()[1];

        RequestHandlerSupport.register(requestType, this);

        PayloadClassHelper.register(requestType, responseType);
    }

    @Override
    public Resp requestReply(Req request) {
        return doReply(request);
    }

    /**
     * handle Request and reply Response.
     *
     * @param req Request
     * @return Response
     */
    protected abstract Resp doReply(Req req);

}