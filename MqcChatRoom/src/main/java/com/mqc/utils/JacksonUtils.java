package com.mqc.utils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.util.Map;

public class JacksonUtils {
    private static final ObjectMapper objectMapper =  JsonMapper.builder()
            .findAndAddModules()
            .build();


    public static <T> T  mapToObject(Map<String, Object> map, Class<T> classType) {
        return objectMapper.convertValue(map, classType);
    }

    public static JsonNode toJsonNode(String jsonData) throws JsonProcessingException, IOException {
        return objectMapper.readTree(jsonData);
    }

    public static <T> String toJsonString(T cls) throws JsonProcessingException {

        return objectMapper.writeValueAsString(cls);
    }

    public static <T> T jsonStrToObject(String json, TypeReference<T> typeReference) throws JsonParseException, JsonMappingException, IOException {
        return objectMapper.readValue(json, typeReference);
    }

    public static String objectToJsonStr(Object o) throws JsonProcessingException {
        return objectMapper.writeValueAsString(o);
    }

    public static Map<String, Object> jsonStrToMap(String json) throws JsonParseException, JsonMappingException, IOException{
        return jsonStrToObject(json, new TypeReference<Map<String, Object>>(){});
    }
}
