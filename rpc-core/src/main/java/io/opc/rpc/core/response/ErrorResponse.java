package io.opc.rpc.core.response;

import io.opc.rpc.api.response.Response;
import lombok.Getter;
import lombok.Setter;

/**
 * ErrorResponse. ErrorResponse for Client and Server.
 *
 * @author caihongwen
 * @version Id: ErrorResponse.java, v 0.1 2022年06月03日 11:52 caihongwen Exp $
 */
@Getter
@Setter
public class ErrorResponse extends Response {

    private int errorCode;

    private String message;

    /**
     * build an error response.
     *
     * @param errorCode errorCode
     * @param msg msg
     * @return response
     */
    public static ErrorResponse build(int errorCode, String msg) {
        ErrorResponse response = new ErrorResponse();
        response.setErrorCode(errorCode);
        response.setMessage(msg);
        return response;
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "requestId='" + requestId + '\'' +
                ", errorCode=" + errorCode +
                ", message='" + message + '\'' +
                '}';
    }

}
