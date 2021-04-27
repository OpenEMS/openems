package io.openems.edge.bridge.mqtt.api;

/**
 * This Wrapper Class is needed by the SubscribeTask. It holds the Value and Expiration Time for a specific Method.
 */
public class CommandWrapper {

    private String value;
    private String expiration;
    private boolean infinite;

    public CommandWrapper(String value, String expiration) {
        this.value = value;
        this.expiration = expiration;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getExpiration() {
        return expiration;
    }

    public boolean isInfinite() {
        return infinite;
    }

    /**
     * Sets the Expiration. If The Expiration should be infinite. The Infinite Boolean will be set and called later in
     * MqttConfigurationComponent.
     *
     * @param expiration expirationTime usually set by MqttSubscribeTaskImpl.
     */
    void setExpiration(String expiration) {
        this.infinite = expiration.toUpperCase().trim().equals("INFINITE");

        this.expiration = expiration;
    }
}
