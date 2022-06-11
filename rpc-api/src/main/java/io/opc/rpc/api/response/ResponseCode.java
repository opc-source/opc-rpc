package io.opc.rpc.api.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ResponseCode.
 *
 * @author mengyuan
 * @version Id: ResponseCode.java, v 0.1 2022年06月09日 19:38 mengyuan Exp $
 */
@AllArgsConstructor
@Getter
public enum ResponseCode {

    /**
     * OK
     */
    OK(200, "OK"),

    /**
     * Fail
     */
    FAIL(500, "Fail"),

    /**
     * Unsupported payload
     */
    UNSUPPORTED_PAYLOAD(534, "Unsupported payload"),

    /**
     * HandleRequest get null
     */
    HANDLE_REQUEST_NULL(535, "HandleRequest get null, maybe has no relevant RequestHandler."),

    /**
     * Server unhealthy
     */
    SERVER_UNHEALTHY(555, "Server unhealthy."),

    // 534 ~ 566, the error code range, for opc-rpc inner

    // 567 ~ 599, the error code range, it is recommended to customize
    ;

    private final int code;

    private final String message;

}
