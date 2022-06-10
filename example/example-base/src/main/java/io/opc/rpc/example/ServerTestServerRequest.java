package io.opc.rpc.example;

import io.opc.rpc.api.request.ServerRequest;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author mengyuan
 * @version Id: ServerTestServerRequest.java, v 0.1 2022年06月10日 11:41 mengyuan Exp $
 */
@Getter
@Setter
@ToString(callSuper = true)
public class ServerTestServerRequest extends ServerRequest {

    private String server;

}
