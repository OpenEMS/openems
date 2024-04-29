package io.openems.edge.io.shelly.shellyplusplugs;

import static io.openems.common.utils.JsonUtils.getAsBoolean;
import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.edge.io.shelly.common.Utils.generateDebugLog;
import static java.lang.Math.round;

import java.util.Objects;

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

import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SinglePhase;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Shelly.Plus.PlugS", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE//
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class IoShellyPlusPlugsImpl extends AbstractOpenemsComponent implements IoShellyPlusPlugs, DigitalOutput,
		SinglePhaseMeter, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	private final Logger log = LoggerFactory.getLogger(IoShellyPlusPlugsImpl.class);
	private final BooleanWriteChannel[] digitalOutputChannels;

	private MeterType meterType = null;
	private SinglePhase phase = null;
	private String baseUrl;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	public IoShellyPlusPlugsImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				IoShellyPlusPlugs.ChannelId.values() //
		);
		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.channel(IoShellyPlusPlugs.ChannelId.RELAY) //
		};

		SinglePhaseMeter.calculateSinglePhaseFromActivePower(this);
		SinglePhaseMeter.calculateSinglePhaseFromCurrent(this);
		SinglePhaseMeter.calculateSinglePhaseFromVoltage(this);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.meterType = config.type();
		this.phase = config.phase();
		this.baseUrl = "http://" + config.ip();
		this.httpBridge = this.httpBridgeFactory.get();

		if (!this.isEnabled()) {
			return;
		}

		this.httpBridge.subscribeJsonEveryCycle(this.baseUrl + "/rpc/Shelly.GetStatus", this::processHttpResult);
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
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.digitalOutputChannels;
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this.getRelayChannel(), this.getActivePowerChannel());
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
			-> this.executeWrite();
		}
	}

	private void processHttpResult(JsonElement result, Throwable error) {
		this._setSlaveCommunicationFailed(result == null);

		Boolean relayStatus = null;
		Boolean updatesAvailable = false;
		Integer activePower = null;
		Integer current = null;
		Integer voltage = null;

		if (error != null) {
			this.logWarn(this.log, error.getMessage());

		} else {
			try {
				var response = getAsJsonObject(result);
				var sysInfo = getAsJsonObject(response, "sys");
				var update = getAsJsonObject(sysInfo, "available_updates");
				updatesAvailable = update != null && !update.entrySet().isEmpty();

				var relays = getAsJsonObject(response, "switch:0");
				activePower = round(getAsFloat(relays, "apower"));
				current = round(getAsFloat(relays, "current") * 1000);
				voltage = round(getAsFloat(relays, "voltage") * 1000);
				relayStatus = getAsBoolean(relays, "output");

			} catch (Exception e) {
				this.logWarn(this.log, e.getMessage());
			}
		}

		this._setRelay(relayStatus);
		this._setActivePower(activePower);
		this._setCurrent(current);
		this._setVoltage(voltage);
		this.channel(IoShellyPlusPlugs.ChannelId.HAS_UPDATE).setNextValue(updatesAvailable);
	}

	/**
	 * Execute on Cycle Event "Execute Write".
	 */
	private void executeWrite() {
		var channel = this.getRelayChannel();
		var readValue = channel.value().get();
		var writeValue = channel.getNextWriteValueAndReset();
		if (writeValue.isEmpty()) {
			return;
		}
		if (Objects.equals(readValue, writeValue.get())) {
			return;
		}
		var index = 0;
		final var url = this.baseUrl + "/relay/" + index + "?turn=" + (writeValue.get() ? "on" : "off");

		this.httpBridge.get(url).whenComplete((t, e) -> {
			this._setSlaveCommunicationFailed(e != null);
			if (e == null) {
				this.logDebug(this.log, "Executed write successfully for URL: " + url);
			} else {
				this.logError(this.log, "Failed to execute write for URL: " + url + "; Error: " + e.getMessage());
			}
		});
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
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	public SinglePhase getPhase() {
		return this.phase;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

}
