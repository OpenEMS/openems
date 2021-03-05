package io.openems.edge.bridge.mqtt.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StringWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.osgi.service.cm.ConfigurationException;

public interface MqttComponent extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * The ConfigurationChannel. If you want to Edit/Init your component via REST/JSON, write to this channel.
         */
        CONFIGURATION(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((StringWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        ));


        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    /**
     * Get the Configuration Channel, if configured by REST/ or json file.
     *
     * @return the channel
     */
    default WriteChannel<String> getConfiguration() {
        return this.channel(ChannelId.CONFIGURATION);
    }

    /**
     * Called By Mqtt Bridge. Component has to implement what to do with Events (Either a event happened internally and
     * tells the broker or vice versa).
     */
    void reactToEvent();

    /**
     * Called By Mqtt Bridge. Component has to implement what to do on commands set by mqtt bridge.
     */
    void reactToCommand();

    /**
     * Updates the JSON Config. Called by MqttBridge.
     *
     * @throws MqttException          If a problem occurred with the mqtt connection.
     * @throws ConfigurationException if the configuration is wrong.
     */
    void updateJsonConfig() throws MqttException, ConfigurationException;


    /**
     * Is Configuration done? --> either JSON Configuration done OR OSGi important for Bridge.
     *
     * @return aboolean;
     */
    boolean isConfigured();
}
