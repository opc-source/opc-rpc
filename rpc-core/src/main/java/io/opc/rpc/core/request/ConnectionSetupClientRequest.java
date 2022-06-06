package io.opc.rpc.core.request;

import io.opc.rpc.api.request.ClientRequest;
import io.opc.rpc.core.annotation.Internal;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * ConnectionSetupClientRequest.
 *
 * @author caihongwen
 * @version Id: ConnectionSetupClientRequest.java, v 0.1 2022年06月03日 11:17 caihongwen Exp $
 */
@Internal
@Getter
@Setter
@Builder
public class ConnectionSetupClientRequest extends ClientRequest {

    private String clientName;

    private Map<String, String> labels;

    @Override
    public String toString() {
        return "ConnectionSetupClientRequest{" +
                "requestId='" + requestId + '\'' +
                ", clientName='" + clientName + '\'' +
                ", labels=" + labels +
                "}";
    }

}
