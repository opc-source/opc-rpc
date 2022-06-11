package io.opc.rpc.api;

import io.opc.rpc.api.constant.OpcConstants;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Endpoint.
 *
 * @author caihongwen
 * @version Id: Endpoint.java, v 0.1 2022年06月02日 22:51 caihongwen Exp $
 */
@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class Endpoint {

    private final String ip;

    private final int port;

    /**
     * get Address, like ip:port
     *
     * @return address like ip:port
     */
    public String getAddress() {
        return this.ip + OpcConstants.COLON + this.port;
    }

    /**
     * resolve ServerAddress to Endpoint set.
     *
     * @param serverAddress eg: localhost:12345,domain,127.0.0.1:12343
     * @return Set<Endpoint>
     */
    public static Set<Endpoint> resolveServerAddress(@Nonnull String serverAddress) {

        final String[] addressArr = serverAddress.split(OpcConstants.COMMA);

        return Arrays.stream(addressArr).map(address -> {
            address = address.trim();
            if (address.contains(OpcConstants.COLON)) {
                final String[] hostAndPort = address.split(OpcConstants.COLON);
                return new Endpoint(hostAndPort[0], Integer.parseInt(hostAndPort[1]));
            } else {
                return new Endpoint(address, OpcConstants.Server.DEFAULT_OPC_RPC_SERVER_PORT);
            }
        }).collect(Collectors.toSet());
    }

    /**
     * random select one.
     *
     * @param endpoints Endpoints
     * @return Endpoint
     */
    public static Endpoint randomOne(@Nonnull Collection<Endpoint> endpoints) {

        final Endpoint[] endpointArr = endpoints.toArray(new Endpoint[0]);
        if (1 == endpointArr.length) {
            return endpointArr[0];
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(0, endpoints.size());
        return endpointArr[randomIndex];
    }

    /**
     * random select one (which not in exclude).
     *
     * @param endpoints Endpoints
     * @return Endpoint
     */
    public static Endpoint randomOneExclude(@Nonnull Collection<Endpoint> endpoints, @Nonnull Endpoint exclude) {

        final Endpoint[] endpointArr = endpoints.toArray(new Endpoint[0]);
        if (1 == endpointArr.length) {
            return endpointArr[0];
        }

        Set<Endpoint> endpointExclude = new HashSet<>(endpoints);
        endpointExclude.remove(exclude);
        return randomOne(endpointExclude);
    }

}