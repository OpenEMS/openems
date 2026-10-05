package io.openems.edge.meter.meshems;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.edge.bridge.mqtt.api.BridgeMqtt;
import io.openems.edge.bridge.mqtt.api.BridgeMqtt.MqttSubscription;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.MqttMessage;
import io.openems.edge.bridge.mqtt.api.QoS;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Shares the MQTT plumbing between the single-phase (SunSpec model 11) and
 * three-phase (SunSpec model 213) MeshEMS OpenAMI meters.
 *
 * <p>
 * The board publishes its SunSpec meter data as JSON over MQTT. This class owns
 * the {@link BridgeMqtt} subscription lifecycle, parses each received message as
 * a JSON object, performs an optional {@code model_id} sanity check and then
 * hands the payload to the concrete implementation via
 * {@link #applyPayload(JsonObject)}. The SunSpec-field-to-Channel mapping itself
 * is baked into each concrete implementation (not operator-configured).
 */
public abstract class AbstractMeterMeshems extends AbstractOpenemsComponent
		implements ElectricityMeter, OpenemsComponent, MqttComponent {

	private final Logger log = LoggerFactory.getLogger(AbstractMeterMeshems.class);

	/** Injected by the concrete component via the overridden setter. */
	protected BridgeMqtt mqttBridge;

	private String topic;
	private MqttSubscription subscription;

	protected AbstractMeterMeshems(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	/**
	 * Sets the {@link BridgeMqtt} reference. The concrete component overrides this
	 * with the {@code @Reference} annotation and the {@code mqttBridgeId} target
	 * filter.
	 *
	 * @param mqttBridge the {@link BridgeMqtt}
	 */
	protected void setMqttBridge(BridgeMqtt mqttBridge) {
		this.mqttBridge = mqttBridge;
	}

	/**
	 * Activates the component and subscribes to the MQTT topic.
	 *
	 * @param context the {@link ComponentContext}
	 * @param id      the Component-ID
	 * @param alias   the alias
	 * @param enabled whether the component is enabled
	 * @param topic   the MQTT topic filter to subscribe to
	 */
	protected void activate(ComponentContext context, String id, String alias, boolean enabled, String topic) {
		super.activate(context, id, alias, enabled);
		this.topic = topic;
		if (enabled) {
			this.subscribeToMqtt();
		}
	}

	@Override
	protected void deactivate() {
		this.unsubscribeFromMqtt();
		super.deactivate();
	}

	private void subscribeToMqtt() {
		try {
			this.subscription = this.mqttBridge.subscribe(this.topic, QoS.AT_LEAST_ONCE, this::handleMqttMessage);
			this.logInfo(this.log, "Subscribed to: " + this.topic);
			this._setMqttCommunicationFailed(false);
		} catch (Exception e) {
			this.logError(this.log, "Failed to subscribe to MQTT topic [" + this.topic + "]: " + e.getMessage());
			this._setMqttCommunicationFailed(true);
		}
	}

	private void unsubscribeFromMqtt() {
		if (this.subscription != null) {
			try {
				this.subscription.unsubscribe();
			} catch (Exception e) {
				this.logWarn(this.log, "Error unsubscribing from MQTT: " + e.getMessage());
			}
			this.subscription = null;
		}
	}

	@Override
	public void retryMqttCommunication() {
		this.unsubscribeFromMqtt();
		this.subscribeToMqtt();
	}

	/**
	 * Handles an incoming MQTT message: parses the JSON payload, verifies the
	 * SunSpec model and applies the baked-in mapping of the concrete
	 * implementation.
	 *
	 * @param message the received {@link MqttMessage}
	 */
	private void handleMqttMessage(MqttMessage message) {
		var payload = message.payloadAsString();
		try {
			var json = JsonParser.parseString(payload).getAsJsonObject();
			if (json.has("model_id") && !json.get("model_id").isJsonNull()) {
				var modelId = json.get("model_id").getAsInt();
				if (modelId != this.expectedModelId()) {
					this.logWarn(this.log, "Unexpected SunSpec model_id [" + modelId + "] on topic " + message.topic()
							+ "; expected [" + this.expectedModelId() + "]");
				}
			}
			this.applyPayload(json);
			this._setMqttCommunicationFailed(false);
		} catch (Exception e) {
			this.logWarn(this.log,
					"Failed to parse MQTT message on topic " + message.topic() + ": " + e.getMessage());
			this._setMqttCommunicationFailed(true);
		}
	}

	/**
	 * The SunSpec model-id this concrete implementation expects (11 or 213).
	 *
	 * @return the expected model-id
	 */
	protected abstract int expectedModelId();

	/**
	 * Applies the baked-in SunSpec-field-to-Channel mapping of the concrete
	 * implementation.
	 *
	 * @param json the parsed JSON payload
	 */
	protected abstract void applyPayload(JsonObject json);

	/**
	 * Reads a numeric JSON field, scales it and rounds it to an Integer. Returns
	 * {@code null} if the field is absent or null, so missing fields are skipped.
	 *
	 * @param json  the JSON payload
	 * @param field the field name
	 * @param scale the factor applied to the raw value
	 * @return the scaled, rounded value or {@code null}
	 */
	protected static Integer optInt(JsonObject json, String field, double scale) {
		var element = json.get(field);
		if (element == null || element.isJsonNull()) {
			return null;
		}
		return (int) Math.round(element.getAsDouble() * scale);
	}

	/**
	 * Reads a numeric JSON field, scales it and truncates it to a Long. Returns
	 * {@code null} if the field is absent or null, so missing fields are skipped.
	 *
	 * @param json  the JSON payload
	 * @param field the field name
	 * @param scale the factor applied to the raw value
	 * @return the scaled value as Long or {@code null}
	 */
	protected static Long optLong(JsonObject json, String field, double scale) {
		var element = json.get(field);
		if (element == null || element.isJsonNull()) {
			return null;
		}
		return (long) (element.getAsDouble() * scale);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link MqttComponent.ChannelId#MQTT_COMMUNICATION_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	protected void _setMqttCommunicationFailed(boolean value) {
		this.channel(MqttComponent.ChannelId.MQTT_COMMUNICATION_FAILED).setNextValue(value);
	}
}
