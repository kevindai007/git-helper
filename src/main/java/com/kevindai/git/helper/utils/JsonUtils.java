package com.kevindai.git.helper.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.CollectionType;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
public class JsonUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        mapper.configure(JsonReadFeature.ALLOW_MISSING_VALUES.mappedFeature(), true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @SneakyThrows
    public static <T> String toJSONString(T entity) {
        String json = mapper.writeValueAsString(entity);
        return json;
    }

    @SneakyThrows
    public static <T> T parseObject(String json, Class<T> type) {
        return mapper.readValue(json, type);
    }

    @SneakyThrows
    public static <T> List<T> parseArray(String json, Class<T> T) {
        CollectionType type = mapper.getTypeFactory().constructCollectionType(List.class, T);
        return mapper.readValue(json, type);
    }

    public static ObjectNode createObjectNode() {
        return mapper.createObjectNode();
    }

    @SneakyThrows
    public static JsonNode parseJsonNode(String jsonText) {
        return mapper.readTree(jsonText);
    }
}
