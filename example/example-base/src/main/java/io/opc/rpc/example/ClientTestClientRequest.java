package io.opc.rpc.example;

import io.opc.rpc.api.request.ClientRequest;
import lombok.Getter;
import lombok.Setter;

/**
 * @author mengyuan
 * @version Id: ClientTestClientRequest.java, v 0.1 2022年06月10日 11:41 mengyuan Exp $
 */
@Getter
@Setter
public class ClientTestClientRequest extends ClientRequest {

    private String test;

    @Override
    public String toString() {
        return "ClientTestClientRequest{" +
                "requestId='" + requestId + '\'' +
                ", headers=" + headers +
                ", test='" + test + '\'' +
                '}';
    }
}
