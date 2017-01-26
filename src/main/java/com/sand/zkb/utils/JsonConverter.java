package com.sand.zkb.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

public class JsonConverter extends MappingJackson2HttpMessageConverter {
    public JsonConverter() {
        super(Jackson2ObjectMapperBuilder
                .json()
                .timeZone("GMT+8")
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .simpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .build());
    }

}
