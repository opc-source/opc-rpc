package io.opc.rpc.api.response;

/**
 * ErrorResponse. ErrorResponse for Client and Server.
 *
 * @author caihongwen
 * @version Id: ErrorResponse.java, v 0.1 2022年06月03日 11:52 caihongwen Exp $
 */
public class ErrorResponse extends Response {

    /**
     * build an error response.
     *
     * @param responseCode ResponseCode
     * @return response
     */
    public static ErrorResponse build(ResponseCode responseCode) {
        return build(responseCode.getCode(), responseCode.getMessage());
    }

    /**
     * build an error response.
     *
     * @param resultCode resultCode
     * @param message message
     * @return response
     */
    public static ErrorResponse build(int resultCode, String message) {
        ErrorResponse response = new ErrorResponse();
        response.setResultCode(resultCode);
        response.setMessage(message);
        return response;
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "requestId='" + getRequestId() + '\'' +
                ", resultCode=" + this.getResultCode() +
                ", message='" + this.getMessage() + '\'' +
                '}';
    }

}
