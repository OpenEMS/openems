package io.openems.edge.io.shelly.shellyem;

import static io.openems.common.utils.JsonUtils.getAsBoolean;
import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.edge.io.shelly.common.Utils.executeWrite;
import static io.openems.edge.io.shelly.common.Utils.generateDebugLog;
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
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhase;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Shelly.EM", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class IoShellyEmImpl extends AbstractOpenemsComponent implements IoShellyEm, DigitalOutput, ElectricityMeter,
		OpenemsComponent, TimedataProvider, EventHandler, SinglePhaseMeter {

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	private final Logger log = LoggerFactory.getLogger(IoShellyEmImpl.class);
	private final BooleanWriteChannel[] digitalOutputChannels;

	private MeterType meterType;
	private SinglePhase phase;
	private String baseUrl;
	private Boolean sumEMeter1AndEMeter2;
	private int channel = 0;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	@Reference()
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	public IoShellyEmImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				SinglePhaseMeter.ChannelId.values(), //
				IoShellyEm.ChannelId.values() //
		);
		this.digitalOutputChannels = new BooleanWriteChannel[] { this.channel(IoShellyEm.ChannelId.RELAY) };
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.meterType = config.type();
		this.baseUrl = "http://" + config.ip();
		this.httpBridge = this.httpBridgeFactory.get();
		this.phase = config.phase();
		this.sumEMeter1AndEMeter2 = config.sumEmeter1AndEmeter2();
		this.channel = config.channel();

		if (this.isEnabled()) {
			this.httpBridge.subscribeJsonEveryCycle(this.baseUrl + "/status", this::processHttpResult);
		}

		SinglePhaseMeter.calculateSinglePhaseFromActivePower(this);
		SinglePhaseMeter.calculateSinglePhaseFromCurrent(this);
		SinglePhaseMeter.calculateSinglePhaseFromVoltage(this);
		SinglePhaseMeter.calculateSinglePhaseFromReactivePower(this);
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
			-> executeWrite(this.getRelayChannel(), this.baseUrl, this.httpBridge, 0);
		}
	}

	private void processHttpResult(HttpResponse<JsonElement> result, Throwable error) {
		this._setSlaveCommunicationFailed(result == null);

		Boolean relay0 = null;
		Integer activePower = null;
		Integer reactivePower = null;
		Integer voltage = null;
		Integer current = null;
		boolean overpower = false;

		if (error != null) {
			this.logDebug(this.log, error.getMessage());

		} else {
			try {
				var response = getAsJsonObject(result.data());

				var relays = getAsJsonArray(response, "relays");
				if (!relays.isEmpty()) {
					var relay = getAsJsonObject(relays.get(0));
					relay0 = getAsBoolean(relay, "ison");
					overpower = getAsBoolean(relay, "overpower");
				}

				int totalPower = 0;
				int totalVoltage = 0;
				int validEmeterCount = 0;
				int totalReactivePower = 0;

				var emeters = getAsJsonArray(response, "emeters");
				for (int i = 0; i < emeters.size(); i++) {
					var emeter = getAsJsonObject(emeters.get(i));
					var power = round(getAsFloat(emeter, "power"));
					var singleReactivePower = round(getAsFloat(emeter, "reactive"));
					var emeterVoltage = round(getAsFloat(emeter, "voltage") * 1000);
					var isValid = getAsBoolean(emeter, "is_valid");

					if (isValid) {
						if (this.sumEMeter1AndEMeter2) {
							totalPower += power;
							totalReactivePower += singleReactivePower;
							totalVoltage += emeterVoltage;
							validEmeterCount++;
						} else if (i == this.channel) {
							totalPower = power;
							totalReactivePower = singleReactivePower;
							totalVoltage = emeterVoltage;
							validEmeterCount++;
						}

						if (i == 0) {
							this.channel(IoShellyEm.ChannelId.EMETER1_EXCEPTION).setNextValue(false);
						} else if (i == 1) {
							this.channel(IoShellyEm.ChannelId.EMETER2_EXCEPTION).setNextValue(false);
						}
					} else {
						if (i == 0) {
							this.channel(IoShellyEm.ChannelId.EMETER1_EXCEPTION).setNextValue(true);
						} else if (i == 1) {
							this.channel(IoShellyEm.ChannelId.EMETER2_EXCEPTION).setNextValue(true);
						}
					}
				}

				if (validEmeterCount > 0) {
					voltage = totalVoltage / validEmeterCount;
					if (voltage > 0) {
						current = Math.round((float) (totalPower * 1000 / voltage) * 1000);
					}
					activePower = totalPower;
					reactivePower = totalReactivePower;
				}

			} catch (OpenemsNamedException e) {
				this.logDebug(this.log, e.getMessage());
			}
		}

		this._setRelay(relay0);
		this.channel(IoShellyEm.ChannelId.RELAY_OVERPOWER_EXCEPTION).setNextValue(overpower);
		this._setActivePower(activePower);
		this._setReactivePower(reactivePower);
		this._setVoltage(voltage);
		this._setCurrent(current);

	}

	private void calculateEnergy() {
		final var activePower = this.getActivePower().get();
		if (activePower == null) {
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower <= 0) {
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
	public SinglePhase getPhase() {
		return this.phase;
	}
}