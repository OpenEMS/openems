package io.openems.edge.io.shelly.shelly3em;

import static io.openems.common.utils.JsonUtils.getAsBoolean;
import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.edge.io.api.ShellyUtils.generateDebugLog;
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
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.io.api.ShellyUtils;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Shelly.3EM", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class IoShelly3EmImpl extends AbstractOpenemsComponent
		implements IoShelly3Em, DigitalOutput, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	private final Logger log = LoggerFactory.getLogger(IoShelly3EmImpl.class);
	private final BooleanWriteChannel[] digitalOutputChannels;

	private MeterType meterType = null;
	private String baseUrl;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	@Reference()
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	public IoShelly3EmImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				IoShelly3Em.ChannelId.values() //
		);
		this.digitalOutputChannels = new BooleanWriteChannel[] { this.channel(IoShelly3Em.ChannelId.RELAY) };

		ElectricityMeter.calculateSumActivePowerFromPhases(this);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.meterType = config.type();
		this.baseUrl = "http://" + config.ip();
		this.httpBridge = this.httpBridgeFactory.get();

		if (this.isEnabled()) {
			this.httpBridge.subscribeJsonEveryCycle(this.baseUrl + "/status", this::processHttpResult);
		}
	}

	@Deactivate
	protected void deactivate() {
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
		super.deactivate();
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.digitalOutputChannels;
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this.digitalOutputChannels, this.getActivePowerChannel());
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
			-> this.calculateEnergy();
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
			-> ShellyUtils.executeWrite(this.getRelayChannel(), this.baseUrl, this.httpBridge, 0);
		}
	}

	private void processHttpResult(JsonElement result, Throwable error) {
		this._setSlaveCommunicationFailed(result == null);

		// Prepare variables
		Boolean relay0 = null;
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
		boolean hasUpdate = false;
		boolean overpower = false;

		if (error != null) {
			this.logDebug(this.log, error.getMessage());

		} else {
			try {
				var response = getAsJsonObject(result);

				var relays = getAsJsonArray(response, "relays");
				if (!relays.isEmpty()) {
					var relay = getAsJsonObject(relays.get(0));
					relay0 = getAsBoolean(relay, "ison");
					overpower = getAsBoolean(relay, "overpower");
				}

				var update = getAsJsonObject(response, "update");
				hasUpdate = getAsBoolean(update, "has_update");

				activePower = round(getAsFloat(response, "total_power"));

				var emeters = getAsJsonArray(response, "emeters");
				for (int i = 0; i < emeters.size(); i++) {
					var emeter = getAsJsonObject(emeters.get(i));
					var power = round(getAsFloat(emeter, "power"));
					var voltage = round(getAsFloat(emeter, "voltage") * 1000);
					var current = round(getAsFloat(emeter, "current") * 1000);
					var isValid = getAsBoolean(emeter, "is_valid");

					switch (i + 1 /* phase */) {
					case 1 -> {
						activePowerL1 = power;
						voltageL1 = voltage;
						currentL1 = current;
						this.channel(IoShelly3Em.ChannelId.EMETER1_EXCEPTION).setNextValue(!isValid);
					}
					case 2 -> {
						activePowerL2 = power;
						voltageL2 = voltage;
						currentL2 = current;
						this.channel(IoShelly3Em.ChannelId.EMETER2_EXCEPTION).setNextValue(!isValid);
					}
					case 3 -> {
						activePowerL3 = power;
						voltageL3 = voltage;
						currentL3 = current;
						this.channel(IoShelly3Em.ChannelId.EMETER3_EXCEPTION).setNextValue(!isValid);
					}
					}
				}

			} catch (OpenemsNamedException e) {
				this.logDebug(this.log, e.getMessage());
			}
		}

		// Actually set Channels
		this._setRelay(relay0);
		this.channel(IoShelly3Em.ChannelId.RELAY_OVERPOWER_EXCEPTION).setNextValue(overpower);
		this._setActivePower(activePower);
		this.channel(IoShelly3Em.ChannelId.HAS_UPDATE).setNextValue(hasUpdate);

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
