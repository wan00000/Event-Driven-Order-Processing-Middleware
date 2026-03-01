// common/src/main/java/com/acme/common/util/JsonUtil.java
package com.acme.common.util;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

public final class JsonUtil {
    private static final Jsonb JSONB = JsonbBuilder.create();

    private JsonUtil() {}

    public static String toJson(Object obj) {
        return JSONB.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> cls) {
        return JSONB.fromJson(json, cls);
    }
}