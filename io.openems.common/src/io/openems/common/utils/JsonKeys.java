package io.openems.common.utils;

public enum JsonKeys {

    ID("id"),
    USERS("users"),
    GROUPS("groups"),
    ROLES("roles"),
    USER_ID("userId"),
    NAME("name"),
    PASSWORD("password"),
    EMAIL("email"),
    DESCRIPTION("description"),
    PERMISSION("permission"),
    SYSTEM_LOG_PERMISSIONS("systemLogPermissions"),
    QUERY_HISTORIC_PERMISSIONS("queryHistoricPermissions"),
    EDGE_CONFIG_PERMISSIONS("edgeConfigPermissions"),
    CREATE_PERMISSIONS("createPermissions"),
    UPDATE_PERMISSIONS("updatePermissions"),
    DELETE_PERMISSIONS("deletePermissions"),
    SUBSCRIBE_CHANNEL_PERMISSIONS("subscribeChannelPermissions"),
    ASSIGNED_TO_GROUPS("assignedToGroups"),
    EDGES("edges"),
    EDGE_ID("edgeId"),
    PERMITTED_CHANNELS("permittedChannels"),
    API_KEY("apiKey"),
    COMMENT("comment"),
    COMPONENT_ID("componentId"),
    CHANNEL_ID("channelId");

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
