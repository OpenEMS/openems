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
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
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
public class LoadpointConsumptionMeterEvccImpl extends AbstractOpenemsComponent
		implements LoadpointConsumptionMeterEvcc, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	private MeterType meterType;

	public LoadpointConsumptionMeterEvccImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				LoadpointConsumptionMeterEvcc.ChannelId.values() //
		);

		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.meterType = config.type();
		if (this.isEnabled() && this.httpBridgeFactory != null) {
			this.httpBridge = this.httpBridgeFactory.get();
			var url = config.apiUrl() + "?jq=.loadpoints[" + config.loadpointIndex() + "]";
			this.httpBridge.subscribeJsonEveryCycle(url, this::processHttpResult);
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
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> this.calculateEnergy();
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

			// Gesamtleistung
			if (lp.has("chargePower")) {
				chargePower = (int) Math.round(getAsDouble(lp, "chargePower"));
				this.channel(LoadpointConsumptionMeterEvcc.ChannelId.CONSUMPTION_POWER).setNextValue(chargePower);
				this._setActivePower(chargePower);
			} else {
				this._setActivePower(null);
			}

			// Aktive Phasen
			int phases = lp.has("phasesActive") ? lp.get("phasesActive").getAsInt() : 0;
			this.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_PHASES).setNextValue(phases);
			double calculatedPower = chargePower;
			if (phases > 1) {
				calculatedPower = chargePower / phases;
			}

			// konsumierte Energie (Gesamt)
			int totalImport = lp.has("chargeTotalImport") ? lp.get("chargeTotalImport").getAsInt() : 0;
			this.channel(LoadpointConsumptionMeterEvcc.ChannelId.CONSUMPTION_ENERGY).setNextValue(totalImport);

			// konsumierte Energie (Sitzung)
			int sessionEnergy = lp.has("sessionEnergy") ? lp.get("sessionEnergy").getAsInt() : 0;
			this.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_SESSION_ENERGY).setNextValue(sessionEnergy);

			// Spannungen pro Phase setzen
			if (lp.has("chargeVoltages") && lp.get("chargeVoltages").isJsonArray()) {
				var voltages = lp.getAsJsonArray("chargeVoltages");

				if (voltages.size() > 0 && voltages.get(0) != null && !voltages.get(0).isJsonNull()) {
					double v1 = voltages.get(0).getAsDouble();
					this.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_VOLTAGE_L1).setNextValue(v1);
					this._setVoltageL1((int) Math.round(v1 * 1000));
				} else {
					this.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_VOLTAGE_L1).setNextValue(null);
				}

				if (voltages.size() > 0 && voltages.get(1) != null && !voltages.get(1).isJsonNull()) {
					double v2 = voltages.get(1).getAsDouble();
					this.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_VOLTAGE_L2).setNextValue(v2);
					this._setVoltageL2((int) Math.round(v2 * 1000));
				} else {
					this.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_VOLTAGE_L2).setNextValue(null);
				}

				if (voltages.size() > 0 && voltages.get(2) != null && !voltages.get(2).isJsonNull()) {
					double v3 = voltages.get(2).getAsDouble();
					this.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_VOLTAGE_L3).setNextValue(v3);
					this._setVoltageL3((int) Math.round(v3 * 1000));
				} else {
					this.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_VOLTAGE_L3).setNextValue(null);
				}
			} else {
				this.logDebug(this.log, "chargeVoltages not provided or null – defaulting voltages");

				// Spannung in mV setzen
				var voltage = 230;
				this._setVoltageL1(voltage * 1000);
				this._setVoltageL2(voltage * 1000);
				this._setVoltageL3(voltage * 1000);
			}

			// Stromstärken pro Phase setzen (übernommen oder berechnet)
			if (lp.has("chargeCurrents") && lp.get("chargeCurrents").isJsonArray()) {
				var currents = lp.getAsJsonArray("chargeCurrents");

				if (currents.size() > 0 && currents.get(0) != null && !currents.get(0).isJsonNull()) {
					double i1 = currents.get(0).getAsDouble();
					this.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_CURRENT_L1).setNextValue(i1);
					this._setCurrentL1((int) i1 * 1000);
				}
				if (currents.size() > 0 && currents.get(1) != null && !currents.get(1).isJsonNull()) {
					double i2 = currents.get(1).getAsDouble();
					this.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_CURRENT_L2).setNextValue(i2);
					this._setCurrentL2((int) i2 * 1000);
				}
				if (currents.size() > 0 && currents.get(2) != null && !currents.get(2).isJsonNull()) {
					double i3 = currents.get(2).getAsDouble();
					this.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_CURRENT_L3).setNextValue(i3);
					this._setCurrentL3((int) i3 * 1000);
				}
			} else {
				this.logDebug(this.log, "chargeCurrents not provided or null – estimating phase current mapping.");

				this.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_CURRENT_L1).setNextValue(null);
				this.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_CURRENT_L2).setNextValue(null);
				this.channel(LoadpointConsumptionMeterEvcc.ChannelId.ACTIVE_CURRENT_L3).setNextValue(null);

				// Stromstärke in mA setzen
				if (phases > 0) {
					this._setCurrentL1((int) (calculatedPower * 1000000 / this.getVoltageL1().get()));
				} else {
					this._setCurrentL1(null);
				}

				if (phases > 1) {
					this._setCurrentL2((int) (calculatedPower * 1000000 / this.getVoltageL2().get()));
				} else {
					this._setCurrentL2(null);
				}

				if (phases > 2) {
					this._setCurrentL3((int) (calculatedPower * 1000000 / this.getVoltageL3().get()));
				} else {
					this._setCurrentL3(null);
				}
			}

			// Leistung in W je Phase setzen (berechnet)
			if (phases > 0 && this.getVoltageL1() != null && this.getCurrentL1() != null) {
				this._setActivePowerL1((this.getVoltageL1().get() * this.getCurrentL1().get() / 1000000));
			} else {
				this._setActivePowerL1(null);
			}

			if (phases > 1 && this.getVoltageL2() != null && this.getCurrentL2() != null) {
				this._setActivePowerL2((this.getVoltageL2().get() * this.getCurrentL2().get() / 1000000));
			} else {
				this._setActivePowerL2(null);
			}

			if (phases > 2 && this.getVoltageL3() != null && this.getCurrentL3() != null) {
				this._setActivePowerL3((this.getVoltageL3().get() * this.getCurrentL3().get() / 1000000));
			} else {
				this._setActivePowerL3(null);
			}

		} catch (OpenemsNamedException e) {
			this.log.warn("Failed to parse evcc loadpoint data: {}", e.getMessage());
		}
	}

	private void calculateEnergy() {
		// Calculate Energy
		final var activePower = this.getActivePower().get();
		if (activePower == null) {
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower <= 0) {
			this.calculateProductionEnergy.update(activePower);
			this.calculateConsumptionEnergy.update(0);
		} else {
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(activePower);
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
