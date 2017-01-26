package com.sand.zkb.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/**
 * Created by SDW on 2015/4/26.
 */
public class JsonConverter extends MappingJackson2HttpMessageConverter {
//    private static ObjectMapper mapper;
    public JsonConverter() {
        super(Jackson2ObjectMapperBuilder
                .json()
                .timeZone("GMT+8")
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .simpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .build());
//        setMapper(super.getObjectMapper());
    }

//    public static ObjectMapper getMapper() {
//        return mapper;
//    }

//    public static void setMapper(ObjectMapper mapper) {
//        BeepJackson2HttpConverter.mapper = mapper;
//    }
}
