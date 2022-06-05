package io.opc.rpc.core.response;

import io.opc.rpc.api.response.Response;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * ErrorResponse. ErrorResponse for Client and Server.
 *
 * @author mengyuan
 * @version Id: ErrorResponse.java, v 0.1 2022年06月03日 11:52 mengyuan Exp $
 */
@Getter
@Setter
@ToString(callSuper = true)
public class ErrorResponse implements Response {

    private String requestId;

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

}
