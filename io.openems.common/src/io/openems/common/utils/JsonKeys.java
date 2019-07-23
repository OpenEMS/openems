package io.openems.common.utils;

public enum JsonKeys {

    ID("id"),
    USERS("users"),
    USER_NAME("username"),
    ROLE("role"),
    ROLES("roles"),
    NAME("name"),
    PASSWORD("password"),
    SALT("salt"),
    EMAIL("email"),
    DESCRIPTION("description"),
    PERMISSION("permission"),
    PARENTS("parentIds"),
    JSON_RPC("jsonRpc"),
    CHANNELS("channels"),
    EDGES("edges"),
    EDGE_ID("edgeId"),
    PERMITTED_CHANNELS("permittedChannels"),
    API_KEY("apiKey"),
    COMMENT("comment"),
    COMPONENT_ID("componentId"),
    CHANNEL_ID("channelId"),
    MACHINES("machines"),
    VERSION("version"),
    PRODUCT_TYPE("productType"),
    IPV4("ipv4"),
    SOC("soc"),
    MACHINE_TYPES("machineType");

    private String value;

    JsonKeys(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public String value() {
        return value;
    }
}
