package de.upb.bibifi.verybest.common.models;

import javax.annotation.Nullable;

public interface ActionSerializer {
    @Nullable
    Action fromJson(String string);

    @Nullable
    String toJson(Action action);
}
