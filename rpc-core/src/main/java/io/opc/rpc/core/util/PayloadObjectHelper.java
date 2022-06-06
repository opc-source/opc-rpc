package io.opc.rpc.core.util;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import java.util.Map;
import lombok.experimental.UtilityClass;

/**
 * PayloadObjectHelper.
 *
 * @author caihongwen
 * @version Id : PayloadObjectHelper.java, v 0.1 2022年06月05日 11:29 caihongwen Exp $
 */
@UtilityClass
public class PayloadObjectHelper {

    public <T extends io.opc.rpc.api.Payload> T buildApiPayload(io.opc.rpc.core.grpc.auto.Payload grpcPayload) {

        return JsonSerialization.deserialize(
                grpcPayload.getBody().getValue().toStringUtf8(),
                PayloadClassHelper.getClass(grpcPayload.getBody().getTypeUrl())
        );
    }

    public <T extends io.opc.rpc.api.Payload> io.opc.rpc.core.grpc.auto.Payload buildGrpcPayload(
            T apiPayload, Map<String, String> headers) {

        return io.opc.rpc.core.grpc.auto.Payload.newBuilder()
                .setMetadata(io.opc.rpc.core.grpc.auto.Metadata.newBuilder().putAllHeaders(headers).build())
                .setBody(Any.newBuilder()
                        .setTypeUrl(apiPayload.getClass().getName())
                        .setValue(ByteString.copyFromUtf8(JsonSerialization.serialize(apiPayload)))
                        .build())
                .build();
    }

}
