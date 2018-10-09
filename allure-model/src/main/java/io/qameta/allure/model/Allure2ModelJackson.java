package io.qameta.allure.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

/**
 * @author charlie (Dmitry Baev).
 */
public final class Allure2ModelJackson {

    public static final String INDENT_OUTPUT_PROPERTY_NAME = "allure.results.indentOutput";

    private Allure2ModelJackson() {
        throw new IllegalStateException("Do not instance Allure2ModelJackson");
    }

    public static ObjectMapper createMapper() {
        return new ObjectMapper()
                .configure(USE_WRAPPER_NAME_AS_PROPERTY_NAME, true)
                .setAnnotationIntrospector(new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()))
                .setSerializationInclusion(NON_NULL)
                .configure(INDENT_OUTPUT, Boolean.getBoolean(INDENT_OUTPUT_PROPERTY_NAME))
                .registerModule(new SimpleModule()
                        .addDeserializer(Status.class, new StatusDeserializer())
                        .addDeserializer(Stage.class, new StageDeserializer())
                );
    }
}
