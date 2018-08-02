package de.upb.bionicbeaver.atm.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.function.Supplier;

/**
 * Provides fully configured {@link ObjectMapper} for JSON to string mapping and vice-versa.
 *
 * @author Siddhartha Moitra
 */
public class JsonMapperProvider implements Supplier<Object> {

    private static final JsonMapperProvider INSTANCE = new JsonMapperProvider();

    public static JsonMapperProvider getInstance() {
        return INSTANCE;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JsonMapperProvider() {
        this.objectMapper
                .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
                .configure(SerializationFeature.WRITE_ENUMS_USING_TO_STRING, true)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    }

    @Override
    public ObjectMapper get() {
        return this.objectMapper;
    }
}
