package io.opc.rpc.example;

import io.opc.rpc.api.request.ClientRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author mengyuan
 * @version Id: ClientTestClientRequest.java, v 0.1 2022年06月10日 11:41 mengyuan Exp $
 */
@Getter
@Setter
@ToString(callSuper = true)
public class ClientTestClientRequest extends ClientRequest {

    private String test;

}
