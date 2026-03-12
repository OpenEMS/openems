package io.openems.edge.io.shelly.shellypro3em;

import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.getAsOptionalJsonArray;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static java.lang.Math.round;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.HashSet;
import java.util.Set;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.mdns.MDnsDiscovery;
import io.openems.edge.io.shelly.common.HttpBridgeShellyService;
import io.openems.edge.io.shelly.common.gen2.IoGen2ShellyBase;
import io.openems.edge.io.shelly.common.gen2.IoGen2ShellyBaseImpl;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Shelly.Pro3EM", //
		immediate = true, //
		configurationPolicy = REQUIRE)
@EventTopics({ //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class IoShellyPro3EmImpl extends IoGen2ShellyBaseImpl
		implements IoShellyPro3Em, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	private final Logger log = LoggerFactory.getLogger(IoShellyPro3EmImpl.class);

	private MeterType meterType = null;
	private boolean shouldInvert = false;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata;

	@Reference
	private MDnsDiscovery mDnsDiscovery;
	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	@Reference
	private HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition;
	@Reference
	private HttpBridgeShellyService.HttpBridgeShellyServiceDefinition httpBridgeShellyServiceDefinition;

	public IoShellyPro3EmImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				IoGen2ShellyBase.ChannelId.values(), //
				ErrorChannelId.values(), //
				IoShellyPro3Em.ChannelId.values() //
		);

		ElectricityMeter.calculateSumActivePowerFromPhases(this);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	public String[] getSupportedShellyDeviceTypes() {
		return new String[] { "Pro3EM" };
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		this.meterType = config.type();
		this.shouldInvert = config.invert();
		super.activate(context, config.id(), config.alias(), config.enabled(), config.ip(), config.mdnsName(),
				config.debugMode(), config.validateDevice());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString() //
				+ (this.metricService != null ? ", " + this.metricService : "");
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
			-> this.calculateEnergy();
		}
	}

	@Override
	protected void subscribeDataCalls() {
		this.cycleService.subscribeJsonEveryCycle(this.baseUrl + "/rpc/EM.GetStatus?id=0", this::processHttpResult);

	}

	private int invert(int value) {
		return this.shouldInvert ? value * -1 : value;
	}

	private void processHttpResult(HttpResponse<JsonElement> result, HttpError error) {
		this._setSlaveCommunicationFailed(result == null);

		Integer activePower = null;
		Integer activePowerL1 = null;
		Integer activePowerL2 = null;
		Integer activePowerL3 = null;
		Integer apparentPower = null;
		Integer apparentPowerL1 = null;
		Integer apparentPowerL2 = null;
		Integer apparentPowerL3 = null;
		Integer voltageL1 = null;
		Integer voltageL2 = null;
		Integer voltageL3 = null;
		Integer currentL1 = null;
		Integer currentL2 = null;
		Integer currentL3 = null;

		if (error != null) {
			this.logWarn(this.log, error.getMessage());
		} else {
			try {
				var response = getAsJsonObject(result.data());
				final var errors = new HashSet<ErrorChannelId>();

				// Check for 'errors' and process if present
				getAsOptionalJsonArray(response, "errors").ifPresent(x -> this.parseShellyErrors(x, errors));

				// Total Active Power
				activePower = this.invert(round(getAsFloat(response, "total_act_power")));
				apparentPower = round(getAsFloat(response, "total_aprt_power"));

				// Extract phase data
				activePowerL1 = this.invert(round(getAsFloat(response, "a_act_power")));
				apparentPowerL1 = round(getAsFloat(response, "a_aprt_power"));
				voltageL1 = round(getAsFloat(response, "a_voltage") * 1000);
				currentL1 = this.invert(round(getAsFloat(response, "a_current") * 1000));
				getAsOptionalJsonArray(response, "a_errors")
						.ifPresent(x -> this.parseShellyErrors(x, "a_errors(%s)", errors));

				activePowerL2 = this.invert(round(getAsFloat(response, "b_act_power")));
				apparentPowerL2 = round(getAsFloat(response, "b_aprt_power"));
				voltageL2 = round(getAsFloat(response, "b_voltage") * 1000);
				currentL2 = this.invert(round(getAsFloat(response, "b_current") * 1000));
				getAsOptionalJsonArray(response, "b_errors")
						.ifPresent(x -> this.parseShellyErrors(x, "b_errors(%s)", errors));

				activePowerL3 = this.invert(round(getAsFloat(response, "c_act_power")));
				apparentPowerL3 = round(getAsFloat(response, "c_aprt_power"));
				voltageL3 = round(getAsFloat(response, "c_voltage") * 1000);
				currentL3 = this.invert(round(getAsFloat(response, "c_current") * 1000));
				getAsOptionalJsonArray(response, "c_errors")
						.ifPresent(x -> this.parseShellyErrors(x, "c_errors(%s)", errors));

				this.setShellyErrors(errors);
			} catch (OpenemsNamedException e) {
				this.logWarn(this.log, "Error processing HTTP response from shelly: " + e.getMessage());
				this._setSlaveCommunicationFailed(true);
			}
		}

		this._setActivePower(activePower);
		setValue(this, IoShellyPro3Em.ChannelId.APPARENT_POWER, apparentPower);

		this._setActivePowerL1(activePowerL1);
		setValue(this, IoShellyPro3Em.ChannelId.APPARENT_POWER_L1, apparentPowerL1);
		this._setVoltageL1(voltageL1);
		this._setCurrentL1(currentL1);

		this._setActivePowerL2(activePowerL2);
		setValue(this, IoShellyPro3Em.ChannelId.APPARENT_POWER_L2, apparentPowerL2);
		this._setVoltageL2(voltageL2);
		this._setCurrentL2(currentL2);

		this._setActivePowerL3(activePowerL3);
		setValue(this, IoShellyPro3Em.ChannelId.APPARENT_POWER_L3, apparentPowerL3);
		this._setVoltageL3(voltageL3);
		this._setCurrentL3(currentL3);
	}

	private void parseShellyErrors(JsonArray jsonErrors, Set<ErrorChannelId> resultList) {
		this.parseShellyErrors(jsonErrors, "%s", resultList);
	}

	private void parseShellyErrors(JsonArray jsonErrors, String errorKeyFormat, Set<ErrorChannelId> resultList) {
		for (var jsonError : jsonErrors) {
			try {
				var shellyErrorCode = errorKeyFormat.formatted(getAsString(jsonError));
				var errorChannel = ErrorChannelId.byShellyErrorCode(shellyErrorCode);
				if (errorChannel.isEmpty()) {
					this.log.warn("Can't find shelly error code '{}'", shellyErrorCode);
					continue;
				}

				resultList.add(errorChannel.get());
			} catch (Exception ex) {
				this.logWarn(this.log, "Error while parsing shelly error code (" + jsonError + "): " + ex.getMessage());
			}
		}
	}

	private void setShellyErrors(Set<ErrorChannelId> channelsWithError) {
		for (var errorChannel : ErrorChannelId.valuesWithShellyErrorCode()) {
			setValue(this, errorChannel, channelsWithError.contains(errorChannel));
		}
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

	@Override
	protected BridgeHttpFactory getHttpBridgeFactory() {
		return this.httpBridgeFactory;
	}

	@Override
	protected HttpBridgeCycleServiceDefinition getHttpBridgeCycleServiceDefinition() {
		return this.httpBridgeCycleServiceDefinition;
	}

	@Override
	protected HttpBridgeShellyService.HttpBridgeShellyServiceDefinition getHttpBridgeShellyServiceDefinition() {
		return this.httpBridgeShellyServiceDefinition;
	}

	@Override
	protected MDnsDiscovery getMDnsDiscovery() {
		return this.mDnsDiscovery;
	}
}
