package de.upb.bibifi.verybest.common.models;

import com.squareup.moshi.FromJson;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.ToJson;
import de.upb.bibifi.verybest.common.util.BigIntegerAdapter;

import javax.annotation.Nullable;
import java.io.IOException;

public class AccountAdapter {
    private final JsonAdapter<AutoValue_Account> realJsonAdapter;

    public AccountAdapter() {
        Moshi moshi = new Moshi.Builder().add(new BigIntegerAdapter()).build();
        realJsonAdapter = moshi.adapter(AutoValue_Account.class);
    }

    @FromJson
    @Nullable
    public Account fromJson(@Nullable String string) throws IOException {
        if (string == null) {
            return null;
        }

        return realJsonAdapter.fromJson(string);
    }

    @ToJson
    public String toJson(@Nullable Account value) {
        return realJsonAdapter.toJson((AutoValue_Account) value);
    }
}
