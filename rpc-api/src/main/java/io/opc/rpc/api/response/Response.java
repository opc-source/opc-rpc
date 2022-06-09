package io.opc.rpc.api.response;

import io.opc.rpc.api.Payload;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Response.
 *
 * @author caihongwen
 * @version Id: Response.java, v 0.1 2022年06月02日 21:42 caihongwen Exp $
 */
@Getter
@Setter
@ToString
public abstract class Response implements Payload {

    private int resultCode = ResponseCode.OK.getCode();

    private String message = ResponseCode.OK.getMessage();

    protected String requestId;

    /**
     * Whether response is OK
     *
     * @return true/false
     */
    public boolean isSuccess() {
        return this.resultCode == ResponseCode.OK.getCode();
    }

}
