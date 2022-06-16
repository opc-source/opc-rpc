package io.opc.rpc.core.util;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.opc.rpc.api.request.Request;
import java.util.Collections;
import lombok.experimental.UtilityClass;

/**
 * PayloadObjectHelper.
 *
 * @author caihongwen
 * @version Id : PayloadObjectHelper.java, v 0.1 2022年06月05日 11:29 caihongwen Exp $
 */
@UtilityClass
public class PayloadObjectHelper {

    private static final io.opc.rpc.core.grpc.auto.Metadata EMPTY = io.opc.rpc.core.grpc.auto.Metadata.newBuilder()
            .putAllHeaders(Collections.emptyMap())
            .build();

    public <T extends io.opc.rpc.api.Payload> T buildApiPayload(io.opc.rpc.core.grpc.auto.Payload grpcPayload) {

        final T apiPayload = JsonSerialization.deserialize(
                grpcPayload.getBody().getValue().toStringUtf8(),
                PayloadClassHelper.getClass(grpcPayload.getBody().getTypeUrl())
        );
        if (apiPayload instanceof Request) {
            // Request.headers get from Metadata.headers
            ((Request) apiPayload).setHeaders(grpcPayload.getMetadata().getHeadersMap());
        }
        return apiPayload;
    }

    public <T extends io.opc.rpc.api.Payload> io.opc.rpc.core.grpc.auto.Payload buildGrpcPayload(T apiPayload) {

        io.opc.rpc.core.grpc.auto.Metadata metadata = EMPTY;
        if ((apiPayload instanceof Request)
                && !((Request) apiPayload).getHeaders().isEmpty()) {
            // Request.headers set into Metadata.headers
            metadata = io.opc.rpc.core.grpc.auto.Metadata.newBuilder()
                    .putAllHeaders(((Request) apiPayload).getHeaders()).build();
            // clear Request.headers
            ((Request) apiPayload).getHeaders().clear();
        }
        return io.opc.rpc.core.grpc.auto.Payload.newBuilder()
                .setMetadata(metadata)
                .setBody(Any.newBuilder()
                        .setTypeUrl(apiPayload.getClass().getName())
                        .setValue(ByteString.copyFromUtf8(JsonSerialization.serialize(apiPayload)))
                        .build())
                .build();
    }

}
