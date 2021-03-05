package io.openems.edge.bridge.mqtt.api;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public interface MqttConnection {
    /**
     * Creates the MqttSubscribe session. HERE: AutoConnect
     *
     * @param mqttBroker   URL of Broker usually from manager/bridge.
     * @param mqttClientId ClientID of the Connection.
     * @param username     username.
     * @param mqttPassword password.
     * @param keepAlive    keepalive.
     * @throws MqttException if connection fails or other problems occured with mqtt.
     */
    void createMqttSubscribeSession(String mqttBroker, String mqttClientId, String username, String mqttPassword, int keepAlive) throws MqttException;

    /**
     * Creates the publish connection. Connection not already occurs bc a last will flag could be set.
     * HERE: NO Auto connect (Bc. of optional Last Will messages)
     *
     * @param broker       URL of Broker usually from manager/bridge.
     * @param clientId     ClientID of the Connection.
     * @param keepAlive    keepalive flag.
     * @param username     username.
     * @param password     password.
     * @param cleanSession clean session flag.
     * @throws MqttException if connection fails or other problems occurred with mqtt.
     */
    void createMqttPublishSession(String broker, String clientId, int keepAlive, String username,
                                  String password, boolean cleanSession) throws MqttException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException;

    /**
     * Adds last will to the    Connection.
     *
     * @param topicLastWill   topic of the last will.
     * @param payloadLastWill payload.
     * @param qosLastWill     Quality of service.
     * @param shouldAddTime   add Time to payload
     * @param retainedFlag    retained flag.
     * @param time            time as string.
     */
    void addLastWill(String topicLastWill, String payloadLastWill, int qosLastWill, boolean shouldAddTime, boolean retainedFlag, String time);

    /**
     * Connects with it's mqttConnectOptions to the broker.
     *
     * @throws MqttException will be thrown if configs are wrong or connection not available.
     */
    void connect() throws MqttException;

    void disconnect() throws MqttException;
}
