package io.openems.edge.bridge.mqtt.connection;

import com.google.gson.JsonObject;
import io.openems.edge.bridge.mqtt.api.MqttConnection;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static io.openems.edge.bridge.mqtt.api.ConfigurationSplits.PAYLOAD_MAPPING_SPLITTER;


/**
 * A Mqtt Connection Created by either the MqttBridge, subscribe or publish-manager.
 * This Component handles the MqttConnection and stores the config params in its {@link #mqttConnectOptions}.
 */
public abstract class AbstractMqttConnection implements MqttConnection, MqttCallbackExtended {

    protected final Logger log = LoggerFactory.getLogger(AbstractMqttConnection.class);
    //MqttClient, information by MqttBridge
    MqttClient mqttClient;
    private final MemoryPersistence persistence;
    private final MqttConnectOptions mqttConnectOptions;
    boolean cleanSessionFlag;

    AbstractMqttConnection() {
        this.persistence = new MemoryPersistence();
        this.mqttConnectOptions = new MqttConnectOptions();
    }

    /**
     * BasicSetup for a Mqtt connection.
     *
     * @param mqttBroker   Broker URL with tcp:// | ssl:// | wss:// prefix
     * @param mqttClientId Client ID usually from Bridge.
     * @param username     username usually from Bridge.
     * @param mqttPassword password for broker usually from Bridge.
     * @param cleanSession cleanSession flag.
     * @param keepAlive    keepAlive of the Session.
     * @throws MqttException is throw if somethings wrong with the Client/Options.
     */
    private void createMqttSessionBasicSetup(String mqttBroker, String mqttClientId, String username, String mqttPassword,
                                             boolean cleanSession, int keepAlive) throws MqttException {
        this.mqttClient = new MqttClient(mqttBroker, mqttClientId + "_RandomId_" + new Random().nextInt(), this.persistence);
        if (!username.trim().equals("")) {
            this.mqttConnectOptions.setUserName(username);
        }
        if (!username.trim().equals("")) {
            this.mqttConnectOptions.setPassword(mqttPassword.toCharArray());
        }
        this.mqttConnectOptions.setCleanSession(cleanSession);
        this.mqttConnectOptions.setKeepAliveInterval(keepAlive);
        this.mqttConnectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        this.mqttClient.setCallback(this);
        this.mqttConnectOptions.setAutomaticReconnect(true);
        this.cleanSessionFlag = cleanSession;
    }


    /**
     * Creates the MqttSubscribe session.
     *
     * @param mqttBroker   URL of Broker usually from manager/bridge.
     * @param mqttClientId ClientID of the Connection.
     * @param username     username.
     * @param mqttPassword password.
     * @param keepAlive    keepAlive.
     * @throws MqttException if connection fails or other problems occurred with mqtt.
     */
    @Override
    public void createMqttSubscribeSession(String mqttBroker, String mqttClientId, String username, String mqttPassword, int keepAlive) throws MqttException {
        this.createMqttSessionBasicSetup(mqttBroker, mqttClientId, username, mqttPassword, false, keepAlive);
        this.connect();
    }

    /**
     * Creates the publish connection. Connection not already occurs bc a last will flag could be set.
     *
     * @param broker       URL of Broker usually from manager/bridge.
     * @param clientId     ClientID of the Connection.
     * @param keepAlive    keepAlive flag.
     * @param username     username.
     * @param password     password.
     * @param cleanSession clean session flag.
     * @throws MqttException if connection fails or other problems occurred with mqtt.
     */
    @Override
    public void createMqttPublishSession(String broker, String clientId, int keepAlive, String username,
                                         String password, boolean cleanSession) throws MqttException {

        this.createMqttSessionBasicSetup(broker, clientId, username, password, cleanSession, keepAlive);

    }

    /**
     * Adds last will to the Connection.
     *
     * @param topicLastWill   topic of the last will.
     * @param payloadLastWill payload.
     * @param qosLastWill     Quality of service.
     * @param shouldAddTime   add Time to payload
     * @param retainedFlag    retained flag.
     * @param time            time as string.
     */
    @Override
    public void addLastWill(String topicLastWill, String payloadLastWill, int qosLastWill, boolean shouldAddTime, boolean retainedFlag, String time) {
        JsonObject lastWillPayload = new JsonObject();

        if (shouldAddTime) {
            lastWillPayload.addProperty("time", time);

        }
        String[] payload = payloadLastWill.split(PAYLOAD_MAPPING_SPLITTER.stringValue);
        AtomicInteger counter = new AtomicInteger(0);
        Arrays.stream(payload).forEachOrdered(consumer -> {
            if (counter.get() % 2 == 0) {
                lastWillPayload.addProperty(consumer, payload[counter.incrementAndGet()]);
            }
        });
        this.mqttConnectOptions.setWill(topicLastWill, payloadLastWill.getBytes(), qosLastWill, retainedFlag);
    }


    /**
     * Connects with it's this.mqttConnectOptions to the broker.
     *
     * @throws MqttException will be thrown if configs are wrong or connection not available.
     */
    @Override
    public void connect() throws MqttException {
        this.log.info("Connecting to Broker: " + this.mqttClient.getClientId());
        this.mqttClient.connect(this.mqttConnectOptions);
        this.log.info("Connected: " + this.mqttClient.getClientId());
    }

    /**
     * Disconnects the Connection. Happens on deactivation. Only for internal usage.
     *
     * @throws MqttException if somethings wrong with the MQTT Connection.
     */
    @Override
    public void disconnect() throws MqttException {

        this.mqttClient.disconnect();
    }

    /**
     * Checks if the Connection is still available.
     *
     * @return a Boolean.
     */
    public boolean isConnected() {
        return this.mqttClient.isConnected();
    }

    @Override
    public void connectComplete(boolean b, String s) {
        this.log.info("Connected to Broker" + this.mqttClient.getClientId());
    }

    @Override
    public void connectionLost(Throwable throwable) {
        this.log.warn("Connection to Broker lost " + this.mqttClient.getClientId());
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }
}
