package io.opc.rpc.core.connection;

import io.opc.rpc.api.Connection;
import io.opc.rpc.api.Endpoint;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * BaseConnection.
 *
 * @author caihongwen
 * @version Id: BaseConnection.java, v 0.1 2022年06月03日 11:32 caihongwen Exp $
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

    private long lastActiveTime = System.currentTimeMillis();

    /**
     * refresh {@link #lastActiveTime} to {@link System#currentTimeMillis()}
     */
    public void refreshActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "Connection{" +
                "connectionId='" + connectionId + '\'' +
                ", clientName='" + clientName + '\'' +
                ", endpoint=" + endpoint +
                ", labels=" + labels +
                '}';
    }

}
