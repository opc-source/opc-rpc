package io.opc.rpc.api.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ExceptionCode.
 *
 * @author mengyuan
 * @version Id: ExceptionCode.java, v 0.1 2022年06月09日 19:38 mengyuan Exp $
 */
@AllArgsConstructor
@Getter
public enum ExceptionCode {

    // 6666 ~ 7777, the exception code range, for opc-rpc inner

    /**
     * Unknown.
     */
    UNKNOWN(6666, "Unknown."),

    /**
     * Init Client Fail.
     */
    INIT_CLIENT_FAIL(6667, "Init Client Fail."),

    /**
     * Init Server Fail.
     */
    INIT_SERVER_FAIL(6668, "Init Server Fail."),

    /// ---------------------------------------------------------  ///

    /**
     * RequestId Conflict.
     */
    REQUEST_ID_CONFLICT(6700, "RequestId Conflict."),

    /// ---------------------------------------------------------  ///

    /**
     * Connection Error.
     */
    CONNECTION_ERROR(6800, "Connection Error."),

    /**
     * Connection Unhealthy.
     */
    CONNECTION_UNHEALTHY(6801, "Connection Unhealthy."),

    /// ---------------------------------------------------------  ///

    /**
     * Register Load Class Error.
     */
    REGISTER_LOAD_CLASS_ERROR(6901, "Register Load Class Error."),

    ;

    private final int code;

    private final String message;

}
