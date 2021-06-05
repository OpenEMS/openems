package io.openems.edge.bridge.mqtt.api;

/**
 * This Enum provides Values for the AbstractMqttComponent and Connection to use -> easier maintenance.
 */
public enum ConfigurationSplits {

    //This TokenSplitter is used to Split the Configuration for Pub/Sub stuff
    //This is used to Split Key:ChannelId in Payload configs
    PAYLOAD_MAPPING_DIVIDER(2),
    //this will check if the Payload you are iterating through is Even --> Key or Odd --> ChannelId
    EVEN(0),

    //Priority!Topic!QoS!RetainFlag!TimeStampEnabled!PayloadNo!TimeToWait
    //The Size of the Publish/SubscribeConfig after Splitting it by the ConfigurationSplitter
    CONFIGURATION_SPLIT_SIZE(7),
    //The Position of the MqttPriority in the split String of a PublishListEntry
    PRIORITY_POSITION(0),
    //The Position of the MQTT Topic in the split String of a PublishListEntry
    TOPIC_POSITION(1),
    //The Position of the MQTT QoS in the split String of a PublishListEntry
    QOS_POSITION(2),
    //The Position of the MQTT RetainFlag in the split String of a PublishListEntry
    RETAIN_FLAG_POSITION(3),
    //The Position of the TimeStampEnabled in the split String of a PublishListEntry
    TIME_STAMP_ENABLED_POSITION(4),
    //The Position of the Payload in the split String of a PublishListEntry (The Payload will be handled later)
    PAYLOAD_NO_POSITION(5),
    //The Position of the Time To Wait in the split String of a PublishListEntry (Time that needs to pass between the last and current publish/subscribe)
    TIME_TO_WAIT_POSITION(6),
    //The Configuration Splitter -> Split the Configuration of the Publish/SubscribeList by this String/Character
    CONFIGURATION_SPLITTER("!"),
    //The Payload Mapping Splitter -> Split the Configuration of the Payload(s) by this Splitter -> e.g. Key:ChannelId
    PAYLOAD_MAPPING_SPLITTER(":");

    public int value;
    public String stringValue;

    ConfigurationSplits(int value) {
        this.value = value;
    }

    ConfigurationSplits(String value) {
        this.stringValue = value;
    }


}
