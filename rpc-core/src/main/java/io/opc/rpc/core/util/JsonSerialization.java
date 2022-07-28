package io.opc.rpc.core.util;

import com.google.gson.Gson;
import lombok.experimental.UtilityClass;

/**
 * JsonSerialization for default.
 *
 * @author caihongwen
 * @version Id: JsonSerialization.java, v 0.1 2022年06月03日 11:44 caihongwen Exp $
 */
@UtilityClass
class JsonSerialization {

    private static final Gson GSON = new Gson();

    String serialize(Object obj) {
        return GSON.toJson(obj);
    }

    <T> T deserialize(String jsonStr, Class<T> clazz) {
        return GSON.fromJson(jsonStr, clazz);
    }

}
