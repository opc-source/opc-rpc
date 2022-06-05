package io.opc.rpc.api.response;

import lombok.Getter;
import lombok.Setter;

/**
 * ClientResponse. Response by Client.
 *
 * @author mengyuan
 * @version Id: ClientResponse.java, v 0.1 2022年06月03日 11:54 mengyuan Exp $
 */
@Getter
@Setter
public abstract class ClientResponse implements Response {

    protected String requestId;

}
