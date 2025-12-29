package io.openems.edge.evcc.loadpoint.single;

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
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.evcc.loadpoint.AbstractLoadpointMeterEvcc;
import io.openems.edge.evcc.loadpoint.PlugState;
import io.openems.edge.evcs.api.DeprecatedEvcs;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.SocEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Consumption.SinglePhaseLoadpoint.Evcc", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { "type=CONSUMPTION_METERED" } //
)
@EventTopics(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class LoadpointConsumptionSinglePhaseMeterEvccImpl extends AbstractLoadpointMeterEvcc
		implements LoadpointConsumptionSinglePhaseMeterEvcc, SocEvcs, Evcs, DeprecatedEvcs, SinglePhaseMeter, ElectricityMeter, OpenemsComponent,
		TimedataProvider {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Reference
	private BridgeHttpFactory httpBridgeFactory;

	@Reference
	private HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	private MeterType meterType;
	private SinglePhase phase;

	public LoadpointConsumptionSinglePhaseMeterEvccImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				SocEvcs.ChannelId.values(), //
				DeprecatedEvcs.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				LoadpointConsumptionSinglePhaseMeterEvcc.ChannelId.values() //
		);

		// Copy production energy to consumption energy (Keba pattern)
		DeprecatedEvcs.copyToDeprecatedEvcsChannels(this);

		SinglePhaseMeter.calculateSinglePhaseFromActivePower(this);
		SinglePhaseMeter.calculateSinglePhaseFromCurrent(this);
		SinglePhaseMeter.calculateSinglePhaseFromVoltage(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.meterType = config.type();
		this.phase = config.phase();

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
			this.channel(LoadpointConsumptionSinglePhaseMeterEvcc.ChannelId.ACTIVE_PHASES).setNextValue(phases);
			this._setPhases(phases > 0 ? phases : 1);

			// Plug state and status
			boolean connected = lp.has("connected") && lp.get("connected").getAsBoolean();
			boolean charging = lp.has("charging") && lp.get("charging").getAsBoolean();
			this.channel(LoadpointConsumptionSinglePhaseMeterEvcc.ChannelId.PLUG)
					.setNextValue(connected ? PlugState.CONNECTED : PlugState.UNPLUGGED);
			if (!connected) {
				this._setStatus(Status.NOT_READY_FOR_CHARGING);
			} else if (charging) {
				this._setStatus(Status.CHARGING);
			} else {
				this._setStatus(Status.READY_FOR_CHARGING);
			}

			// Store charger's native energy meter reading (for informational purposes)
			if (lp.has("chargeTotalImport") && !lp.get("chargeTotalImport").isJsonNull()) {
				long chargeTotalImport = Math.round(lp.get("chargeTotalImport").getAsDouble() * 1000.0);
				this.channel(LoadpointConsumptionSinglePhaseMeterEvcc.ChannelId.CHARGE_TOTAL_IMPORT).setNextValue(chargeTotalImport);
			} else {
				this.channel(LoadpointConsumptionSinglePhaseMeterEvcc.ChannelId.CHARGE_TOTAL_IMPORT).setNextValue(null);
			}

			// Note: ACTIVE_PRODUCTION_ENERGY is now always calculated from power via CalculateEnergyFromPower
			// in AbstractLoadpointMeterEvcc.handleEvent()

			// Session energy
			int sessionEnergy = lp.has("sessionEnergy") ? lp.get("sessionEnergy").getAsInt() : 0;
			this.channel(LoadpointConsumptionSinglePhaseMeterEvcc.ChannelId.ACTIVE_SESSION_ENERGY)
					.setNextValue(sessionEnergy);
			this._setEnergySession(sessionEnergy);

			// Vehicle info
			if (lp.has("vehicleSoc") && !lp.get("vehicleSoc").isJsonNull()) {
				this._setSoc((int) Math.round(lp.get("vehicleSoc").getAsDouble()));
			} else {
				this._setSoc(null);
			}

			if (lp.has("vehicleName") && !lp.get("vehicleName").isJsonNull()) {
				this.channel(LoadpointConsumptionSinglePhaseMeterEvcc.ChannelId.VEHICLE_NAME)
						.setNextValue(lp.get("vehicleName").getAsString());
			} else {
				this.channel(LoadpointConsumptionSinglePhaseMeterEvcc.ChannelId.VEHICLE_NAME).setNextValue(null);
			}

			if (lp.has("mode") && !lp.get("mode").isJsonNull()) {
				this.channel(LoadpointConsumptionSinglePhaseMeterEvcc.ChannelId.MODE)
						.setNextValue(lp.get("mode").getAsString());
			}

			if (lp.has("enabled")) {
				this.channel(LoadpointConsumptionSinglePhaseMeterEvcc.ChannelId.ENABLED)
						.setNextValue(lp.get("enabled").getAsBoolean());
			}

			// Voltage (single phase - use first available)
			this.processVoltage(lp);

			// Current (single phase - use first available or estimate)
			this.processCurrent(lp, phases, chargePower);

		} catch (OpenemsNamedException e) {
			this.log.warn("Failed to parse evcc loadpoint data: {}", e.getMessage());
		}
	}

	private void processVoltage(com.google.gson.JsonObject lp) {
		if (lp.has("chargeVoltages") && !lp.get("chargeVoltages").isJsonNull()
				&& lp.get("chargeVoltages").isJsonArray()) {
			var voltages = lp.getAsJsonArray("chargeVoltages");

			// Use first available voltage
			for (int i = 0; i < voltages.size(); i++) {
				if (voltages.get(i) != null && !voltages.get(i).isJsonNull()) {
					this._setVoltage((int) Math.round(voltages.get(i).getAsDouble() * 1000));
					return;
				}
			}
			this._setVoltage(null);
		} else {
			this._setVoltage(230 * 1000);
		}
	}

	private void processCurrent(com.google.gson.JsonObject lp, int phases, int chargePower) {
		if (lp.has("chargeCurrents") && !lp.get("chargeCurrents").isJsonNull()
				&& lp.get("chargeCurrents").isJsonArray()) {
			var currents = lp.getAsJsonArray("chargeCurrents");

			// Use first available current
			for (int i = 0; i < currents.size(); i++) {
				if (currents.get(i) != null && !currents.get(i).isJsonNull()) {
					this._setCurrent((int) Math.round(currents.get(i).getAsDouble() * 1000));
					return;
				}
			}
			this._setCurrent(null);
		} else {
			// Estimate current from power - use getNextValue() since voltage was just set
			var voltage = this.channel(ElectricityMeter.ChannelId.VOLTAGE).getNextValue().get();
			double calculatedPower = phases > 1 ? (double) chargePower / phases : chargePower;
			if (phases > 0 && voltage != null) {
				this._setCurrent((int) (calculatedPower * 1000000 / (Integer) voltage));
			} else {
				this._setCurrent(null);
			}
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

	@Override
	public SinglePhase getPhase() {
		return this.phase;
	}
}
