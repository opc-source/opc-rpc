package io.opc.rpc.core;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * BaseConnection.
 *
 * @author mengyuan
 * @version Id: BaseConnection.java, v 0.1 2022年06月03日 11:32 mengyuan Exp $
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public abstract class BaseConnection implements Connection {

    private String connectionId;

    private String clientName;

    private Endpoint endpoint;

    private Map<String, String> labels;

}
