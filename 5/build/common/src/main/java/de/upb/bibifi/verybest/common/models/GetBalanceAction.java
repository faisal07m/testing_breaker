package de.upb.bibifi.verybest.common.models;

import java.util.Objects;

public class GetBalanceAction extends Action {
    private final String name;

    public GetBalanceAction(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetBalanceAction that = (GetBalanceAction) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "GetBalanceAction{" +
                "name='" + name + '\'' +
                '}';
    }
}
