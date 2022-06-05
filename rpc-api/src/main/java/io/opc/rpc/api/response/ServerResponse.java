package io.opc.rpc.api.response;

import lombok.Getter;
import lombok.Setter;

/**
 * ServerResponse. Response by Server.
 *
 * @author mengyuan
 * @version Id: ServerResponse.java, v 0.1 2022年06月03日 11:54 mengyuan Exp $
 */
@Getter
@Setter
public abstract class ServerResponse implements Response {

    protected String requestId;

}
