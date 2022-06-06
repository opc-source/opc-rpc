package io.opc.rpc.core;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.experimental.UtilityClass;

/**
 * ConnectionManager.
 *
 * @author caihongwen
 * @version Id: ConnectionManager.java, v 0.1 2022年06月03日 12:01 caihongwen Exp $
 */
@UtilityClass
public class ConnectionManager {

    private static final Map<String/*connectionId*/, Connection> connectionMap = new ConcurrentHashMap<>(16);

    /**
     * get Connection.
     *
     * @param connectionId connectionId
     * @return Connection or null
     */
    public Connection getConnection(String connectionId) {
        return connectionMap.get(connectionId);
    }

    /**
     * get Connections.
     *
     * @return Connections
     */
    public Collection<Connection> getConnections() {
        return connectionMap.values();
    }

    /**
     * Hold Connection and close old.
     *
     * @param connection Connection
     * @return ture->has old, false->not old
     */
    public boolean holdAndCloseOld(Connection connection) {
        final Connection oldConnect = holdConnection(connection);
        if (oldConnect == null) {
            return false;
        }
        oldConnect.close();
        return true;
    }

    /**
     * Hold Connection.
     *
     * @param connection Connection
     * @return null or oldConnection
     */
    public Connection holdConnection(Connection connection) {
        return connectionMap.put(connection.getConnectionId(), connection);
    }

    /**
     * remove and close Connection.
     *
     * @param connectionId String
     * @return exit old.
     */
    public boolean removeAndClose(String connectionId) {
        final Connection connection = connectionMap.remove(connectionId);
        if (connection == null) {
            return false;
        }
        connection.close();
        return true;
    }

    /**
     * remove and close all Connection.
     */
    public void removeAndCloseAll() {
        connectionMap.values().forEach(Connection::close);
        connectionMap.clear();
    }

}