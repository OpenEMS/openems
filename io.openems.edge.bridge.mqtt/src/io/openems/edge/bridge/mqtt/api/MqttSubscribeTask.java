package io.openems.edge.bridge.mqtt.api;

import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;


/**
 * This provides the Interface for MqttSubscribeTasks. It allows the MqttSubscribeManager to access the tasks, as well as
 * the Command component to access all the command values.
 */
public interface MqttSubscribeTask extends MqttTask {

    /**
     * Called by MqttSubscribeManager. Response to Payload.
     *
     * @param payload the Payload for the concrete MqttTask.
     */
    void response(String payload);


    /**
     * Converts the time. Usually Called by Manager.
     *
     * @param timeZone given by Manager-Class.
     */
    void convertTime(DateTimeZone timeZone);

    /**
     * Gets the Time where the Payload was received (important for CommandValues -> check if command is expired).
     *
     * @return the Time (Joda-Time)
     */

    DateTime getTime();

    /**
     * Get the Commands and their WrapperClass.
     *
     * @return The Map.
     */
    Map<MqttCommandType, CommandWrapper> getCommandValues();
}
