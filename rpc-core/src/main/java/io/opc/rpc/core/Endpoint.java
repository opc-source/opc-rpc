package io.opc.rpc.core;

import static io.opc.rpc.api.constant.OpcConstants.COLON;

import lombok.Getter;

/**
 * Endpoint.
 *
 * @author caihongwen
 * @version Id: Endpoint.java, v 0.1 2022年06月02日 22:51 caihongwen Exp $
 */
public class Endpoint {

    @Getter
    private final String ip;

    @Getter
    private final int port;

    public Endpoint(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    /**
     * get Address, like ip:port
     *
     * @return address like ip:port
     */
    public String getAddress() {
        return this.ip + COLON + this.port;
    }

}