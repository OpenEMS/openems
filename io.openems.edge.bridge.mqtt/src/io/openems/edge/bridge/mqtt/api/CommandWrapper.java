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

    /**
     * Get the Value send by the Command subscription.
     *
     * @return the value.
     */
    public String getValue() {
        return this.value;
    }

    /**
     * Set the value of a command.
     *
     * @param value the value, usually from SubscribeTask.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Get the Expiration of this command. (Date is saved in the Task. Expiration in seconds is stored here.
     *
     * @return the expiration.
     */
    public String getExpiration() {
        return this.expiration;
    }

    /**
     * If no expiration is given/set to INFINITE, this will be true.
     *
     * @return if the command holds up forever.
     */
    public boolean isInfinite() {
        return this.infinite;
    }

    /**
     * Sets the Expiration. In case the expiration should be infinite, the boolean will be set to true and called later in
     * MqttConfigurationComponent.
     *
     * @param expiration expirationTime usually set by MqttSubscribeTaskImpl.
     */
    void setExpiration(String expiration) {
        this.infinite = expiration.toUpperCase().trim().equals("INFINITE");

        this.expiration = expiration;
    }
}
