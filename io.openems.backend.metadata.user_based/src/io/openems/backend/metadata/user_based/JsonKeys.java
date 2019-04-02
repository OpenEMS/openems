package io.openems.backend.metadata.user_based;

public enum JsonKeys {

    USERS("users"),
    USER_ID("userId"),
    NAME("name"),
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

    public String getValue() {
        return value;
    }
}
