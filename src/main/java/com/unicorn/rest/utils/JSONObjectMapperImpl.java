package com.unicorn.rest.utils;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

@Provider
public class JSONObjectMapperImpl implements ContextResolver<ObjectMapper> {

    private final ObjectMapper defaultObjectMapper;

    public JSONObjectMapperImpl() {
        defaultObjectMapper = new ObjectMapper()
        .configure(SerializationFeature.INDENT_OUTPUT, true)
        .configure(SerializationFeature.WRAP_ROOT_VALUE, true)
        .configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false)
        .configure(SerializationFeature.FLUSH_AFTER_WRITE_VALUE, false)
        .configure(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS, true)
        .configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true)
        .configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true)
        .setAnnotationIntrospector(createJaxbJacksonAnnotationIntrospector());
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return defaultObjectMapper;
    }

    private static AnnotationIntrospector createJaxbJacksonAnnotationIntrospector() {
        AnnotationIntrospector jaxbIntrospector = new JaxbAnnotationIntrospector(TypeFactory.defaultInstance());
        AnnotationIntrospector jacksonIntrospector = new JacksonAnnotationIntrospector();
        return AnnotationIntrospector.pair(jacksonIntrospector, jaxbIntrospector);
    }


}
