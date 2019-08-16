package io.openems.common.accesscontrol;

public enum ExecutePermission {

    ALLOW,
    REFUSE;

    public static ExecutePermission from(String value) {
        return ExecutePermission.valueOf(value);
    }
}
