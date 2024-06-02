package io.openems.edge.io.shelly.shellypro3em;

import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static java.lang.Math.round;

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
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Shelly.Pro.3EM", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class IoShellyPro3EmImpl extends AbstractOpenemsComponent
		implements IoShellyPro3Em, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	private final Logger log = LoggerFactory.getLogger(IoShellyPro3EmImpl.class);

	private MeterType meterType = null;
	private String baseUrl;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	@Reference()
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	public IoShellyPro3EmImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				IoShellyPro3Em.ChannelId.values() //
		);

		ElectricityMeter.calculateSumActivePowerFromPhases(this);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.meterType = config.type();
		this.baseUrl = "http://" + config.ip();
		this.httpBridge = this.httpBridgeFactory.get();

		if (this.isEnabled()) {
			this.httpBridge.subscribeJsonEveryCycle(this.baseUrl + "/rpc/EM.GetStatus?id=0", this::processHttpResult);
		}
	}

	@Deactivate
	protected void deactivate() {
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
		super.deactivate();
	}

	@Override
	public String debugLog() {
		var b = new StringBuilder();
		b.append(this.getActivePowerChannel().value().asString());

		return b.toString();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
			-> this.calculateEnergy();
		}
	}

	private void processHttpResult(JsonElement result, Throwable error) {
		this._setSlaveCommunicationFailed(result == null);

		Integer activePower = null;
		Integer activePowerL1 = null;
		Integer activePowerL2 = null;
		Integer activePowerL3 = null;
		Integer voltageL1 = null;
		Integer voltageL2 = null;
		Integer voltageL3 = null;
		Integer currentL1 = null;
		Integer currentL2 = null;
		Integer currentL3 = null;
		boolean phaseSequenceError = false;
		boolean powerMeterFailure = false;
		boolean noLoadCondition = false;

		if (error != null) {
			this.logDebug(this.log, error.getMessage());
		} else {
			try {
				var response = getAsJsonObject(result);

				// Check for 'errors' and process if present
				if (response.has("errors") && response.get("errors").isJsonArray()) {

					var errors = response.getAsJsonArray("errors");

					for (JsonElement errorElement : errors) {
						var errorType = errorElement.getAsString();

						switch (errorType) {
						case "phase_sequence":
							phaseSequenceError = true;
							break;
						case "power_meter_failure":
							powerMeterFailure = true;
							break;
						case "no_load":
							noLoadCondition = true;
							break;
						}
					}
				} else {
					this.logDebug(this.log, "No errors reported.");
				}

				// Total Active Power
				activePower = round(getAsFloat(response, "total_act_power"));

				// Extract phase data
				activePowerL1 = round(getAsFloat(response, "a_act_power"));
				voltageL1 = round(getAsFloat(response, "a_voltage") * 1000);
				currentL1 = round(getAsFloat(response, "a_current") * 1000);

				activePowerL2 = round(getAsFloat(response, "b_act_power"));
				voltageL2 = round(getAsFloat(response, "b_voltage") * 1000);
				currentL2 = round(getAsFloat(response, "b_current") * 1000);

				activePowerL3 = round(getAsFloat(response, "c_act_power"));
				voltageL3 = round(getAsFloat(response, "c_voltage") * 1000);
				currentL3 = round(getAsFloat(response, "c_current") * 1000);

			} catch (OpenemsNamedException e) {
				this.logDebug(this.log, e.getMessage());
			}
		}

		this._setActivePower(activePower);
		this.channel(IoShellyPro3Em.ChannelId.PHASE_SEQUENCE_ERROR).setNextValue(phaseSequenceError);
		this.channel(IoShellyPro3Em.ChannelId.NO_LOAD).setNextValue(noLoadCondition);
		this.channel(IoShellyPro3Em.ChannelId.POWER_METER_FAILURE).setNextValue(powerMeterFailure);

		this._setActivePowerL1(activePowerL1);
		this._setVoltageL1(voltageL1);
		this._setCurrentL1(currentL1);

		this._setActivePowerL2(activePowerL2);
		this._setVoltageL2(voltageL2);
		this._setCurrentL2(currentL2);

		this._setActivePowerL3(activePowerL3);
		this._setVoltageL3(voltageL3);
		this._setCurrentL3(currentL3);
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {
		// Calculate Energy
		final var activePower = this.getActivePower().get();
		if (activePower == null) {
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower >= 0) {
			this.calculateProductionEnergy.update(activePower);
			this.calculateConsumptionEnergy.update(0);
		} else {
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(-activePower);
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
}
