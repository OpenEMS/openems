package io.openems.edge.bridge.mqtt.api;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.List;

/**
 * This Interface provides the MqttBridge Nature. This allows the Telemetry/Command Component to add it's task to this
 * bridge and communicate via MQTT.
 */
public interface MqttBridge extends OpenemsComponent {

    /**
     * Check if the Given Component is in the MqttBridge.
     *
     * @param id the Id of the Component that should be checked
     * @return true if the corresponding Component is allocated
     */

    boolean containsComponent(String id);

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * The Mqtt Types Available, those will be written in the Config.
         * <ul>
         *     <li> Interface: MqttBridge
         *     <li> Type: String
         * </ul>
         */
        MQTT_TYPES(Doc.of(OpenemsType.STRING));

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
     * Adds Task to the Bridge.
     *
     * @param id       usually from Config of a concrete MqttComponent, called by abstractMqttComponent.
     * @param mqttTask MqttTask created by the AbstractMqttComponent.
     * @throws MqttException if subscription fails.
     */
    void addMqttTask(String id, MqttTask mqttTask) throws MqttException;

    /**
     * Remove the MqttTask by their ID. Removes all Tasks with the same ID --> Usually called on deactivation
     * of the Component or when Config is updated
     *
     * @param id ID of the Tasks usually from AbstractMqttComponent.
     */

    void removeMqttTasks(String id);

    default Channel<String> setMqttTypes() {
        return this.channel(ChannelId.MQTT_TYPES);
    }

    /**
     * List of all SubscribeTasks corresponding to the given device Id.
     *
     * @param id the id of the corresponding Component
     * @return the MqttTaskList.
     */
    List<MqttTask> getSubscribeTasks(String id);

    /**
     * Adds the MqttComponent to the Bridge; Used for Update ; React to Events/ Controls / etc.
     *
     * @param id        id of the MqttComponent usually from config of the Component
     * @param component the Component itself.
     */
    void addMqttComponent(String id, MqttComponent component);

    /**
     * Removes the Mqtt  Component and their Tasks. Usually called on deactivation of the MqttComponent
     *
     * @param id id of the Component you want to remove.
     */
    void removeMqttComponent(String id);

    /**
     * Checks if one of the Managers is connected to the Mqtt Server.
     *
     * @return true if the connection is established
     */
    boolean isConnected();

    /**
     * Gets the Publish tasks mapped to a MqttComponent.
     *
     * @param id the componentId
     * @return the List of MqttTasks
     */
    List<MqttTask> getPublishTasks(String id);


}


