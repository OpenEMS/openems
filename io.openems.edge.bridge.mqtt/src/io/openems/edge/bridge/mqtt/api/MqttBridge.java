package io.openems.edge.bridge.mqtt.api;

import java.util.List;

import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.joda.time.DateTimeZone;




public interface MqttBridge extends OpenemsComponent {

    DateTimeZone getTimeZone();

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
                .debounce(10, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)), //
        CYCLE_TIME_IS_TOO_SHORT(Doc.of(Level.WARNING) //
                .debounce(10, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)), //
        EXECUTION_DURATION(Doc.of(OpenemsType.LONG)),
        MQTT_TYPES(Doc.of(OpenemsType.STRING));

        private final Doc doc;

        private ChannelId(Doc doc) {
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

    List<MqttTask> getSubscribeTasks(String id);

    /**
     * Adds the MqttComponent to the Bridge; Used for Update ; React to Events/ Controls / etc.
     *
     * @param id        id of the MqttComponent usually from config of the Component
     * @param component the Component itself.
     * @return false if the key is already in List. (Happens on not unique ID)
     */
    boolean addMqttComponent(String id, MqttComponent component);

    /**
     * Removes the Mqtt  Component and their Tasks. Usually called on deactivation of the MqttComponent
     *
     * @param id id of the Component you want to remove.
     */
    void removeMqttComponent(String id);

    //NOT IN USE RN BUT ARE HERE FOR FUTURE IMPLEMENTATION
    List<MqttTask> getPublishTasks(String id);

    String getSubscribePayloadFromTopic(String topic, MqttType type);
}


