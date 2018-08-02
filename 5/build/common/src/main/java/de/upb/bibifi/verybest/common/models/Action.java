package de.upb.bibifi.verybest.common.models;

public class Action {
    private final String type = getClass().getName();

    public String type() {
        return type;
    }
}
