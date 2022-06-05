package io.opc.rpc.core;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.experimental.UtilityClass;

/**
 * ConnectionManager.
 *
 * @author mengyuan
 * @version Id: ConnectionManager.java, v 0.1 2022年06月03日 12:01 mengyuan Exp $
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
     * get Connection EntrySet.
     *
     * @return Connection EntrySet
     */
    public Set<Entry<String, Connection>> connectionEntrySet() {
        return connectionMap.entrySet();
    }

    /**
     * Hold Connection.
     *
     * @param connection Connection
     * @return null or oldConnection
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
     * @return exit ola.
     */
    public boolean removeAndCloseConnection(String connectionId) {
        final Connection connection = connectionMap.remove(connectionId);
        if (connection == null) {
            return false;
        }
        connection.close();
        return true;
    }

}
