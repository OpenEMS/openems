package io.openems.edge.bridge.mqtt.component;

import io.openems.edge.bridge.mqtt.api.CommandWrapper;
import io.openems.edge.bridge.mqtt.api.MqttSubscribeTask;
import io.openems.edge.common.channel.Channel;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public interface MqttConfigurationComponent {
    /**
     * Inits the Tasks by Channel and Payloadstyle. IMPORTANT: Only called/exec if it was created by OSGi!
     *
     * @param channels     The Channel of the parent.
     * @param payloadStyle payloadStyle usually from config.
     * @throws MqttException          Throws MqttException if subscription fails.
     * @throws ConfigurationException If the Configuration has errors.
     */
    void initTasks(List<Channel<?>> channels, String payloadStyle) throws MqttException, ConfigurationException;

    boolean hasBeenConfigured();

    /**
     * Tells the parent if the Command is expired.
     *
     * @param task MqttTask given by Parent.
     * @param key  Commandwrapper used to get Expiration and InfiniteTime Status(==never expire).
     * @return aboolean
     */
    boolean expired(MqttSubscribeTask task, CommandWrapper key);

    /**
     * Update OSGi UI.
     *
     * @param configuration The Configuration of the parent.
     * @param channelIdList Name in Configuration --> where to put Channel.
     * @param channels      ChannelList (used for size)
     * @param length        Entry size of ChannelList (In Config)
     */
    void update(Configuration configuration, String channelIdList, List<Channel<?>> channels, int length);

    boolean isConfigured();

    /**
     * Init Json via JsonFile.
     *
     * @param channels    channels of Parent.
     * @param pathForJson Path to Json Config File.
     * @throws IOException            thrown if JsonFile does not exist.
     * @throws ConfigurationException if JsonFile has wrong config.
     * @throws MqttException          If subscription fails.
     */
    void initJson(ArrayList<Channel<?>> channels, String pathForJson) throws IOException, ConfigurationException, MqttException;

    /**
     * Updates the JsonConfig via OpenEmsChannel: Configuration
     *
     * @param channels Parent Channel
     * @param s        content of ConfigurationChannel
     * @throws ConfigurationException if wrong Config was given.
     * @throws MqttException          if subscription fails.
     */
    void updateJsonByChannel(ArrayList<Channel<?>> channels, String s) throws ConfigurationException, MqttException;

    /**
     * Checks if the Value is legit e.g. if it is "NotDefined" (No Value read yet) or "NaN" (Happens if a new Schedule is called by broker)
     *
     * @param value value of CommandWrapper in Parent Task.
     * @return aBoolean
     */
    boolean valueLegit(String value);
}
