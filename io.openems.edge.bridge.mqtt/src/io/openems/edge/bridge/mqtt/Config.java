package io.openems.edge.bridge.mqtt;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;


@ObjectClassDefinition(
        name = "Bridge Mqtt",
        description = "Mqtt Bridge to communicate with a specific broker.")
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "MqttBridge - ID", description = "Id of Mqtt Bridge.")
    String id() default "MqttBridge";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    boolean useCoreCycleTime();

    @AttributeDefinition(name = "Broker URL", description = "URL of the Broker (if any given)..if you set a URL, you do NOT need to enter IP+ Port+ basepath + connection")
    String brokerUrl() default "";

    @AttributeDefinition(name = "Broker IP", description = "IP of the broker")
    String ipBroker() default "localhost";

    @AttributeDefinition(name = "Port", description = "The Port the broker opened for communication(1883 or 8883 are common)")
    int portBroker() default 1883;

    @AttributeDefinition(name = "Basepath", description = "The Basepath of the MQTT Connection.")
    String basepath() default "/ws";

    @AttributeDefinition(name = "Connection Type", description = "Tcp or TLS",
            options = {
                    @Option(label = "Tcp", value = "Tcp"),
                    @Option(label = "TLS", value = "ssl"),
                    @Option(label = "Websocket", value = "Wss")
            })
    String connection() default "Tcp";

    @AttributeDefinition(name = "Username", description = "Username for the Broker")
    String username() default "user";

    @AttributeDefinition(name = "Password", description = "Password", type = AttributeType.PASSWORD)
    String password() default "user";


    @AttributeDefinition(name = "ClientName", description = "ClientId used for brokerConnection")
    String clientId() default "OpenEMS-1";

    @AttributeDefinition(name = "Keep Alive", description = "Keep Alive in Seconds")
    int keepAlive() default 60;

    @AttributeDefinition(name = "LastWillSet", description = "Do you want a Last Will / Testament to be enabled")
    boolean lastWillSet() default true;

    @AttributeDefinition(name = "Topic Last Will", description = "Topic for Last Will")
    String topicLastWill() default "OpenEMS/Leaflet_0/Status/";


    @AttributeDefinition(name = "LastWill Payload", description = "Payload for the last Will")
    String payloadLastWill() default "Status : Connected";

    @AttributeDefinition(name = "QoS of Last Will", description = "Quality of Service of last Will Msg")
    int qosLastWill() default 0;

    @AttributeDefinition(name = "Clean Session Flag", description = "If set to false --> Persistent Session")
    boolean cleanSessionFlag() default true;

    @AttributeDefinition(name = "Retaines Message for Last Will?", description = "Retained Flag of Mqtt Last Will Message.")
    boolean retainedFlag() default true;

    @AttributeDefinition(name = "Use Time for Last Will", description = "Send a timestamp?")
    boolean timeStampEnabled() default true;

    @AttributeDefinition(name = "Mqtt Types", description = "Possible MqttTypes, will be filled after activation")
    String[] mqttTypes() default {""};

    @AttributeDefinition(name = "Mqtt Priorities", description = "MqttPriorities, will be filled automatically after activation")
    String[] mqttPriorities() default {""};

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Mqtt Bridge [{id}]";
}
