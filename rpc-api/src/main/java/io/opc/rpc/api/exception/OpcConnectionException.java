package io.opc.rpc.api.exception;

/**
 * OpcConnectionException.
 *
 * @author mengyuan
 * @version Id: OpcConnectionException.java, v 0.1 2022年06月10日 15:42 mengyuan Exp $
 */
public class OpcConnectionException extends OpcRpcRuntimeException {

    public OpcConnectionException() {
        super(ExceptionCode.CONNECTION_ERROR);
    }

    public OpcConnectionException(ExceptionCode exceptionCode) {
        super(exceptionCode);
    }

    public OpcConnectionException(ExceptionCode exceptionCode, Throwable throwable) {
        super(exceptionCode, throwable);
    }

    public OpcConnectionException(int errCode, String errMsg) {
        super(errCode, errMsg);
    }

    public OpcConnectionException(int errCode, Throwable throwable) {
        super(errCode, throwable);
    }

    public OpcConnectionException(int errCode, String errMsg, Throwable throwable) {
        super(errCode, errMsg, throwable);
    }

}
