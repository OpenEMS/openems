package io.openems.edge.bridge.mqtt.api;

/**
 * CommandTypes supported by OpenEMS, if you want to handle more commands, just put them here.
 * The SubscribeTask will automatically add and handle them
 * After adding it here, go to MqttCommands Nature and add corresponding Channels, as well as expand the MqttCommandComponent.
 */
public enum MqttCommandType {
    SETTEMPERATURE, SETSCHEDULE, SETPERFORMANCE, SETPOWER;

    /**
     * Checks if the commandType is correct.
     *
     * @param commandTypeCompare commandType. Usually in payload from the Broker.
     * @return a Boolean
     */

    public static boolean contains(String commandTypeCompare) {
        for (MqttCommandType mqttCommandType : MqttCommandType.values()) {
            if (mqttCommandType.name().equals(commandTypeCompare)) {
                return true;
            }
        }
        return false;
    }
}
