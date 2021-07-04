package io.qameta.allure.reader;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.qameta.allure.model.Parameter;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;

/**
 * @author charlie (Dmitry Baev).
 */
public final class AllureObjectMapperFactory {

    private AllureObjectMapperFactory() {
        throw new IllegalStateException("do not instance");
    }

    public static ObjectMapper createMapper() {
        return new ObjectMapper()
                .enable(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .registerModule(new SimpleModule()
                        .addDeserializer(Status.class, new StatusDeserializer())
                        .addDeserializer(Stage.class, new StageDeserializer())
                        .addDeserializer(Parameter.Mode.class, new ParameterModeDeserializer())
                );
    }
}
