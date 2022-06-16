package io.opc.rpc.api.request;

import io.opc.rpc.api.Payload;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Request.
 *
 * @author caihongwen
 * @version Id: Request.java, v 0.1 2022年06月02日 21:42 caihongwen Exp $
 */
public abstract class Request implements Payload {

    {
        requestId = generateRequestId();
    }

    /**
     * generate a new requestId.
     *
     * @return a new requestId
     */
    protected abstract String generateRequestId();

    /**
     * RequestId generate on construct.
     */
    protected final String requestId;

    /**
     * metadata headers.
     */
    protected Map<String, String> headers = Collections.emptyMap();

    /**
     * Getter method for property <tt>requestId</tt>.
     *
     * @return property value of requestId
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Getter method for property <tt>headers</tt>.
     *
     * @return property value of headers
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Setter method for property <tt>headers</tt>.
     *
     * @param headers value to be assigned to property headers
     */
    public void setHeaders(Map<String, String> headers) {
        Objects.requireNonNull(headers, "headers is null");
        this.headers = headers;
    }

    /**
     * put a key/val into headers
     */
    public synchronized void putHeaderIfValNonnull(@Nonnull String key, @Nullable String val) {
        if (val != null) {
            this.putHeader(key, val);
        }
    }

    /**
     * put a key/val into headers
     */
    public synchronized void putHeader(@Nonnull String key, @Nonnull String val) {
        Objects.requireNonNull(val, "val is null");
        if (Collections.emptyMap().equals(this.headers)) {
            this.headers = new HashMap<>(8);
        }
        this.headers.put(key, val);
    }

    /**
     * put key/val into headers
     */
    public synchronized void putHeaders(Map<String, String> headers) {
        Objects.requireNonNull(headers, "headers is null");
        if (Collections.emptyMap().equals(this.headers)) {
            this.headers = new HashMap<>(8);
        }
        this.headers.putAll(headers);
    }

}
