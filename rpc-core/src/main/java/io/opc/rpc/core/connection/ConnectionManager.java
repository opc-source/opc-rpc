package io.opc.rpc.core.connection;

import io.opc.rpc.api.Connection;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ConnectionManager.
 *
 * @author caihongwen
 * @version Id: ConnectionManager.java, v 0.1 2022年06月03日 12:01 caihongwen Exp $
 */
public class ConnectionManager {

    private final Map<String/*connectionId*/, Connection> connectionMap = new ConcurrentHashMap<>(256);

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

    /**
     * get active timeout Connections
     */
    public Collection<Connection> getActiveTimeoutConnections(final long timeout) {
        final Collection<Connection> connections = getConnections();
        final long now = System.currentTimeMillis();
        return connections.stream().filter(conn ->
                        (conn instanceof BaseConnection) && (now - timeout > ((BaseConnection) conn).getLastActiveTime()))
                .collect(Collectors.toList());
    }

    /**
     * refresh Connection's activeTime
     *
     * @param connectionId connectionId
     */
    public void refreshActiveTime(String connectionId) {
        final Connection connection = getConnection(connectionId);
        refreshActiveTime(connection);
    }

    /**
     * refresh Connection's activeTime
     *
     * @param connection Connection
     */
    public static void refreshActiveTime(final Connection connection) {
        if (connection != null && connection instanceof BaseConnection) {
            ((BaseConnection) connection).refreshActiveTime();
        }
    }

    /**
     * is active timeout.
     *
     * @param connection Connection
     * @param timeout timeout
     */
    public static boolean isActiveTimeout(final Connection connection, final long timeout) {
        final long now = System.currentTimeMillis();
        return (connection instanceof BaseConnection) && (now - timeout > ((BaseConnection) connection).getLastActiveTime());
    }

}
