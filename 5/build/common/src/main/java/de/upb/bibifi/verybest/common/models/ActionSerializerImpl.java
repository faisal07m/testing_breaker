package de.upb.bibifi.verybest.common.models;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import de.upb.bibifi.verybest.common.util.MoshiProvider;

import javax.annotation.Nullable;
import java.io.IOException;

public class ActionSerializerImpl implements ActionSerializer {
    private final Moshi moshi;

    public ActionSerializerImpl(){
        moshi = MoshiProvider.provideMoshi();
    }

    @Override
    @Nullable
    public Action fromJson(String string) {
        JsonAdapter<Action> actionAdapter = moshi.adapter(Action.class);
        try {
            return actionAdapter.fromJson(string);
        } catch (IOException e) {
            e.printStackTrace();
            throw new AssertionError(e);
        }
    }

    @Override
    @Nullable
    public String toJson(Action action) {
        JsonAdapter<Action> actionAdapter = moshi.adapter(Action.class);
        return actionAdapter.toJson(action);
    }
}
