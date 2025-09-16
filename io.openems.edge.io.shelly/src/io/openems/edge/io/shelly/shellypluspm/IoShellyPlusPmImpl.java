package io.openems.edge.io.shelly.shellypluspm;

import static io.openems.common.utils.JsonUtils.getAsBoolean;
import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE;
import static io.openems.edge.io.shelly.common.Utils.generateDebugLog;
import static java.lang.Math.round;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.Objects;

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

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Shelly.PlusPM", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_EXECUTE_WRITE, //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class IoShellyPlusPmImpl extends AbstractOpenemsComponent implements IoShellyPlusPm, DigitalOutput,
		SinglePhaseMeter, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	private final Logger log = LoggerFactory.getLogger(IoShellyPlusPmImpl.class);
	private final BooleanWriteChannel[] digitalOutputChannels;

	private MeterType meterType = null;
	private SinglePhase phase = null;
	private String baseUrl;
	private int factor = 1;
	private int channel = 0;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata;

	@Reference(cardinality = MANDATORY)
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	public IoShellyPlusPmImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				IoShellyPlusPm.ChannelId.values() //
		);
		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.channel(IoShellyPlusPm.ChannelId.RELAY) //
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

		this.channel = config.channel();
		if (config.inverted()) {
			factor = -1;
		}

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
		return generateDebugLog(this.digitalOutputChannels, this.getActivePowerChannel());
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> this.calculateEnergy();
		case TOPIC_CYCLE_EXECUTE_WRITE -> this.executeWrite(this.getRelayChannel(), channel);
		}
	}

	private void processHttpResult(HttpResponse<JsonElement> result, Throwable error) {
		this._setSlaveCommunicationFailed(result == null);

		Integer power = null;
		Integer voltage = null;
		Integer current = null;
		Boolean relayX = null;
		boolean restartRequired = false;

		if (error != null) {
			this.logDebug(this.log, error.getMessage());

		} else {
			try {
				var jsonResponse = getAsJsonObject(result.data());
				var switchX = getAsJsonObject(jsonResponse, "switch:" + channel);
				power = round(getAsFloat(switchX, "apower")) * factor;
				voltage = round(getAsFloat(switchX, "voltage") * 1000);
				current = round(getAsFloat(switchX, "current") * 1000);
				relayX = getAsBoolean(switchX, "output");

				var sys = getAsJsonObject(jsonResponse, "sys");
				restartRequired = getAsBoolean(sys, "restart_required");

			} catch (OpenemsNamedException e) {
				this.logDebug(this.log, e.getMessage());
			}
		}

		this._setRelay(relayX);
		this._setActivePower(power);
		this._setCurrent(current);
		this._setVoltage(voltage);
		this.channel(IoShellyPlusPm.ChannelId.NEEDS_RESTART).setNextValue(restartRequired);
	}

	/**
	 * Execute on Cycle Event "Execute Write".
	 * 
	 * @param channel write channel
	 * @param index   index
	 */
	private void executeWrite(BooleanWriteChannel channel, int index) {
		var readValue = channel.value().get();
		var writeValue = channel.getNextWriteValueAndReset();
		if (writeValue.isEmpty()) {
			// no write value
			return;
		}
		if (Objects.equals(readValue, writeValue.get())) {
			// read value equals write value, so no need to write
			return;
		}
		final String url = this.baseUrl + "/relay/" + index + "?turn=" + (writeValue.get() ? "on" : "off");
		this.httpBridge.get(url).whenComplete((response, error) -> {
			if (error != null) {
				this.logError(this.log, "HTTP request failed: " + error.getMessage());
				this._setSlaveCommunicationFailed(true);
			} else {
				// Optionally log success or handle response
				this._setSlaveCommunicationFailed(false);
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