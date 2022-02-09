package io.openems.edge.bridge.mqtt.component;

import io.openems.edge.bridge.mqtt.api.MqttBridge;
import io.openems.edge.bridge.mqtt.api.MqttSubscribeTask;
import io.openems.edge.bridge.mqtt.api.CommandWrapper;
import io.openems.edge.bridge.mqtt.api.MqttType;
import io.openems.edge.common.channel.Channel;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This Class provides the implementation of the MqttConfigurationComponent.
 * It allows e.g. the MqttTelemetryComponent to use the AbstractMqttComponent as well as some more Methods.
 * Such as initiation of Tasks, checking expiration of commands etc.
 */
public class MqttConfigurationComponentImpl implements MqttConfigurationComponent {


    private final MqttComponentImpl mqttComponent;

    public MqttConfigurationComponentImpl(String[] subscriptions, String[] publish, String[] payloads, String id,
                                          boolean createdByOsgi, MqttBridge mqttBridge, String mqttId, MqttType mqttType) {
        this.mqttComponent = new MqttComponentImpl(id, Arrays.asList(subscriptions), Arrays.asList(publish),
                Arrays.asList(payloads), createdByOsgi, mqttBridge, mqttId, mqttType);
    }

    /**
     * Initiates the Tasks by Channel and PayloadStyle. IMPORTANT: Only called/exec if it was created by OSGi!
     *
     * @param channels     The Channel of the parent.
     * @param payloadStyle payloadStyle usually from config.
     * @throws MqttException          Throws MqttException if subscription fails.
     * @throws ConfigurationException If the Configuration has errors.
     */
    @Override
    public void initTasks(List<Channel<?>> channels, String payloadStyle) throws MqttException, ConfigurationException {
        try {
            this.mqttComponent.initTasks(channels, payloadStyle);
            this.mqttComponent.setHasBeenConfigured(true);
        } catch (MqttException | ConfigurationException e) {
            this.mqttComponent.setHasBeenConfigured(false);
            throw e;
        }
    }

    /**
     * Check if the Components Configuration is done. (OSGi Config)
     *
     * @return a Boolean.
     */
    @Override
    public boolean hasBeenConfigured() {
        if (this.mqttComponent != null) {
            return this.mqttComponent.hasBeenConfigured();
        }
        return false;
    }

    /**
     * Tells the parent if the Command is expired.
     *
     * @param task MqttTask given by Parent.
     * @param key  CommandWrapper used to get Expiration and InfiniteTime Status(==never expire).
     * @return a Boolean
     */
    @Override
    public boolean expired(MqttSubscribeTask task, CommandWrapper key) {
        if (key.isInfinite()) {
            return false;
        }
        if (key.getValue().equals("NOTDEFINED") || key.getValue() == null) {
            return true;
        }
        return this.mqttComponent.expired(task, Integer.parseInt(key.getExpiration()));
    }

    /**
     * Update OSGi UI.
     *
     * @param configuration The Configuration of the parent.
     * @param channelIdList Name in Configuration --> where to put Channel.
     * @param channels      ChannelList (used for size)
     * @param length        Entry size of ChannelList (In Config)
     */
    @Override
    public void update(Configuration configuration, String channelIdList, List<Channel<?>> channels, int length) {
        this.mqttComponent.update(configuration, channelIdList, channels, length);
    }

    /**
     * Tells if the Component is ready and configured.
     *
     * @return a Boolean
     */
    @Override
    public boolean isConfigured() {
        return this.hasBeenConfigured();
    }

    /**
     * Init Json via JsonFile.
     *
     * @param channels    channels of Parent.
     * @param pathForJson Path to Json Config File.
     * @throws IOException            thrown if JsonFile does not exist.
     * @throws ConfigurationException if JsonFile has wrong config.
     * @throws MqttException          If subscription fails.
     */
    @Override
    public void initJson(ArrayList<Channel<?>> channels, String pathForJson) throws IOException, ConfigurationException, MqttException {
        this.mqttComponent.initJsonFromFile(channels, pathForJson);
    }

    /**
     * Updates the JsonConfig via OpenEmsChannel: Configuration.
     *
     * @param channels Parent Channel
     * @param content  content of ConfigurationChannel
     * @throws ConfigurationException if wrong Config was given.
     * @throws MqttException          if subscription fails.
     */
    @Override
    public void updateJsonByChannel(ArrayList<Channel<?>> channels, String content) throws ConfigurationException, MqttException {
        this.mqttComponent.initJson(channels, content);
    }

    /**
     * Checks if the Value is legitimate.
     * E.g. if it is "NotDefined" (No Value read yet) or "NaN" (Happens if a new Schedule is called by broker).
     *
     * @param value value of CommandWrapper in Parent Task.
     * @return a Boolean
     */

    @Override
    public boolean valueLegit(String value) {
        return !(value.toUpperCase().equals("NOTDEFINED") || value.toUpperCase().equals("NAN"));
    }

    /**
     * Getter for the {@link AbstractMqttComponent}.
     *
     * @return the AbstractMqttComponent
     */

    @Override
    public AbstractMqttComponent getAbstractComponent() {
        return this.mqttComponent;
    }


    private class MqttComponentImpl extends AbstractMqttComponent {
        /**
         * Initially update Config and after that set params for initTasks.
         *
         * @param id            id of this Component, usually from configuredDevice and it's config.
         * @param subConfigList Subscribe ConfigList, containing the Configuration for the subscribeTasks.
         * @param pubConfigList Publish ConfigList, containing the Configuration for the publishTasks.
         * @param payloads      containing all the Payloads. ConfigList got the Payload list as well.
         * @param createdByOsgi is this Component configured by OSGi or not. If not --> Read JSON File/Listen to Configuration Channel.
         * @param mqttBridge    mqttBridge of this Component.
         * @param mqttId        the Id showing up in the Payload in the Broker.
         * @param mqttType      the Type (e.g. Telemetry..Command etc)
         */
        public MqttComponentImpl(String id, List<String> subConfigList, List<String> pubConfigList, List<String> payloads,
                                 boolean createdByOsgi, MqttBridge mqttBridge, String mqttId, MqttType mqttType) {
            super(id, subConfigList, pubConfigList, payloads, createdByOsgi, mqttBridge, mqttId, mqttType);
        }
    }


}
