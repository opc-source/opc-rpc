package io.opc.rpc.api.exception;

import lombok.Getter;

/**
 * Opc Rpc RuntimeException.
 *
 * @author caihongwen
 * @version Id: OpcRpcRuntimeException.java, v 0.1 2022年06月09日 23:12 caihongwen Exp $
 */
public class OpcRpcRuntimeException extends RuntimeException {

    private static final long serialVersionUID = -7023388348540049828L;

    public static final String ERROR_MESSAGE_FORMAT = "errCode: %d, errMsg: %s";

    @Getter
    private final int errCode;

    public OpcRpcRuntimeException(ExceptionCode exceptionCode) {
        this(exceptionCode.getCode(), exceptionCode.getMessage());
    }

    public OpcRpcRuntimeException(ExceptionCode exceptionCode, Throwable throwable) {
        this(exceptionCode.getCode(), exceptionCode.getMessage(), throwable);
    }

    public OpcRpcRuntimeException(int errCode, String errMsg) {
        super(String.format(ERROR_MESSAGE_FORMAT, errCode, errMsg));
        this.errCode = errCode;
    }

    public OpcRpcRuntimeException(int errCode, Throwable throwable) {
        super(throwable);
        this.errCode = errCode;
    }

    public OpcRpcRuntimeException(int errCode, String errMsg, Throwable throwable) {
        super(String.format(ERROR_MESSAGE_FORMAT, errCode, errMsg), throwable);
        this.errCode = errCode;
    }

}
