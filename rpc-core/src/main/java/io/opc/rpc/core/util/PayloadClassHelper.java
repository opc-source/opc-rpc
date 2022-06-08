package io.opc.rpc.core.util;

import io.opc.rpc.core.handle.ClientDetectionRequestHandler;
import io.opc.rpc.core.request.ConnectionInitClientRequest;
import io.opc.rpc.core.request.ConnectionSetupClientRequest;
import io.opc.rpc.core.request.ServerDetectionClientRequest;
import io.opc.rpc.core.response.ConnectionInitServerResponse;
import io.opc.rpc.core.response.ConnectionSetupServerResponse;
import io.opc.rpc.core.response.ErrorResponse;
import io.opc.rpc.core.response.ServerDetectionServerResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.experimental.UtilityClass;

/**
 * PayloadClassHelper.
 *
 * @author caihongwen
 * @version Id : PayloadClassHelper.java, v 0.1 2022年06月05日 11:29 caihongwen Exp $
 */
@UtilityClass
public class PayloadClassHelper {

    /**
     * key: requestClassName
     * val: ResponseClass
     */
    private static final Map<String, Class<?>> REQUEST_WITH_RESPONSE_CLASS_MAP;

    /**
     * key: responseClassName
     * val: RequestClass
     */
    private static final Map<String, Class<?>> RESPONSE_WITH_REQUEST_CLASS_MAP;

    /**
     * key: className
     * val: Class
     */
    private static final Map<String, Class<?>> ALL_CLASS_NAP;

    static {
        REQUEST_WITH_RESPONSE_CLASS_MAP = new ConcurrentHashMap<>(16);
        RESPONSE_WITH_REQUEST_CLASS_MAP = new ConcurrentHashMap<>(16);
        ALL_CLASS_NAP = new ConcurrentHashMap<>(32);

        // register all @Internal class, Request & Response
        register(ConnectionInitClientRequest.class, ConnectionInitServerResponse.class);
        register(ConnectionSetupClientRequest.class, ConnectionSetupServerResponse.class);
        register(ServerDetectionClientRequest.class, ServerDetectionServerResponse.class);
        register(ErrorResponse.class);

        // TODO how to init?
        new ClientDetectionRequestHandler();
    }

    public void register(Class<?> requestClass, Class<?> responseClass) {

        REQUEST_WITH_RESPONSE_CLASS_MAP.put(requestClass.getName(), responseClass);
        RESPONSE_WITH_REQUEST_CLASS_MAP.put(responseClass.getName(), requestClass);

        ALL_CLASS_NAP.put(requestClass.getName(), requestClass);
        ALL_CLASS_NAP.put(responseClass.getName(), responseClass);
    }

    public void register(Class<?> clazz) {
        ALL_CLASS_NAP.put(clazz.getName(), clazz);
    }

    @SuppressWarnings("unchecked")
    public <T> Class<T> getClass(String className) {
        return (Class<T>) ALL_CLASS_NAP.get(className);
    }

    @SuppressWarnings("unchecked")
    public <T> Class<T> getRequestClassByResponse(String responseClassName) {
        return (Class<T>) RESPONSE_WITH_REQUEST_CLASS_MAP.get(responseClassName);
    }

    @SuppressWarnings("unchecked")
    public <T> Class<T> getResponseClassByRequest(String requestClassName) {
        return (Class<T>) REQUEST_WITH_RESPONSE_CLASS_MAP.get(requestClassName);
    }

}
