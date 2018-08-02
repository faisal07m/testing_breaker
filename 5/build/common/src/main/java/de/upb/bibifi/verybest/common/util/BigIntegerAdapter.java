package de.upb.bibifi.verybest.common.util;

import com.squareup.moshi.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.math.BigInteger;

public class BigIntegerAdapter extends JsonAdapter<BigInteger> {
    @Nullable
    @Override
    @FromJson
    public BigInteger fromJson(JsonReader reader) throws IOException {
        String next = reader.nextString();
        if (next != null) {
            return new BigInteger(next);
        } else {
            return null;
        }
    }

    @Override
    @ToJson
    public void toJson(JsonWriter writer, @Nullable BigInteger value) throws IOException {
        if (value != null) {
            writer.value(value.toString());
        } else {
            writer.nullValue();
        }
    }
}
