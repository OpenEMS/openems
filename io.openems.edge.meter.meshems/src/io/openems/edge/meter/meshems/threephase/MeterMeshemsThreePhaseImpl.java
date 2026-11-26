package io.openems.edge.meter.meshems.threephase;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import com.google.gson.JsonObject;

import io.openems.common.channel.AccessMode;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.mqtt.api.BridgeMqtt;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.meshems.AbstractMeterMeshems;

/**
 * MeshEMS OpenAMI three-phase subpanel meter (SunSpec model 213).
 *
 * <p>
 * The device publishes a flat JSON object per message with per-phase fields, the
 * relevant keys being:
 *
 * <pre>
 * {"model_id":213,"Hz":50.01,
 *  "AphA":2.5,"AphB":3.0,"AphC":1.5,
 *  "PhVphA":230.0,"PhVphB":231.0,"PhVphC":229.0,
 *  "VarphA":100.0,"VarphB":120.0,"VarphC":80.0,
 *  "WphA":575.0,"WphB":672.0,"WphC":345.0,
 *  "TotWhImport":123456.0,"TotWhExport":654.0}
 * </pre>
 *
 * <p>
 * The baked-in mapping sets the per-phase L1/L2/L3 values; the aggregate values
 * are derived via the {@link ElectricityMeter} {@code calculateSum*FromPhases}
 * helpers registered in the constructor.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Meshems.ThreePhase", //
		immediate = true, //
		configurationPolicy = REQUIRE)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
@GenerateTargetsFromReferences("MqttBridge")
public class MeterMeshemsThreePhaseImpl extends AbstractMeterMeshems
		implements MeterMeshemsThreePhase, ElectricityMeter, OpenemsComponent, MqttComponent, EventHandler, ModbusSlave {

	@Reference
	private ComponentManager componentManager;

	@Override
	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.mqttBridgeId})(enabled=true))")
	protected void setMqttBridge(BridgeMqtt mqttBridge) {
		super.setMqttBridge(mqttBridge);
	}

	private Config config;

	public MeterMeshemsThreePhaseImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MqttComponent.ChannelId.values() //
		);
		// Derive the aggregate values from the per-phase L1/L2/L3 values.
		ElectricityMeter.calculateSumActivePowerFromPhases(this);
		ElectricityMeter.calculateSumReactivePowerFromPhases(this);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), config.topic(),
				config.staleTimeoutSeconds(), this.componentManager);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.onCycleBeforeProcessImage();
			break;
		}
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Override
	protected int expectedModelId() {
		return 213;
	}

	@Override
	protected void applyPayload(JsonObject json) {
		var invert = this.config.invert();

		// PhVphA/B/C (V) -> VoltageL1/2/3 (mV)
		this._setVoltageL1(optInt(json, "PhVphA", 1000));
		this._setVoltageL2(optInt(json, "PhVphB", 1000));
		this._setVoltageL3(optInt(json, "PhVphC", 1000));

		// AphA/B/C (A) -> CurrentL1/2/3 (mA)
		this._setCurrentL1(optInt(json, "AphA", 1000));
		this._setCurrentL2(optInt(json, "AphB", 1000));
		this._setCurrentL3(optInt(json, "AphC", 1000));

		// WphA/B/C (W) -> ActivePowerL1/2/3; negated if inverted
		this._setActivePowerL1(invertIfTrue(optInt(json, "WphA", 1), invert));
		this._setActivePowerL2(invertIfTrue(optInt(json, "WphB", 1), invert));
		this._setActivePowerL3(invertIfTrue(optInt(json, "WphC", 1), invert));

		// VarphA/B/C (var) -> ReactivePowerL1/2/3; negated if inverted
		this._setReactivePowerL1(invertIfTrue(optInt(json, "VarphA", 1), invert));
		this._setReactivePowerL2(invertIfTrue(optInt(json, "VarphB", 1), invert));
		this._setReactivePowerL3(invertIfTrue(optInt(json, "VarphC", 1), invert));

		// Hz (Hz) -> Frequency (mHz)
		this._setFrequency(optInt(json, "Hz", 1000));

		// TotWhImport/TotWhExport -> energy; swapped if inverted
		this.applyEnergy(json, invert);
	}

	@Override
	protected void clearChannels() {
		this._setVoltageL1(null);
		this._setVoltageL2(null);
		this._setVoltageL3(null);
		this._setCurrentL1(null);
		this._setCurrentL2(null);
		this._setCurrentL3(null);
		this._setActivePowerL1(null);
		this._setActivePowerL2(null);
		this._setActivePowerL3(null);
		this._setReactivePowerL1(null);
		this._setReactivePowerL2(null);
		this._setReactivePowerL3(null);
		this._setFrequency(null);
		// ACTIVE_CONSUMPTION_ENERGY/ACTIVE_PRODUCTION_ENERGY are cumulated Channels;
		// OpenEMS ignores 'null' on them once defined, so they keep the last total.
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode) //
		);
	}
}
