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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.MeterType;
import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleService;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.evcc.loadpoint.LoadpointConsumptionMeterEvcc;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Consumption.SinglePhaseLoadpoint.Evcc", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { "type=CONSUMPTION_METERED" } //
)
@EventTopics(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class LoadpointConsumptionSinglePhaseMeterEvccImpl extends AbstractOpenemsComponent
		implements LoadpointConsumptionSinglePhaseMeterEvcc, SinglePhaseMeter, ElectricityMeter, OpenemsComponent,
		TimedataProvider, EventHandler {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	@Reference
	private HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition;
	private BridgeHttp httpBridge;
	private HttpBridgeCycleService cycleService;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	private MeterType meterType;

	private SinglePhase phase;

	public LoadpointConsumptionSinglePhaseMeterEvccImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				LoadpointConsumptionSinglePhaseMeterEvcc.ChannelId.values() //
		);

		SinglePhaseMeter.calculateSinglePhaseFromActivePower(this);
		SinglePhaseMeter.calculateSinglePhaseFromCurrent(this);
		SinglePhaseMeter.calculateSinglePhaseFromVoltage(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.meterType = config.type();
		this.phase = config.phase();
		if (this.isEnabled() && this.httpBridgeFactory != null) {
			this.httpBridge = this.httpBridgeFactory.get();
			this.cycleService = this.httpBridge.createService(this.httpBridgeCycleServiceDefinition);
			var url = config.apiUrl() + "?jq=.loadpoints[" + config.loadpointIndex() + "]";
			this.cycleService.subscribeJsonEveryCycle(url, this::processHttpResult);
		}

	}

	@Override
	@Deactivate
	protected void deactivate() {
		if (this.httpBridge != null) {
			this.httpBridgeFactory.unget(this.httpBridge);
			this.httpBridge = null;
		}
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateEnergy();
			break;
		}
	}

	private void processHttpResult(HttpResponse<JsonElement> result, HttpError error) {
		if (error != null) {
			this.logDebug(this.log, error.getMessage());
			return;
		}

		try {
			this.logDebug(this.log, "processHttpResult");
			var lp = getAsJsonObject(result.data());

			int chargePower = 0;
			if (lp.has("chargePower")) {
				chargePower = (int) Math.round(getAsDouble(lp, "chargePower"));
				this._setActivePower(chargePower);
			} else {
				this._setActivePower(null);
			}

			int phases = lp.has("phasesActive") ? lp.get("phasesActive").getAsInt() : 0;
			this.channel(LoadpointConsumptionSinglePhaseMeterEvcc.ChannelId.ACTIVE_PHASES).setNextValue(phases);

			// Use double to maintain precision when dividing power across phases
			double calculatedPower = chargePower;
			if (phases > 1) {
				calculatedPower = (double) chargePower / phases;
			}

			int totalImport = lp.has("chargeTotalImport") ? lp.get("chargeTotalImport").getAsInt() : 0;
			this.channel(LoadpointConsumptionSinglePhaseMeterEvcc.ChannelId.CONSUMPTION_ENERGY)
					.setNextValue(totalImport);

			int sessionEnergy = lp.has("sessionEnergy") ? lp.get("sessionEnergy").getAsInt() : 0;
			this.channel(LoadpointConsumptionSinglePhaseMeterEvcc.ChannelId.ACTIVE_SESSION_ENERGY)
					.setNextValue(sessionEnergy);

			if (lp.has("chargeVoltages") && lp.get("chargeVoltages").isJsonArray()) {
				var voltages = lp.getAsJsonArray("chargeVoltages");

				if (voltages.size() > 0 && voltages.get(0) != null && !voltages.get(0).isJsonNull()) {
					double v1 = voltages.get(0).getAsDouble();
					this._setVoltage((int) Math.round(v1 * 1000));
				} else if (voltages.size() > 1 && voltages.get(1) != null && !voltages.get(1).isJsonNull()) {
					double v2 = voltages.get(1).getAsDouble();
					this._setVoltage((int) Math.round(v2 * 1000));
				} else if (voltages.size() > 2 && voltages.get(2) != null && !voltages.get(2).isJsonNull()) {
					double v3 = voltages.get(2).getAsDouble();
					this._setVoltage((int) Math.round(v3 * 1000));
				} else {
					this._setVoltage(null);
				}
			} else {
				this.logDebug(this.log, "chargeVoltages not provided or null – defaulting voltages");
				this._setVoltage(230 * 1000);
			}

			if (lp.has("chargeCurrents") && lp.get("chargeCurrents").isJsonArray()) {
				var currents = lp.getAsJsonArray("chargeCurrents");

				if (currents.size() > 0 && currents.get(0) != null && !currents.get(0).isJsonNull()) {
					double i1 = currents.get(0).getAsDouble();
					this._setCurrent((int) Math.round(i1 * 1000));
				} else if (currents.size() > 1 && currents.get(1) != null && !currents.get(1).isJsonNull()) {
					double i2 = currents.get(1).getAsDouble();
					this._setCurrent((int) Math.round(i2 * 1000));
				} else if (currents.size() > 2 && currents.get(2) != null && !currents.get(2).isJsonNull()) {
					double i3 = currents.get(2).getAsDouble();
					this._setCurrent((int) Math.round(i3 * 1000));
				} else {
					this._setCurrent(null);
				}
			} else {
				this.logDebug(this.log, "chargeCurrents not provided or null – estimating phase current mapping.");

				if (phases > 0) {
					this._setCurrent((int) (calculatedPower * 1000000 / this.getVoltage().get()));
				} else {
					this._setCurrent(null);
				}
			}

		} catch (OpenemsNamedException e) {
			this.log.warn("Failed to parse evcc loadpoint data: {}", e.getMessage());
		}
	}

	private void calculateEnergy() {
		final var activePower = this.getActivePower().get();
		if (activePower == null) {
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower > 0) {
			this.calculateProductionEnergy.update(Math.abs(activePower));
			this.calculateConsumptionEnergy.update(0);
		} else {
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(Math.abs(activePower));
		}
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
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public SinglePhase getPhase() {
		return this.phase;
	}
}
