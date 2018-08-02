package de.upb.bibifi.verybest.common.models;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A {@link JsonAdapter.Factory} that can create a polymorphic {@link JsonAdapter} which will parse
 * a given type based on the value under the name set via {@link Builder#setKey(String)}.
 *
 * <p>Usage:
 * <pre>{@code
 *    JsonAdapter.Factory polymorphicAdapterFactory =
 *      new GenericPolymorphicJsonAdapterFactory.Builder()
 *        .setKey("the-key-which-will-provide-the-value")
 *        .map("some_value", YourObject.class)
 *        .map("some_other_value", YourOtherObject.class)
 *        .build();
 *
 *     Moshi moshi = new Moshi.Builder()
 *      .add(polymorphicAdapterFactory)
 *      .build();
 * }</pre>
 *
 * <p>The adapter can be restricted to a specific <strong>parent</strong> type. This will allow to
 * minimize the amount of adapters created for each object. See {@link Builder} for more info.
 */
@SuppressWarnings("NullAway")
public final class GenericPolymorphicJsonAdapterFactory implements JsonAdapter.Factory {
    private final Map<String, Class<?>> kindToClass;
    private final Class<?> parent;
    private final String key;
    private final boolean lenient;

    GenericPolymorphicJsonAdapterFactory(Builder builder) {
        this.kindToClass = builder.valueToClass;
        this.parent = builder.parent;
        this.key = builder.key;
        this.lenient = builder.lenient;
    }

    @Override public JsonAdapter<?> create(
            Type type, Set<? extends Annotation> annotations, Moshi moshi) {
        if (!annotations.isEmpty()) return null;

        if (typeSupported(type)) {
            Map<String, JsonAdapter<?>> nameToAdapter = new LinkedHashMap<>();
            Map<Class<?>, JsonAdapter<?>> classToAdapter = new LinkedHashMap<>();
            for (Map.Entry<String, Class<?>> classEntry : kindToClass.entrySet()) {
                JsonAdapter<Object> adapter =
                        moshi.nextAdapter(this, classEntry.getValue(), annotations);
                nameToAdapter.put(classEntry.getKey(), adapter);
                classToAdapter.put(classEntry.getValue(), adapter);
            }

            GenericPolymorphicJsonAdapter adapter =
                    new GenericPolymorphicJsonAdapter(key, nameToAdapter, classToAdapter);

            return lenient ? adapter.lenient() : adapter;
        }

        return null;
    }

    private boolean typeSupported(Type type) {
        if (parent != null) {
            Class<?> rawType = Types.getRawType(type);
            return parent.isAssignableFrom(rawType);
        }

        for (Class<?> cls : kindToClass.values()) if (type == cls) return true;
        return false;
    }

    private static final class GenericPolymorphicJsonAdapter extends JsonAdapter<Object> {
        private final String kindKey;
        private final Map<String, JsonAdapter<?>> nameToAdapter;
        private final Map<Class<?>, JsonAdapter<?>> classToAdapter;

        GenericPolymorphicJsonAdapter(String kindKey, Map<String, JsonAdapter<?>> nameToAdapter,
                                      Map<Class<?>, JsonAdapter<?>> classToAdapter) {
            this.kindKey = kindKey;
            this.nameToAdapter = nameToAdapter;
            this.classToAdapter = classToAdapter;
        }

        @Override public Object fromJson(JsonReader reader) throws IOException {
            Object jsonObject = reader.readJsonValue();
            if (jsonObject instanceof Map) {
                // readJsonValue() can only return one type of map.
                //noinspection unchecked
                Map<String, Object> json = (Map<String, Object>) jsonObject;

                Object kind = json.get(kindKey);
                if (kind instanceof String) {
                    JsonAdapter<?> jsonAdapter = nameToAdapter.get(kind);
                    if (jsonAdapter != null) {
                        return jsonAdapter.fromJsonValue(jsonObject);
                    }

                    return nullIfLenient(reader,
                            "No adapter registered for " + kind + " at path " + reader.getPath());
                }

                return nullIfLenient(reader, "Expected KIND to be a string, but found "
                        + (kind != null ? kind.getClass().getSimpleName() : null)
                        + " at path " + reader.getPath());
            }

            return nullIfLenient(reader, "Expected Map, but found "
                    + (jsonObject != null ? jsonObject.getClass().getSimpleName() : null)
                    + " at path " + reader.getPath());
        }

        private Object nullIfLenient(JsonReader reader, String message) {
            if (reader.isLenient()) return null;
            throw new JsonDataException(message);
        }

        @Override public void toJson(JsonWriter writer, Object object) throws IOException {
            if (object != null) {
                //noinspection unchecked
                JsonAdapter<Object> adapter = (JsonAdapter<Object>) classToAdapter.get(object.getClass());
                if (adapter != null) {
                    adapter.toJson(writer, object);
                } else {
                    if (writer.isLenient()) {
                        writer.nullValue();
                    } else {
                        throw new JsonDataException(
                                "No adapter registered for " + object.getClass().getSimpleName());
                    }
                }
            } else {
                writer.nullValue();
            }
        }
    }

    /** Constructs a {@link GenericPolymorphicJsonAdapterFactory}. */
    public static final class Builder {
        Map<String, Class<?>> valueToClass = new LinkedHashMap<>();
        final Class<?> parent;
        String key;
        boolean lenient = false;

        /**
         * Creates a new builder, which is not opinionated regarding the parent of the supported types.
         */
        public Builder() {
            this(null);
        }

        /**
         * Creates a new builder, that will force each type to be a descendant of {@code parent}.
         */
        public Builder(Class<?> parent) {
            this.parent = parent;
        }

        /**
         * Set the key/name of the {@link String} value that will be used to distinguish types from one
         * another.
         */
        public Builder setKey(String key) {
            if (key == null) throw new NullPointerException("key == null");
            this.key = key;
            return this;
        }

        public Builder setLenient(boolean lenient) {
            this.lenient = lenient;
            return this;
        }

        /** Map a value to a specific type. */
        public Builder map(String value, Class<?> toType) {
            if (value == null) throw new NullPointerException("value == null");
            if (toType == null) throw new NullPointerException("toType == null");

            if (parent != null) {
                if (!parent.isAssignableFrom(toType)) {
                    throw new IllegalArgumentException(toType + " must inherit from " + parent);
                }
            }

            valueToClass.put(value, toType);
            return this;
        }

        public GenericPolymorphicJsonAdapterFactory build() {
            if (key == null) throw new IllegalStateException("key not set!");
            if (valueToClass.isEmpty()) {
                throw new IllegalStateException("No kind -> type mapping registered.");
            }
            return new GenericPolymorphicJsonAdapterFactory(this);
        }
    }
}