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
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Consumption.Loadpoint.Evcc", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { "type=CONSUMPTION_METERED" } //
)
@EventTopics(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class LoadpointConsumptionMeterEvccImpl extends AbstractLoadpointMeterEvcc
		implements LoadpointConsumptionMeterEvcc, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	@Reference
	private HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition;
	private BridgeHttp httpBridge;
	private HttpBridgeCycleService cycleService;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	/**
	 * Energy calculators for both positive and negative power values.
	 *
	 * <p>
	 * EVCC loadpoint is a consumption meter that typically reports positive
	 * ActivePower values (consumption). According to ElectricityMeter API:
	 * <ul>
	 * <li>ACTIVE_PRODUCTION_ENERGY = integral over positive ACTIVE_POWER values
	 * <li>ACTIVE_CONSUMPTION_ENERGY = integral over negative ACTIVE_POWER values
	 * </ul>
	 * Both are provided for completeness, though for a wallbox/charging station,
	 * ACTIVE_PRODUCTION_ENERGY is the primary channel as it accumulates positive
	 * power (consumption). This ensures compatibility with UI history charts which
	 * expect consumption meters to use ActiveProductionEnergy.
	 */
	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	/**
	 * Energy calculators for each phase (L1, L2, L3).
	 *
	 * <p>
	 * These calculators provide phase-specific energy values for phase-accurate
	 * history charts. Each phase has both production and consumption energy
	 * calculators to handle positive (consumption) and negative (production)
	 * power values respectively.
	 */
	private final CalculateEnergyFromPower calculateProductionEnergyL1 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1);
	private final CalculateEnergyFromPower calculateConsumptionEnergyL1 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1);
	private final CalculateEnergyFromPower calculateProductionEnergyL2 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2);
	private final CalculateEnergyFromPower calculateConsumptionEnergyL2 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2);
	private final CalculateEnergyFromPower calculateProductionEnergyL3 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3);
	private final CalculateEnergyFromPower calculateConsumptionEnergyL3 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3);

	private MeterType meterType;

	public LoadpointConsumptionMeterEvccImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				LoadpointConsumptionMeterEvcc.ChannelId.values() //
		);

		ElectricityMeter.calculateSumActivePowerFromPhases(this);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.meterType = config.type();
		this.initializeLoadpointReference(config.loadpointTitle(), config.loadpointIndex());

		if (this.isEnabled() && this.httpBridgeFactory != null) {
			this.httpBridge = this.httpBridgeFactory.get();
			this.cycleService = this.httpBridge.createService(this.httpBridgeCycleServiceDefinition);

			// Build JQ filter: try to match by title first, fallback to index
			var jqFilter = this.buildLoadpointFilter(config.loadpointTitle(), config.loadpointIndex());
			var url = config.apiUrl() + "?jq=" + jqFilter;
			this.logInfo(this.log, "Subscribing to loadpoint with filter: " + jqFilter);
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
			this.calculateEnergyPerPhase();
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

			// Check if we got the expected loadpoint and warn if fallback was used
			this.checkLoadpointMatch(lp, this.log);

			int chargePower = 0;
			if (lp.has("chargePower")) {
				chargePower = (int) Math.round(getAsDouble(lp, "chargePower"));
			}

			int phases = lp.has("phasesActive") ? lp.get("phasesActive").getAsInt() : 0;
			this.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_PHASES).setNextValue(phases);

			// Use double to maintain precision when dividing power across phases
			double calculatedPower = chargePower;
			if (phases > 1) {
				calculatedPower = (double) chargePower / phases;
			}

			// Nutzung des Integrators über die Leistung
			// Double totalImport = lp.has("chargeTotalImport") ?
			// lp.get("chargeTotalImport").getAsDouble() : null;
			//
			// if (totalImport != null) {
			// Long consumptionEnergyWh = Math.round(totalImport * 1000.0);
			// this._setActiveConsumptionEnergy(consumptionEnergyWh);
			// } else {
			// this._setActiveConsumptionEnergy((Long) null);
			// }

			int sessionEnergy = lp.has("sessionEnergy") ? lp.get("sessionEnergy").getAsInt() : 0;
			this.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_SESSION_ENERGY).setNextValue(sessionEnergy);

			if (lp.has("chargeVoltages") && lp.get("chargeVoltages").isJsonArray()) {
				var voltages = lp.getAsJsonArray("chargeVoltages");

				if (voltages.size() > 0 && voltages.get(0) != null && !voltages.get(0).isJsonNull()) {
					double v1 = voltages.get(0).getAsDouble();
					this._setVoltageL1((int) Math.round(v1 * 1000));
				} else {
					this._setVoltageL1(null);
				}

				if (voltages.size() > 1 && voltages.get(1) != null && !voltages.get(1).isJsonNull()) {
					double v2 = voltages.get(1).getAsDouble();
					this._setVoltageL2((int) Math.round(v2 * 1000));
				} else {
					this._setVoltageL2(null);
				}

				if (voltages.size() > 2 && voltages.get(2) != null && !voltages.get(2).isJsonNull()) {
					double v3 = voltages.get(2).getAsDouble();
					this._setVoltageL3((int) Math.round(v3 * 1000));
				} else {
					this._setVoltageL3(null);
				}
			} else {
				this.logDebug(this.log, "chargeVoltages not provided or null – defaulting voltages");

				int voltage = 230;
				this._setVoltageL1(TypeUtils.multiply(voltage, 1000));
				this._setVoltageL2(TypeUtils.multiply(voltage, 1000));
				this._setVoltageL3(TypeUtils.multiply(voltage, 1000));
			}

			if (lp.has("chargeCurrents") && lp.get("chargeCurrents").isJsonArray()) {
				var currents = lp.getAsJsonArray("chargeCurrents");

				if (currents.size() > 0 && currents.get(0) != null && !currents.get(0).isJsonNull()) {
					double i1 = currents.get(0).getAsDouble();
					this._setCurrentL1((int) Math.round(i1 * 1000));
				} else {
					this._setCurrentL1(null);
				}
				if (currents.size() > 1 && currents.get(1) != null && !currents.get(1).isJsonNull()) {
					double i2 = currents.get(1).getAsDouble();
					this._setCurrentL2((int) Math.round(i2 * 1000));
				} else {
					this._setCurrentL2(null);
				}
				if (currents.size() > 2 && currents.get(2) != null && !currents.get(2).isJsonNull()) {
					double i3 = currents.get(2).getAsDouble();
					this._setCurrentL3((int) Math.round(i3 * 1000));
				} else {
					this._setCurrentL3(null);
				}
			} else {
				this.logDebug(this.log, "chargeCurrents not provided or null – estimating phase current mapping.");

				if (phases > 0) {
					int currentL1 = (int) (calculatedPower * 1000000 / this.getVoltageL1().get());
					this._setCurrentL1(currentL1);
				} else {
					this._setCurrentL1(null);
				}

				if (phases > 1) {
					int currentL2 = (int) (calculatedPower * 1000000 / this.getVoltageL2().get());
					this._setCurrentL2(currentL2);
				} else {
					this._setCurrentL2(null);
				}

				if (phases > 2) {
					int currentL3 = (int) (calculatedPower * 1000000 / this.getVoltageL3().get());
					this._setCurrentL3(currentL3);
				} else {
					this._setCurrentL3(null);
				}
			}

			if (phases > 0 && this.getVoltageL1() != null && this.getCurrentL1() != null) {
				this._setActivePowerL1((int) ((long) this.getVoltageL1().get() * this.getCurrentL1().get() / 1000000));
			} else {
				this._setActivePowerL1(null);
			}

			if (phases > 1 && this.getVoltageL2() != null && this.getCurrentL2() != null) {
				this._setActivePowerL2((int) ((long) this.getVoltageL2().get() * this.getCurrentL2().get() / 1000000));
			} else {
				this._setActivePowerL2(null);
			}

			if (phases > 2 && this.getVoltageL3() != null && this.getCurrentL3() != null) {
				this._setActivePowerL3((int) ((long) this.getVoltageL3().get() * this.getCurrentL3().get() / 1000000));
			} else {
				this._setActivePowerL3(null);
			}

		} catch (OpenemsNamedException e) {
			this.log.warn("Failed to parse evcc loadpoint data: {}", e.getMessage());
		}
	}

	/**
	 * Calculate energy from power values, splitting positive and negative values.
	 *
	 * <p>
	 * Positive power values (consumption) are accumulated in ACTIVE_PRODUCTION_ENERGY,
	 * negative power values (production) are accumulated in ACTIVE_CONSUMPTION_ENERGY.
	 * For a wallbox/charging station, positive values are expected (consumption only),
	 * so ACTIVE_PRODUCTION_ENERGY is the primary channel used by UI history charts.
	 */
	private void calculateEnergy() {
		final var activePower = this.getActivePower().get();
		if (activePower == null) {
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower > 0) {
			// Positive power = consumption -> accumulate in ACTIVE_PRODUCTION_ENERGY
			this.calculateProductionEnergy.update(Math.abs(activePower));
			this.calculateConsumptionEnergy.update(0);
		} else {
			// Negative power = production -> accumulate in ACTIVE_CONSUMPTION_ENERGY
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(Math.abs(activePower));
		}
	}

	/**
	 * Calculate energy per phase from phase-specific power values.
	 *
	 * <p>
	 * This method calculates energy for each phase (L1, L2, L3) separately,
	 * enabling phase-accurate history charts in the UI. Positive power values
	 * are accumulated in ACTIVE_PRODUCTION_ENERGY_LX, negative values in
	 * ACTIVE_CONSUMPTION_ENERGY_LX.
	 */
	private void calculateEnergyPerPhase() {
		// L1
		final var activePowerL1 = this.getActivePowerL1().get();
		if (activePowerL1 == null) {
			this.calculateProductionEnergyL1.update(null);
			this.calculateConsumptionEnergyL1.update(null);
		} else if (activePowerL1 > 0) {
			this.calculateProductionEnergyL1.update(Math.abs(activePowerL1));
			this.calculateConsumptionEnergyL1.update(0);
		} else {
			this.calculateProductionEnergyL1.update(0);
			this.calculateConsumptionEnergyL1.update(Math.abs(activePowerL1));
		}

		// L2
		final var activePowerL2 = this.getActivePowerL2().get();
		if (activePowerL2 == null) {
			this.calculateProductionEnergyL2.update(null);
			this.calculateConsumptionEnergyL2.update(null);
		} else if (activePowerL2 > 0) {
			this.calculateProductionEnergyL2.update(Math.abs(activePowerL2));
			this.calculateConsumptionEnergyL2.update(0);
		} else {
			this.calculateProductionEnergyL2.update(0);
			this.calculateConsumptionEnergyL2.update(Math.abs(activePowerL2));
		}

		// L3
		final var activePowerL3 = this.getActivePowerL3().get();
		if (activePowerL3 == null) {
			this.calculateProductionEnergyL3.update(null);
			this.calculateConsumptionEnergyL3.update(null);
		} else if (activePowerL3 > 0) {
			this.calculateProductionEnergyL3.update(Math.abs(activePowerL3));
			this.calculateConsumptionEnergyL3.update(0);
		} else {
			this.calculateProductionEnergyL3.update(0);
			this.calculateConsumptionEnergyL3.update(Math.abs(activePowerL3));
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
}
