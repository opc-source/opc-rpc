package io.opc.rpc.core.handle;

import io.opc.rpc.api.RequestHandler;
import io.opc.rpc.api.request.Request;
import io.opc.rpc.api.response.Response;
import io.opc.rpc.core.util.PayloadClassHelper;
import java.lang.reflect.ParameterizedType;

/**
 * BaseRequestHandler.
 *
 * @author caihongwen
 * @version Id: BaseRequestHandler.java, v 0.1 2022年06月05日 17:09 caihongwen Exp $
 */
public abstract class BaseRequestHandler<Req extends Request, Resp extends Response> implements RequestHandler<Req, Resp> {

    {
        ParameterizedType superGenericSuperclass = (ParameterizedType) this.getClass().getGenericSuperclass();
        final Class<?> requestType = (Class<?>) superGenericSuperclass.getActualTypeArguments()[0];
        final Class<?> responseType = (Class<?>) superGenericSuperclass.getActualTypeArguments()[1];

        //noinspection unchecked
        RequestHandlerSupport.register(requestType.getName(), (RequestHandler<Request, Response>) this);

        PayloadClassHelper.register(requestType, responseType);
    }

    @Override
    public Resp requestReply(Req request) {
        return doReply(request);
    }

    protected abstract Resp doReply(Req req);

}