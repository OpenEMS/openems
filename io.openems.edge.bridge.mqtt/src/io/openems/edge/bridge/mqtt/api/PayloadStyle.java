package io.openems.edge.bridge.mqtt.api;

/**
 * Payload-Styles.
 * <p>
 * "STANDARD" PayloadStyle for publish (Telemetry) is:
 * {
 * TimeStamp : TIME ,
 * ID : Id -Of-The-Component,
 * "metrics" : {
 * NameForBroker: Value,
 * NAME : VALUE,
 * }
 * }
 * For Command it is:
 * {
 * "time": "TIME_ISO_UTC",
 * "method":MethodName (look up MqttCommandTypes)
 * "device": DeviceId (Usually can be ignored bc topics are unique therefore device ID is not important)
 * "value": Value (Value for the Method)
 * "expires": TimeInSeconds (Time till the Command Expires)
 * }
 * </p>
 * If you need different Payload-Styles add here an enum and add them to pub and sub task.
 */
public enum PayloadStyle {
    STANDARD;

    /**
     * Checks if the given PayloadStyle is supported.
     *
     * @param style the given PayloadStyle
     * @return a boolean.
     */
    public static boolean contains(String style) {
        for (PayloadStyle payloadStyle : PayloadStyle.values()) {
            if (payloadStyle.name().equals(style)) {
                return true;
            }
        }
        return false;
    }
}
