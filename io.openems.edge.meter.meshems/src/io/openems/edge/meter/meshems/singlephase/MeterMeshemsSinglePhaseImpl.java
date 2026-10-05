package io.openems.edge.meter.meshems.singlephase;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import com.google.gson.JsonObject;

import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.mqtt.api.BridgeMqtt;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.meter.meshems.AbstractMeterMeshems;

/**
 * MeshEMS OpenAMI single-phase tenant meter (SunSpec model 11).
 *
 * <p>
 * The device publishes a flat JSON object per message, e.g.:
 *
 * <pre>
 * {"model_id":11,"Phase":0,"Hz":50.02,"PhA":2.834,"PhV":228.5,"PhW":616.7,
 *  "VA":647.7,"Var":198.2,"PF":0.952,"TotWhImport":100286.4,"TotWhExport":0}
 * </pre>
 *
 * <p>
 * The baked-in mapping sets the aggregate {@link ElectricityMeter} values; the
 * per-phase L1/L2/L3 values are derived for the configured {@link SinglePhase}
 * via the {@link SinglePhaseMeter} helpers registered in the constructor.
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Meshems.SinglePhase", //
		immediate = true, //
		configurationPolicy = REQUIRE)
@GenerateTargetsFromReferences("MqttBridge")
public class MeterMeshemsSinglePhaseImpl extends AbstractMeterMeshems
		implements MeterMeshemsSinglePhase, ElectricityMeter, SinglePhaseMeter, OpenemsComponent, MqttComponent {

	@Override
	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.mqttBridgeId})(enabled=true))")
	protected void setMqttBridge(BridgeMqtt mqttBridge) {
		super.setMqttBridge(mqttBridge);
	}

	private Config config;

	public MeterMeshemsSinglePhaseImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				SinglePhaseMeter.ChannelId.values(), //
				MqttComponent.ChannelId.values() //
		);
		// Derive the per-phase values from the aggregate values based on the configured
		// phase.
		SinglePhaseMeter.calculateSinglePhaseFromActivePower(this);
		SinglePhaseMeter.calculateSinglePhaseFromReactivePower(this);
		SinglePhaseMeter.calculateSinglePhaseFromCurrent(this);
		SinglePhaseMeter.calculateSinglePhaseFromVoltage(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled(), config.topic());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public SinglePhase getPhase() {
		return this.config.phase();
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Override
	protected int expectedModelId() {
		return 11;
	}

	@Override
	protected void applyPayload(JsonObject json) {
		var invert = this.config.invert();

		// PhV (V) -> Voltage (mV); PhA (A) -> Current (mA)
		this._setVoltage(optInt(json, "PhV", 1000));
		this._setCurrent(optInt(json, "PhA", 1000));

		// PhW (W) -> ActivePower; Var (var) -> ReactivePower; negate if inverted
		var activePower = optInt(json, "PhW", 1);
		if (invert && activePower != null) {
			activePower = -activePower;
		}
		this._setActivePower(activePower);

		var reactivePower = optInt(json, "Var", 1);
		if (invert && reactivePower != null) {
			reactivePower = -reactivePower;
		}
		this._setReactivePower(reactivePower);

		// Hz (Hz) -> Frequency (mHz)
		this._setFrequency(optInt(json, "Hz", 1000));

		// TotWhImport -> ActiveConsumptionEnergy; TotWhExport -> ActiveProductionEnergy;
		// swap the two if inverted
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

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}
}
