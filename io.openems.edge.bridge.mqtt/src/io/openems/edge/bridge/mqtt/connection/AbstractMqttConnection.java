package io.openems.edge.bridge.mqtt.connection;

import com.google.gson.JsonObject;
import io.openems.edge.bridge.mqtt.api.MqttConnection;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * A Mqtt Connection Created by either the mqttBridge, subscribe or publish-manager.
 */
public abstract class AbstractMqttConnection implements MqttConnection {
    //MqttClient, Informations by MqttBridge
    MqttClient mqttClient;
    private MemoryPersistence persistence;
    private MqttConnectOptions mqttConnectOptions;
    boolean cleanSessionFlag;

    private boolean disconnected = false;

    AbstractMqttConnection() {
        //protected boolean lastWillSet;
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
        this.mqttClient = new MqttClient(mqttBroker, mqttClientId, this.persistence);
        if (!username.trim().equals("")) {
            mqttConnectOptions.setUserName(username);
        }
        if (!username.trim().equals("")) {
            mqttConnectOptions.setPassword(mqttPassword.toCharArray());
        }
        mqttConnectOptions.setCleanSession(cleanSession);
        mqttConnectOptions.setKeepAliveInterval(keepAlive);
        mqttConnectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        mqttConnectOptions.setAutomaticReconnect(true);
        this.cleanSessionFlag = cleanSession;
    }


    /**
     * Creates the MqttSubscribe session.
     *
     * @param mqttBroker   URL of Broker usually from manager/bridge.
     * @param mqttClientId ClientID of the Connection.
     * @param username     username.
     * @param mqttPassword password.
     * @param keepAlive    keepalive.
     * @throws MqttException if connection fails or other problems occured with mqtt.
     */
    @Override
    public void createMqttSubscribeSession(String mqttBroker, String mqttClientId, String username, String mqttPassword, int keepAlive) throws MqttException {
        createMqttSessionBasicSetup(mqttBroker, mqttClientId, username, mqttPassword, false, keepAlive);
        connect();
    }

    /**
     * Creates the publish connection. Connection not already occurs bc a last will flag could be set.
     *
     * @param broker       URL of Broker usually from manager/bridge.
     * @param clientId     ClientID of the Connection.
     * @param keepAlive    keepalive flag.
     * @param username     username.
     * @param password     password.
     * @param cleanSession clean session flag.
     * @throws MqttException if connection fails or other problems occurred with mqtt.
     */
    @Override
    public void createMqttPublishSession(String broker, String clientId, int keepAlive, String username,
                                         String password, boolean cleanSession) throws MqttException {

        createMqttSessionBasicSetup(broker, clientId, username, password, cleanSession, keepAlive);

    }

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
    @Override
    public void addLastWill(String topicLastWill, String payloadLastWill, int qosLastWill, boolean shouldAddTime, boolean retainedFlag, String time) {
        JsonObject lastWillPayload = new JsonObject();

        if (shouldAddTime) {
            lastWillPayload.addProperty("time", time);

        }
        String[] payload = payloadLastWill.split(":");
        AtomicInteger counter = new AtomicInteger(0);
        Arrays.stream(payload).forEachOrdered(consumer -> {
            if (counter.get() % 2 == 0) {
                lastWillPayload.addProperty(consumer, payload[counter.incrementAndGet()]);
            }
        });
        mqttConnectOptions.setWill(topicLastWill, payloadLastWill.getBytes(), qosLastWill, retainedFlag);
    }


    /**
     * Connects with it's mqttConnectOptions to the broker.
     *
     * @throws MqttException will be thrown if configs are wrong or connection not available.
     */
    @Override
    public void connect() throws MqttException {
        System.out.println("Connecting to Broker");
        this.mqttClient.connect(this.mqttConnectOptions);
        this.disconnected = false;
        System.out.println("Connected");
    }

    @Override
    public void disconnect() throws MqttException {

        this.mqttClient.disconnect();

    }

    @Override
    public MqttClient getMqttClient() {
        return this.mqttClient;
    }

    /**
     * Usually false. Usually you don't want a clean session within an EMS. But for Future implementations:
     * If CleanSessions should be an option.
     *
     * @return aBoolean.
     */
    @Override
    public boolean isCleanSession() {
        return this.cleanSessionFlag;
    }

    public MqttConnectOptions getMqttConnectOptions() {
        return mqttConnectOptions;
    }
}
