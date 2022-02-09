package io.openems.edge.bridge.mqtt.connection;

import io.openems.edge.bridge.mqtt.api.MqttConnectionPublish;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * The MqttConnectionPublishImpl provides methods to send Messages via MQTT to a Cloud/Broker.
 * This connection is used by the PublishManager.
 */
public class MqttConnectionPublishImpl extends AbstractMqttConnection implements MqttConnectionPublish {

    public MqttConnectionPublishImpl() {
        super();
    }

    /**
     * Sends a message to broker, with defined params.
     *
     * @param topic      Topic of the payload.
     * @param message    Payload of the message.
     * @param qos        Quality of Service of this Message.
     * @param retainFlag Should the message be retained.
     * @throws MqttException if broker not available or somethings bad configured in the mqtt message.
     */
    @Override
    public void sendMessage(String topic, String message, int qos, boolean retainFlag) throws MqttException {
        MqttMessage messageMqtt;

        messageMqtt = new MqttMessage(message.getBytes());
        messageMqtt.setQos(qos);
        messageMqtt.setRetained(retainFlag);
        super.mqttClient.publish(topic, messageMqtt);
        super.log.info("Message published: " + messageMqtt);
    }

}
