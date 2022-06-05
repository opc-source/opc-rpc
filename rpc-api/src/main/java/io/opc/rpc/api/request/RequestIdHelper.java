package io.opc.rpc.api.request;

import java.util.concurrent.atomic.AtomicLong;

/**
 * RequestIdHelper.
 *
 * @author caihongwen
 * @version Id: RequestIdHelper.java, v 0.1 2022年06月02日 21:45 caihongwen Exp $
 */
class RequestIdHelper {

    private final long initialValue;

    private final long delta;

    private final AtomicLong sequence;

    RequestIdHelper(long initialValue, long delta) {
        this.initialValue = initialValue;
        this.delta = delta;
        this.sequence = new AtomicLong(initialValue);
    }

    /**
     * Generate requestId.
     *
     * @return requestId
     */
    String generateRequestId() {
        final long seq = this.sequence.getAndAdd(delta);

        if (seq > Long.MAX_VALUE - 1000) {
            this.sequence.set(this.initialValue);
        }

        return String.valueOf(seq);
    }

}
