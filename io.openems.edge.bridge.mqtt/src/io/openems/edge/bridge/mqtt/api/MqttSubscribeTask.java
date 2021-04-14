package io.openems.edge.bridge.mqtt.api;

import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;



public interface MqttSubscribeTask extends MqttTask {

    /**
     * Called by MqttSubscribeManager. Response to Payload.
     *
     * @param payload the Payload for the concrete MqttTask.
     */
    void response(String payload);

    /**
     * MessageId of the MqttTask. Given by the MqttBridge.
     *
     * @param messageId the Number of the message.
     */

    void putMessageId(int messageId);

    /**
     * For Future Implementation.
     *
     * @return the MessageId
     */
    int getMessageId();

    /**
     * Converts the time. Usually Called by Manager.
     *
     * @param timeZone given by Manager-Class.
     */
    void convertTime(DateTimeZone timeZone);

    DateTime getTime();

    /**
     * Get the Commands and their WrapperClass.
     *
     * @return The Map.
     */
    Map<MqttCommandType, CommandWrapper> getCommandValues();
}
