package io.opc.rpc.core.handle;

import io.opc.rpc.api.RequestHandler;
import io.opc.rpc.api.request.Request;
import io.opc.rpc.api.response.Response;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.experimental.UtilityClass;

/**
 * RequestHandlerSupport.
 *
 * @author caihongwen
 * @version Id : RequestHandlerSupport.java, v 0.1 2022年06月05日 17:19 caihongwen Exp $
 */
@UtilityClass
@SuppressWarnings({"rawtypes", "unchecked"})
public class RequestHandlerSupport {

    /**
     * Request.className -> RequestHandler
     */
    private static final Map<String, RequestHandler> REQUEST_HANDLER_MAP = new ConcurrentHashMap<>();

    public static void register(Class<?> requestClass, RequestHandler requestHandler) {
        REQUEST_HANDLER_MAP.put(requestClass.getName(), requestHandler);
    }

    public static Response handleRequest(Request request) {
        final RequestHandler requestHandler = REQUEST_HANDLER_MAP.get(request.getClass().getName());
        if (requestHandler == null) {
            return null;
        }
        return requestHandler.requestReply(request);
    }

}
