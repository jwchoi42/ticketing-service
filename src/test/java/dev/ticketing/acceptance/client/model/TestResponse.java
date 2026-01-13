package dev.ticketing.acceptance.client.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class TestResponse {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final int statusCode;
    private final byte[] bodyBytes;

    public int statusCode() {
        return statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBodyAsString() {
        return bodyBytes == null ? "" : new String(bodyBytes, StandardCharsets.UTF_8);
    }

    public <T> T as(Class<T> classType) {
        try {
            return objectMapper.readValue(bodyBytes, classType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize response", e);
        }
    }

    public JsonPathWrapper jsonPath() {
        return new JsonPathWrapper(getBodyAsString());
    }

    @RequiredArgsConstructor
    public static class JsonPathWrapper {
        private final String json;
        private final Map<String, Object> params = new HashMap<>();

        public JsonPathWrapper param(String key, Object value) {
            params.put(key, value);
            return this;
        }

        public String getString(String path) {
            return read(path, String.class);
        }

        public Integer getInt(String path) {
            return read(path, Integer.class);
        }

        public Long getLong(String path) {
            return read(path, Long.class);
        }

        public <T> List<T> getList(String path) {
            return read(path, List.class);
        }

        public <K, V> Map<K, V> getMap(String path) {
            return read(path, Map.class);
        }

        public Object get(String path) {
            return read(path, Object.class);
        }

        private <T> T read(String path, Class<T> type) {
            String processedPath = path;
            // Handle RestAssured GPath style find { it.type == type }
            if (processedPath.contains("find { it.")) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue().toString();
                    processedPath = processedPath.replaceFirst(
                        "(\\w+)\\.find \\{ it\\.(\\w+) == " + key + " \\}\\.(\\w+)",
                        "$1[?(@.$2 == '" + value + "')].$3"
                    );
                }
            }

            // Ensure Jayway JsonPath prefix
            if (!processedPath.startsWith("$")) {
                processedPath = "$." + processedPath;
            }

            try {
                Object result = JsonPath.parse(json).read(processedPath);
                if (result instanceof List && ((List<?>) result).size() == 1 && !type.equals(List.class)) {
                    result = ((List<?>) result).get(0);
                }
                
                if (result == null) return null;
                
                if (type.isInstance(result)) {
                    return type.cast(result);
                }
                
                // Handle numeric conversions
                if (type.equals(Long.class) && result instanceof Integer) {
                    return type.cast(((Integer) result).longValue());
                }
                if (type.equals(String.class)) {
                    return type.cast(result.toString());
                }
                
                return type.cast(result);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
