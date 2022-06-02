package io.opc.rpc.core;

import lombok.Getter;

/**
 * Endpoint.
 *
 * @author caihongwen
 * @version Id: Endpoint.java, v 0.1 2022年06月02日 22:51 caihongwen Exp $
 */
public class Endpoint {

    public static final String COLON = ":";

    @Getter
    private String ip;

    @Getter
    private int port;

    public Endpoint(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public String getAddress() {
        return this.ip + COLON + this.port;
    }

}