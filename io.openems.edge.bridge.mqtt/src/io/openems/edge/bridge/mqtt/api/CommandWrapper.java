package io.openems.edge.bridge.mqtt.api;

/**
 * This Wrapper Class is needed by the SubscribeTask. It holds the Value and Expiration Time for a specific Method.
 * <p>
 * This is how a command works:
 * A Command is connected to a specific "Method" this method will be provided within the payload.
 * The SubscribeTask, will read through the Payload and get the "method" and place there a CommandWrapper.
 * The CommandWrapper contains a Value, that will be written to the corresponding channel, as long as the "Expiration"Time is not reached.
 * e.g. if a Command should be set at 1:00 pm and has an expiration time of 600 seconds -> reset the value at 1:10pm
 * or else use the value of the Method.
 * Supported Commands are listed in the {@link MqttCommandType}. Each Command has a corresponding Channel name in
 * {@link MqttCommands}.
 * The {@link io.openems.edge.bridge.mqtt.handler.MqttCommandComponent} handles the Commands and Expiration.
 * An OpenEMSComponent using Commands, needs only to implement the MqttCommands interface and react to the Values within the MqttCommands Channel.
 * </p>
 * <p>
 * E.g.:
 * {
 * "time": "2020-11-28T18:50:22.016751+00:00",
 * "method": "setPower",
 * "device": "chp-1",
 * "value": 15,
 * "expires": 900
 * }
 * </p>
 *
 * <p>
 * Here a Message was send / received at 18:50. The Method is called "setPower" with a value of 15 and expiration time of 900.
 * <p>
 * The SubscribeTask will Map the Command to the CommandWrapper and sets the value of 15 and the expiration of 900.
 *
 * </p>
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
