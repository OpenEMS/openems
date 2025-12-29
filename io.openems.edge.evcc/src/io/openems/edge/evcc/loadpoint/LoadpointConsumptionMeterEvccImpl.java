package io.openems.edge.evcc.loadpoint;

import static io.openems.common.utils.JsonUtils.getAsDouble;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.timedata.api.Timedata;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.MeterType;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcs.api.DeprecatedEvcs;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.SocEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Consumption.Loadpoint.Evcc", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { "type=CONSUMPTION_METERED" } //
)
@EventTopics(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class LoadpointConsumptionMeterEvccImpl extends AbstractLoadpointMeterEvcc
		implements LoadpointConsumptionMeterEvcc, SocEvcs, Evcs, DeprecatedEvcs, ElectricityMeter, OpenemsComponent, TimedataProvider {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Reference
	private BridgeHttpFactory httpBridgeFactory;

	@Reference
	private HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	private MeterType meterType;

	public LoadpointConsumptionMeterEvccImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				SocEvcs.ChannelId.values(), //
				DeprecatedEvcs.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				LoadpointConsumptionMeterEvcc.ChannelId.values() //
		);

		// Copy production energy to consumption energy (Keba pattern)
		DeprecatedEvcs.copyToDeprecatedEvcsChannels(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.meterType = config.type();

		this._setChargingType(io.openems.edge.evcs.api.ChargingType.AC);
		this._setFixedMinimumHardwarePower(config.minChargingPowerW());
		this._setFixedMaximumHardwarePower(config.maxChargingPowerW());
		Evcs.addCalculatePowerLimitListeners(this);

		this.activateHttpSubscription(context, config.id(), config.alias(), config.enabled(),
				config.apiUrl(), config.loadpointTitle(), config.loadpointIndex(), this.log);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.deactivateHttpSubscription();
	}

	@Override
	protected void processHttpResult(HttpResponse<JsonElement> result, HttpError error) {
		if (error != null) {
			this.logDebug(this.log, error.getMessage());
			this._setChargingstationCommunicationFailed(true);
			return;
		}
		this._setChargingstationCommunicationFailed(false);

		try {
			var lp = getAsJsonObject(result.data());
			this.checkLoadpointMatch(lp, this.log);

			// Power
			int chargePower = 0;
			if (lp.has("chargePower")) {
				chargePower = (int) Math.round(getAsDouble(lp, "chargePower"));
				this._setActivePower(chargePower);
			} else {
				this._setActivePower(null);
			}

			// Phases
			int phases = lp.has("phasesActive") ? lp.get("phasesActive").getAsInt() : 0;
			this.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_PHASES).setNextValue(phases);
			this._setPhases(phases > 0 ? phases : 3);

			// Plug state and status
			boolean connected = lp.has("connected") && lp.get("connected").getAsBoolean();
			boolean charging = lp.has("charging") && lp.get("charging").getAsBoolean();
			this.channel(LoadpointConsumptionMeterEvcc.ChannelId.PLUG)
					.setNextValue(connected ? PlugState.CONNECTED : PlugState.UNPLUGGED);
			if (!connected) {
				this._setStatus(Status.NOT_READY_FOR_CHARGING);
			} else if (charging) {
				this._setStatus(Status.CHARGING);
			} else {
				this._setStatus(Status.READY_FOR_CHARGING);
			}

			// Session energy
			int sessionEnergy = lp.has("sessionEnergy") ? lp.get("sessionEnergy").getAsInt() : 0;
			this.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_SESSION_ENERGY).setNextValue(sessionEnergy);
			this._setEnergySession(sessionEnergy);

			// Store charger's native energy meter reading (for informational purposes)
			if (lp.has("chargeTotalImport") && !lp.get("chargeTotalImport").isJsonNull()) {
				long chargeTotalImport = Math.round(lp.get("chargeTotalImport").getAsDouble() * 1000.0);
				this.channel(LoadpointConsumptionMeterEvcc.ChannelId.CHARGE_TOTAL_IMPORT).setNextValue(chargeTotalImport);
			} else {
				this.channel(LoadpointConsumptionMeterEvcc.ChannelId.CHARGE_TOTAL_IMPORT).setNextValue(null);
			}

			// Note: ACTIVE_PRODUCTION_ENERGY is now always calculated from power via CalculateEnergyFromPower
			// in AbstractLoadpointMeterEvcc.handleEvent()

			// Vehicle info
			if (lp.has("vehicleSoc") && !lp.get("vehicleSoc").isJsonNull()) {
				this._setSoc((int) Math.round(lp.get("vehicleSoc").getAsDouble()));
			} else {
				this._setSoc(null);
			}

			if (lp.has("vehicleName") && !lp.get("vehicleName").isJsonNull()) {
				this.channel(LoadpointConsumptionMeterEvcc.ChannelId.VEHICLE_NAME)
						.setNextValue(lp.get("vehicleName").getAsString());
			} else {
				this.channel(LoadpointConsumptionMeterEvcc.ChannelId.VEHICLE_NAME).setNextValue(null);
			}

			if (lp.has("mode") && !lp.get("mode").isJsonNull()) {
				this.channel(LoadpointConsumptionMeterEvcc.ChannelId.MODE)
						.setNextValue(lp.get("mode").getAsString());
			}

			if (lp.has("enabled")) {
				this.channel(LoadpointConsumptionMeterEvcc.ChannelId.ENABLED)
						.setNextValue(lp.get("enabled").getAsBoolean());
			}

			// Voltages
			this.processVoltages(lp, phases);

			// Currents
			this.processCurrents(lp, phases, chargePower);

			// Per-phase power from V×I
			this.calculatePhasePower(phases);

		} catch (OpenemsNamedException e) {
			this.log.warn("Failed to parse evcc loadpoint data: {}", e.getMessage());
		}
	}

	private void processVoltages(com.google.gson.JsonObject lp, int phases) {
		if (lp.has("chargeVoltages") && !lp.get("chargeVoltages").isJsonNull()
				&& lp.get("chargeVoltages").isJsonArray()) {
			var voltages = lp.getAsJsonArray("chargeVoltages");

			if (voltages.size() > 0 && voltages.get(0) != null && !voltages.get(0).isJsonNull()) {
				this._setVoltageL1((int) Math.round(voltages.get(0).getAsDouble() * 1000));
			} else {
				this._setVoltageL1(null);
			}

			if (voltages.size() > 1 && voltages.get(1) != null && !voltages.get(1).isJsonNull()) {
				this._setVoltageL2((int) Math.round(voltages.get(1).getAsDouble() * 1000));
			} else {
				this._setVoltageL2(null);
			}

			if (voltages.size() > 2 && voltages.get(2) != null && !voltages.get(2).isJsonNull()) {
				this._setVoltageL3((int) Math.round(voltages.get(2).getAsDouble() * 1000));
			} else {
				this._setVoltageL3(null);
			}
		} else {
			int voltage = 230;
			this._setVoltageL1(TypeUtils.multiply(voltage, 1000));
			this._setVoltageL2(TypeUtils.multiply(voltage, 1000));
			this._setVoltageL3(TypeUtils.multiply(voltage, 1000));
		}
	}

	private void processCurrents(com.google.gson.JsonObject lp, int phases, int chargePower) {
		// Use getNextValue() since voltages were just set via setNextValue()
		var voltageL1 = (Integer) this.channel(ElectricityMeter.ChannelId.VOLTAGE_L1).getNextValue().get();
		var voltageL2 = (Integer) this.channel(ElectricityMeter.ChannelId.VOLTAGE_L2).getNextValue().get();
		var voltageL3 = (Integer) this.channel(ElectricityMeter.ChannelId.VOLTAGE_L3).getNextValue().get();
		double powerPerPhase = phases > 0 ? (double) chargePower / phases : 0;

		if (lp.has("chargeCurrents") && !lp.get("chargeCurrents").isJsonNull()
				&& lp.get("chargeCurrents").isJsonArray()) {
			var currents = lp.getAsJsonArray("chargeCurrents");

			if (currents.size() > 0 && currents.get(0) != null && !currents.get(0).isJsonNull()) {
				this._setCurrentL1((int) Math.round(currents.get(0).getAsDouble() * 1000));
			} else {
				this._setCurrentL1(null);
			}
			if (currents.size() > 1 && currents.get(1) != null && !currents.get(1).isJsonNull()) {
				this._setCurrentL2((int) Math.round(currents.get(1).getAsDouble() * 1000));
			} else {
				this._setCurrentL2(null);
			}
			if (currents.size() > 2 && currents.get(2) != null && !currents.get(2).isJsonNull()) {
				this._setCurrentL3((int) Math.round(currents.get(2).getAsDouble() * 1000));
			} else {
				this._setCurrentL3(null);
			}
		} else {
			// Estimate currents from power
			if (phases > 0 && voltageL1 != null) {
				this._setCurrentL1((int) (powerPerPhase * 1000000 / voltageL1));
			} else {
				this._setCurrentL1(null);
			}

			if (phases > 1 && voltageL2 != null) {
				this._setCurrentL2((int) (powerPerPhase * 1000000 / voltageL2));
			} else {
				this._setCurrentL2(null);
			}

			if (phases > 2 && voltageL3 != null) {
				this._setCurrentL3((int) (powerPerPhase * 1000000 / voltageL3));
			} else {
				this._setCurrentL3(null);
			}
		}
	}

	private void calculatePhasePower(int phases) {
		// Use getNextValue() since voltages and currents were just set via setNextValue()
		var voltageL1 = this.channel(ElectricityMeter.ChannelId.VOLTAGE_L1).getNextValue().get();
		var voltageL2 = this.channel(ElectricityMeter.ChannelId.VOLTAGE_L2).getNextValue().get();
		var voltageL3 = this.channel(ElectricityMeter.ChannelId.VOLTAGE_L3).getNextValue().get();
		var currentL1 = this.channel(ElectricityMeter.ChannelId.CURRENT_L1).getNextValue().get();
		var currentL2 = this.channel(ElectricityMeter.ChannelId.CURRENT_L2).getNextValue().get();
		var currentL3 = this.channel(ElectricityMeter.ChannelId.CURRENT_L3).getNextValue().get();

		if (phases > 0 && voltageL1 != null && currentL1 != null) {
			this._setActivePowerL1((int) ((long) (Integer) voltageL1 * (Integer) currentL1 / 1000000));
		} else {
			this._setActivePowerL1(null);
		}

		if (phases > 1 && voltageL2 != null && currentL2 != null) {
			this._setActivePowerL2((int) ((long) (Integer) voltageL2 * (Integer) currentL2 / 1000000));
		} else {
			this._setActivePowerL2(null);
		}

		if (phases > 2 && voltageL3 != null && currentL3 != null) {
			this._setActivePowerL3((int) ((long) (Integer) voltageL3 * (Integer) currentL3 / 1000000));
		} else {
			this._setActivePowerL3(null);
		}
	}

	@Override
	protected Logger getLogger() {
		return this.log;
	}

	@Override
	protected Integer getActivePowerValue() {
		return this.getActivePower().get();
	}

	@Override
	protected Integer getActivePowerL1Value() {
		return this.getActivePowerL1().get();
	}

	@Override
	protected Integer getActivePowerL2Value() {
		return this.getActivePowerL2().get();
	}

	@Override
	protected Integer getActivePowerL3Value() {
		return this.getActivePowerL3().get();
	}

	@Override
	protected BridgeHttpFactory getHttpBridgeFactory() {
		return this.httpBridgeFactory;
	}

	@Override
	protected HttpBridgeCycleServiceDefinition getHttpBridgeCycleServiceDefinition() {
		return this.httpBridgeCycleServiceDefinition;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}
}
