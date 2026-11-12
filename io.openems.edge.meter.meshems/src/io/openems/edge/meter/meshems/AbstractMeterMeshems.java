package io.openems.edge.meter.meshems;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

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
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * Shares the MQTT plumbing between the single-phase (SunSpec model 11) and
 * three-phase (SunSpec model 213) MeshEMS OpenAMI meters.
 *
 * <p>
 * The board publishes its SunSpec meter data as JSON over MQTT. This class owns
 * the {@link BridgeMqtt} subscription lifecycle. The MQTT callback only parses
 * and validates each message (JSON object, matching {@code model_id}) and keeps
 * the latest valid payload. The Channels are written atomically once per Cycle
 * from {@link #onCycleBeforeProcessImage()}, which the concrete implementation
 * calls on {@code TOPIC_CYCLE_BEFORE_PROCESS_IMAGE}. If no message was received
 * for longer than the configured stale timeout, all mapped Channels are cleared
 * via {@link #clearChannels()} and
 * {@link MqttComponent.ChannelId#MQTT_COMMUNICATION_FAILED} is set. The
 * SunSpec-field-to-Channel mapping itself is baked into each concrete
 * implementation (not operator-configured).
 */
public abstract class AbstractMeterMeshems extends AbstractOpenemsComponent
		implements ElectricityMeter, OpenemsComponent, MqttComponent {

	private final Logger log = LoggerFactory.getLogger(AbstractMeterMeshems.class);

	/** Injected by the concrete component via the overridden setter. */
	protected BridgeMqtt mqttBridge;

	/** The latest valid payload that has not been applied to the Channels yet. */
	private final AtomicReference<JsonObject> latestPayload = new AtomicReference<>(null);

	private ComponentManager componentManager;
	private String topic;
	private Duration staleTimeout;
	private MqttSubscription subscription;

	/** Instant of the latest valid message, or null if none was received yet. */
	private volatile Instant lastMessageAt = null;

	/** Guards that a {@code model_id} mismatch is logged only once. */
	private volatile boolean modelIdMismatchLogged = false;

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
	 * <p>
	 * All mapped Channels start undefined and
	 * {@link MqttComponent.ChannelId#MQTT_COMMUNICATION_FAILED} is only cleared
	 * once a valid payload has been applied, i.e. not merely by subscribing.
	 *
	 * @param context             the {@link ComponentContext}
	 * @param id                  the Component-ID
	 * @param alias               the alias
	 * @param enabled             whether the component is enabled
	 * @param topic               the MQTT topic filter to subscribe to
	 * @param staleTimeoutSeconds seconds without a message before the Channels
	 *                            are cleared
	 * @param componentManager    the {@link ComponentManager}, used for its
	 *                            {@link ComponentManager#getClock()}
	 */
	protected void activate(ComponentContext context, String id, String alias, boolean enabled, String topic,
			int staleTimeoutSeconds, ComponentManager componentManager) {
		super.activate(context, id, alias, enabled);
		this.topic = topic;
		this.staleTimeout = Duration.ofSeconds(staleTimeoutSeconds);
		this.componentManager = componentManager;
		this.latestPayload.set(null);
		this.lastMessageAt = null;
		this.modelIdMismatchLogged = false;
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
		if (this.topic == null || this.topic.isBlank()) {
			this.logError(this.log, "MQTT topic is not configured; set a topic (e.g. openami/<device-id>/meter_0)");
			this._setMqttCommunicationFailed(true);
			return;
		}
		try {
			this.subscription = this.mqttBridge.subscribe(this.topic, QoS.AT_LEAST_ONCE, this::handleMqttMessage);
			this.logInfo(this.log, "Subscribed to: " + this.topic);
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
	 * Handles an incoming MQTT message: parses the payload as JSON object,
	 * verifies the SunSpec {@code model_id} (if present) and stores the payload for
	 * the next Cycle. No Channel is written here.
	 *
	 * @param message the received {@link MqttMessage}
	 */
	private void handleMqttMessage(MqttMessage message) {
		try {
			var json = JsonParser.parseString(message.payloadAsString()).getAsJsonObject();
			var modelIdElement = json.get("model_id");
			if (modelIdElement != null && !modelIdElement.isJsonNull()) {
				var modelId = modelIdElement.getAsInt();
				if (modelId != this.expectedModelId()) {
					if (!this.modelIdMismatchLogged) {
						this.logWarn(this.log, "Rejecting SunSpec model_id [" + modelId + "] on topic "
								+ message.topic() + "; expected [" + this.expectedModelId() + "]");
						this.modelIdMismatchLogged = true;
					}
					this._setMqttCommunicationFailed(true);
					return;
				}
			}
			this.modelIdMismatchLogged = false;
			this.lastMessageAt = Instant.now(this.componentManager.getClock());
			this.latestPayload.set(json);
		} catch (Exception e) {
			this.logWarn(this.log,
					"Failed to parse MQTT message on topic " + message.topic() + ": " + e.getMessage());
			this._setMqttCommunicationFailed(true);
		}
	}

	/**
	 * Applies the latest pending payload to the Channels, or clears them if no
	 * message was received within the stale timeout. To be called by the concrete
	 * implementation on {@code TOPIC_CYCLE_BEFORE_PROCESS_IMAGE}.
	 */
	protected void onCycleBeforeProcessImage() {
		var payload = this.latestPayload.getAndSet(null);
		if (payload != null) {
			try {
				this.applyPayload(payload);
				this._setMqttCommunicationFailed(false);
			} catch (Exception e) {
				this.logWarn(this.log, "Failed to apply MQTT payload: " + e.getMessage());
				this.clearChannels();
				this._setMqttCommunicationFailed(true);
			}
			return;
		}
		var lastMessageAt = this.lastMessageAt;
		if (lastMessageAt == null || Duration
				.between(lastMessageAt, Instant.now(this.componentManager.getClock()))
				.compareTo(this.staleTimeout) > 0) {
			this.clearChannels();
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
	 * implementation. A field that is absent from the payload sets its Channel to
	 * UNDEFINED.
	 *
	 * @param json the parsed JSON payload
	 */
	protected abstract void applyPayload(JsonObject json);

	/**
	 * Sets every Channel that {@link #applyPayload(JsonObject)} maps to UNDEFINED.
	 * Called when no message was received within the stale timeout. Cumulated
	 * energy Channels are excluded: OpenEMS ignores 'null' on them once defined.
	 */
	protected abstract void clearChannels();

	/**
	 * Maps {@code TotWhImport} to {@code ACTIVE_CONSUMPTION_ENERGY} and
	 * {@code TotWhExport} to {@code ACTIVE_PRODUCTION_ENERGY}; the two are swapped
	 * if {@code invert} is set.
	 *
	 * @param json   the parsed JSON payload
	 * @param invert whether to swap consumption and production energy
	 */
	protected void applyEnergy(JsonObject json, boolean invert) {
		var consumptionEnergy = optLong(json, "TotWhImport", 1);
		var productionEnergy = optLong(json, "TotWhExport", 1);
		if (invert) {
			var tmp = consumptionEnergy;
			consumptionEnergy = productionEnergy;
			productionEnergy = tmp;
		}
		this._setActiveConsumptionEnergy(consumptionEnergy);
		this._setActiveProductionEnergy(productionEnergy);
	}

	/**
	 * Negates the value if {@code invert} is set; null stays null.
	 *
	 * @param value  the value or null
	 * @param invert whether to negate
	 * @return the (negated) value or null
	 */
	protected static Integer invertIfTrue(Integer value, boolean invert) {
		if (invert && value != null) {
			return -value;
		}
		return value;
	}

	/**
	 * Reads a numeric JSON field, scales it and rounds it to an Integer. Returns
	 * {@code null} unless the field is a finite JSON number, so the Channel becomes
	 * UNDEFINED for absent, null, non-numeric, NaN or infinite values.
	 *
	 * @param json  the JSON payload
	 * @param field the field name
	 * @param scale the factor applied to the raw value
	 * @return the scaled, rounded value or {@code null}
	 */
	protected static Integer optInt(JsonObject json, String field, double scale) {
		var value = optFiniteDouble(json, field);
		if (value == null) {
			return null;
		}
		return (int) Math.round(value * scale);
	}

	/**
	 * Reads a numeric JSON field, scales it and rounds it to a Long. Returns
	 * {@code null} unless the field is a finite JSON number, so the Channel becomes
	 * UNDEFINED for absent, null, non-numeric, NaN or infinite values.
	 *
	 * @param json  the JSON payload
	 * @param field the field name
	 * @param scale the factor applied to the raw value
	 * @return the scaled, rounded value or {@code null}
	 */
	protected static Long optLong(JsonObject json, String field, double scale) {
		var value = optFiniteDouble(json, field);
		if (value == null) {
			return null;
		}
		return Math.round(value * scale);
	}

	private static Double optFiniteDouble(JsonObject json, String field) {
		var element = json.get(field);
		if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
			return null;
		}
		var value = element.getAsDouble();
		if (!Double.isFinite(value)) {
			return null;
		}
		return value;
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
