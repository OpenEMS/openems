package io.openems.edge.evcs.openwb;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.mqtt.api.BridgeMqtt;
import io.openems.edge.bridge.mqtt.api.BridgeMqtt.MqttSubscription;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.MqttMessage;
import io.openems.edge.bridge.mqtt.api.QoS;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.OpenWB", //
		immediate = true, //
		configurationPolicy = REQUIRE)
public class EvcsOpenWbImpl extends AbstractOpenemsComponent
		implements EvcsOpenWb, ElectricityMeter, OpenemsComponent, Evcs, TimedataProvider, ModbusSlave, MqttComponent {

	private final Logger log = LoggerFactory.getLogger(EvcsOpenWbImpl.class);

	private static final String TOPIC_PREFIX = "openWB/internal_chargepoint/";

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata;

	private volatile BridgeMqtt mqttBridge;

	private Config config;
	private String topicPrefix;
	private MqttSubscription subscription;
	private Integer energyStartSession = null;

	// State tracking for status calculation
	private Boolean lastPlugState = null;
	private Boolean lastChargeState = null;

	public EvcsOpenWbImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				MqttComponent.ChannelId.values() //
		);

		ElectricityMeter.calculateSumActivePowerFromPhases(this);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(enabled=true)")
	protected void setMqttBridge(BridgeMqtt mqttBridge) {
		this.mqttBridge = mqttBridge;
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		this.topicPrefix = TOPIC_PREFIX + config.chargePoint().value + "/get/";

		this._setStatus(Status.NOT_READY_FOR_CHARGING);
		this._setChargingType(ChargingType.AC);

		if (config.enabled()) {
			this.subscribeToMqtt();
		}
	}

	/**
	 * Subscribes to the OpenWB MQTT topics via the bridge.
	 */
	private void subscribeToMqtt() {
		try {
			var subscribeTopicPattern = this.topicPrefix + "#";
			this.subscription = this.mqttBridge.subscribe(subscribeTopicPattern, QoS.AT_LEAST_ONCE,
					this::handleMqttMessage);
			this.logInfo(this.log, "Subscribed to: " + subscribeTopicPattern);
			this._setMqttCommunicationFailed(false);
		} catch (Exception e) {
			this.logError(this.log, "Failed to subscribe to MQTT topics: " + e.getMessage());
			this._setMqttCommunicationFailed(true);
		}
	}

	/**
	 * Handles incoming MQTT messages from the bridge.
	 *
	 * @param message the received {@link MqttMessage}
	 */
	private void handleMqttMessage(MqttMessage message) {
		var payload = message.payloadAsString();

		try {
			var json = JsonParser.parseString(payload);
			this.handleMessage(message.topic(), json);
			this._setMqttCommunicationFailed(false);
		} catch (Exception e) {
			this.logWarn(this.log, "Failed to parse MQTT message on topic " + message.topic() + ": " + e.getMessage());
		}
	}

	@Deactivate
	protected void deactivate() {
		if (this.subscription != null) {
			try {
				this.subscription.unsubscribe();
			} catch (Exception e) {
				this.logWarn(this.log, "Error unsubscribing from MQTT: " + e.getMessage());
			}
			this.subscription = null;
		}
		super.deactivate();
	}

	@Override
	public void retryMqttCommunication() {
		// Unsubscribe if there's an existing subscription
		if (this.subscription != null) {
			try {
				this.subscription.unsubscribe();
			} catch (Exception e) {
				// Ignore errors during cleanup
			}
			this.subscription = null;
		}
		// Re-subscribe
		this.subscribeToMqtt();
	}

	/**
	 * Handles an incoming MQTT message.
	 *
	 * @param topic the MQTT topic
	 * @param json  the parsed JSON payload
	 */
	private void handleMessage(String topic, JsonElement json) {
		// Extract the subtopic (e.g., "voltages", "currents", etc.)
		var subtopic = topic.replace(this.topicPrefix, "");

		switch (subtopic) {
		case "voltages" -> this.handleVoltages(json);
		case "currents" -> this.handleCurrents(json);
		case "power" -> this.handlePower(json);
		case "powers" -> this.handlePowers(json);
		case "imported" -> this.handleImported(json);
		case "plug_state" -> this.handlePlugState(json);
		case "charge_state" -> this.handleChargeState(json);
		case "phases_in_use" -> this.handlePhasesInUse(json);
		case "fault_state" -> this.handleFaultState(json);
		default -> {
			// Ignore other topics
		}
		}
	}

	private void handleVoltages(JsonElement json) {
		if (json.isJsonArray()) {
			var arr = json.getAsJsonArray();
			this._setVoltageL1(Math.round(arr.get(0).getAsFloat() * 1000)); // V -> mV
			this._setVoltageL2(Math.round(arr.get(1).getAsFloat() * 1000));
			this._setVoltageL3(Math.round(arr.get(2).getAsFloat() * 1000));
		}
	}

	private void handleCurrents(JsonElement json) {
		if (json.isJsonArray()) {
			var arr = json.getAsJsonArray();
			this._setCurrentL1(Math.round(arr.get(0).getAsFloat() * 1000)); // A -> mA
			this._setCurrentL2(Math.round(arr.get(1).getAsFloat() * 1000));
			this._setCurrentL3(Math.round(arr.get(2).getAsFloat() * 1000));
		}
	}

	private void handlePower(JsonElement json) {
		this._setActivePower(Math.round(json.getAsFloat()));
	}

	private void handlePowers(JsonElement json) {
		if (json.isJsonArray()) {
			var arr = json.getAsJsonArray();
			this._setActivePowerL1(Math.round(arr.get(0).getAsFloat()));
			this._setActivePowerL2(Math.round(arr.get(1).getAsFloat()));
			this._setActivePowerL3(Math.round(arr.get(2).getAsFloat()));
		}
	}

	private void handleImported(JsonElement json) {
		var energyTotal = Math.round(json.getAsFloat());

		// Set energy channels directly from OpenWB readings
		// EVCS only consumes energy, so production is always 0
		this._setActiveProductionEnergy(0L);
		this._setActiveConsumptionEnergy(energyTotal);

		// Calculate session energy
		if (this.energyStartSession != null) {
			var energySession = (int) Math.max(0, energyTotal - this.energyStartSession);
			this._setEnergySession(energySession);
		}
	}

	private void handlePlugState(JsonElement json) {
		this.lastPlugState = json.getAsBoolean();
		this.updateStatus();
	}

	private void handleChargeState(JsonElement json) {
		this.lastChargeState = json.getAsBoolean();
		this.updateStatus();
	}

	private void handlePhasesInUse(JsonElement json) {
		this._setPhases(json.getAsInt());
	}

	private void handleFaultState(JsonElement json) {
		var faultState = json.getAsInt();
		// 0: No error, 1: Warning, 2: Error
		if (faultState >= 2) {
			this._setMqttCommunicationFailed(true);
		}
	}

	/**
	 * Updates the EVCS status based on plug and charge states.
	 */
	private void updateStatus() {
		if (this.lastPlugState == null || this.lastChargeState == null) {
			return;
		}

		Status previousStatus = this.getStatus();
		Status newStatus;

		if (this.lastChargeState) {
			newStatus = Status.CHARGING;
			if (previousStatus == Status.NOT_READY_FOR_CHARGING) {
				this.startNewSession();
			}
		} else if (this.lastPlugState) {
			newStatus = Status.READY_FOR_CHARGING;
			if (previousStatus == Status.NOT_READY_FOR_CHARGING) {
				this.startNewSession();
			}
		} else {
			newStatus = Status.NOT_READY_FOR_CHARGING;
		}

		this._setStatus(newStatus);
	}

	/**
	 * Starts a new charging session by recording the current energy total.
	 */
	private void startNewSession() {
		var consumptionEnergy = this.getActiveConsumptionEnergy().get();
		if (consumptionEnergy != null) {
			this.energyStartSession = consumptionEnergy.intValue();
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
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.CONSUMPTION_METERED;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				Evcs.getModbusSlaveNatureTable(accessMode) //
		);
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}
}
