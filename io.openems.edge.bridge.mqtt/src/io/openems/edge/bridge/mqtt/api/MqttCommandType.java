package io.openems.edge.bridge.mqtt.api;

/**
 * CommandTypes supported by OpenEMS, if you want to handle more commands, just put them here.
 * The SubscribeTask will add them automatically to their Map.
 * After adding it here, go to your component and edit the implementation of your reactToCommand() method.
 */
public enum MqttCommandType {
    SETTEMPERATURE, SETSCHEDULE, SETPERFORMANCE, SETPOWER

}
