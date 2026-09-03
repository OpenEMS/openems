package io.openems.edge.meter.mqtt;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.common.types.MeterType;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.mqtt.api.BridgeMqtt;
import io.openems.edge.bridge.mqtt.api.BridgeMqtt.MqttSubscription;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.MqttMessage;
import io.openems.edge.bridge.mqtt.api.QoS;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Mqtt", //
		immediate = true, //
		configurationPolicy = REQUIRE)
@GenerateTargetsFromReferences("mqttBridge")
public class MeterMqttImpl extends AbstractOpenemsComponent
		implements MeterMqtt, ElectricityMeter, OpenemsComponent, MqttComponent {

	private final Logger log = LoggerFactory.getLogger(MeterMqttImpl.class);
	private final List<Mapping> mappings = new ArrayList<>();

	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.mqttBridgeId})(enabled=true))")
	private BridgeMqtt mqttBridge;

	private Config config;
	private MqttSubscription subscription;

	public MeterMqttImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MqttComponent.ChannelId.values() //
		);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		this.parseMappings(config.mapping());

		if (config.enabled()) {
			this.subscribeToMqtt();
		}
	}

	@Deactivate
	protected void deactivate() {
		this.unsubscribeFromMqtt();
		super.deactivate();
	}

	/**
	 * Parses the configured mapping entries into {@link Mapping} objects. Malformed
	 * entries are logged and skipped.
	 *
	 * @param mapping the raw mapping entries from configuration
	 */
	private void parseMappings(String[] mapping) {
		this.mappings.clear();
		if (mapping == null) {
			return;
		}
		for (var entry : mapping) {
			if (entry == null || entry.isBlank()) {
				continue;
			}
			var parts = entry.split(":");
			if (parts.length != 3) {
				this.logWarn(this.log, "Ignoring malformed mapping entry [" + entry + "]: "
						+ "expected format \"jsonField:CHANNEL:scale\"");
				continue;
			}
			var jsonField = parts[0].trim();
			var channelName = parts[1].trim();
			ElectricityMeter.ChannelId channelId;
			try {
				channelId = ElectricityMeter.ChannelId.valueOf(channelName);
			} catch (IllegalArgumentException e) {
				this.logWarn(this.log, "Ignoring mapping entry [" + entry + "]: unknown Channel [" + channelName + "]");
				continue;
			}
			double scale;
			try {
				scale = Double.parseDouble(parts[2].trim());
			} catch (NumberFormatException e) {
				this.logWarn(this.log, "Ignoring mapping entry [" + entry + "]: invalid scale [" + parts[2] + "]");
				continue;
			}
			this.mappings.add(new Mapping(jsonField, channelId, scale));
		}
	}

	/**
	 * Subscribes to the configured MQTT topic via the bridge.
	 */
	private void subscribeToMqtt() {
		try {
			this.subscription = this.mqttBridge.subscribe(this.config.topic(), QoS.AT_LEAST_ONCE,
					this::handleMqttMessage);
			this.logInfo(this.log, "Subscribed to: " + this.config.topic());
			this._setMqttCommunicationFailed(false);
		} catch (Exception e) {
			this.logError(this.log, "Failed to subscribe to MQTT topic [" + this.config.topic() + "]: " + e.getMessage());
			this._setMqttCommunicationFailed(true);
		}
	}

	/**
	 * Unsubscribes from the MQTT topic, if subscribed.
	 */
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
	 * Handles an incoming MQTT message: parses the flat JSON payload and applies the
	 * configured mappings to the {@link ElectricityMeter} channels.
	 *
	 * @param message the received {@link MqttMessage}
	 */
	private void handleMqttMessage(MqttMessage message) {
		var payload = message.payloadAsString();
		try {
			var json = JsonParser.parseString(payload).getAsJsonObject();
			this.applyMappings(json);
			this._setMqttCommunicationFailed(false);
		} catch (Exception e) {
			this.logWarn(this.log,
					"Failed to parse MQTT message on topic " + message.topic() + ": " + e.getMessage());
			this._setMqttCommunicationFailed(true);
		}
	}

	/**
	 * Applies the configured mappings to the given JSON object.
	 *
	 * @param json the parsed JSON payload
	 */
	private void applyMappings(JsonObject json) {
		for (var mapping : this.mappings) {
			var element = json.get(mapping.jsonField());
			if (element == null || element.isJsonNull()) {
				this.logDebug(this.log, "Field [" + mapping.jsonField() + "] absent in payload; skipping");
				continue;
			}
			var scaled = element.getAsDouble() * mapping.scale();
			var channel = this.channel(mapping.channelId());
			if (channel.getType() == OpenemsType.LONG) {
				channel.setNextValue((long) scaled);
			} else {
				channel.setNextValue((int) Math.round(scaled));
			}
		}
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link MqttComponent.ChannelId#MQTT_COMMUNICATION_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	private void _setMqttCommunicationFailed(boolean value) {
		this.channel(MqttComponent.ChannelId.MQTT_COMMUNICATION_FAILED).setNextValue(value);
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	/**
	 * A single JSON-field-to-Channel mapping.
	 *
	 * @param jsonField the JSON field name to read
	 * @param channelId the target {@link ElectricityMeter.ChannelId}
	 * @param scale     the factor applied to the raw JSON value
	 */
	private record Mapping(String jsonField, ElectricityMeter.ChannelId channelId, double scale) {
	}
}
