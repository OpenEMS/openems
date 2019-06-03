package io.openems.common.access_control;

public enum ExecutePermission {

    ALLOW,
    REFUSE;

    public static ExecutePermission from(String value) {
        return ExecutePermission.valueOf(value);
    }
}
