package io.openems.common.access_control;

public enum Permission {

    READ,
    WRITE;

    public static Permission from(String value) {
        return Permission.valueOf(value);
    }
}
