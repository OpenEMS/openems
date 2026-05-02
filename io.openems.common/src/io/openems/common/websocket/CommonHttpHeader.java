package io.openems.common.websocket;

/**
 * Defines commonly used HTTP header names as constants for use across the OpenEMS codebase.
 */
public enum CommonHttpHeader {
    APIKEY("Apikey"),
    INSTANCE_ID("Instance-Id");

    private final String header;

    CommonHttpHeader(String header) {
        this.header = header;
    }

    /**
     * The HTTP header name as a {@link String}.
     *
     * @return header name
     */
    public String asString() {
        return this.header;
    }
}
