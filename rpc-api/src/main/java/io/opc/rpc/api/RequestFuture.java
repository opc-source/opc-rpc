package io.opc.rpc.api;

import io.opc.rpc.api.response.Response;
import java.util.concurrent.Future;

/**
 * Future for request, who async waiting a Response.
 *
 * @author caihongwen
 * @version Id: RequestFuture.java, v 0.1 2022年06月16日 14:05 caihongwen Exp $
 */
public interface RequestFuture<R extends Response> extends Future<R> {

}
