package io.opc.rpc.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Opc Rpc Status.
 *
 * @author caihongwen
 * @version Id: OpcRpcStatus.java, v 0.1 2022年06月11日 19:15 caihongwen Exp $
 */
@Getter
@AllArgsConstructor
public enum OpcRpcStatus {

    /**
     * wait to init.
     */
    WAIT_INIT(0, "Wait to init..."),

    /**
     * in starting.
     */
    STARTING(2, "Already staring, wait to connect..."),

    /**
     * unhealthy.
     */
    UNHEALTHY(4, "Unhealthy, maybe connection closed, in reconnecting..."),

    /**
     * in running.
     */
    RUNNING(6, "Running."),

    /**
     * shutdown.
     */
    SHUTDOWN(8, "Shutdown.");

    private final int status;

    private final String desc;

}
