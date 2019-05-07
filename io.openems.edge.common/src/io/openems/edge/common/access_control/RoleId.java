package io.openems.edge.common.access_control;

import java.util.Objects;

public class RoleId {

    private final String id;

    public RoleId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleId roleId = (RoleId) o;
        return Objects.equals(id, roleId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
