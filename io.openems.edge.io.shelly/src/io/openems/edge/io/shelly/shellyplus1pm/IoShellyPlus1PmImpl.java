package io.openems.edge.io.shelly.shellyplus1pm;

import static io.openems.common.utils.JsonUtils.getAsBoolean;
import static io.openems.common.utils.JsonUtils.getAsFloat;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static java.lang.Math.round;

import java.util.Objects;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
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
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SinglePhase;
import io.openems.edge.meter.api.SinglePhaseMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Shelly.Plus1Pm", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE//
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class IoShellyPlus1PmImpl extends AbstractOpenemsComponent implements IoShellyPlus1Pm, DigitalOutput,
		SinglePhaseMeter, ElectricityMeter, OpenemsComponent, TimedataProvider, EventHandler {

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	private final Logger log = LoggerFactory.getLogger(IoShellyPlus1PmImpl.class);
	private final BooleanWriteChannel[] digitalOutputChannels;

	private MeterType meterType = null;
	private SinglePhase phase = null;
	private String baseUrl;

	private volatile Timedata timedata;

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	public IoShellyPlus1PmImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				IoShellyPlus1Pm.ChannelId.values() //
		);
		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.channel(IoShellyPlus1Pm.ChannelId.RELAY) //
		};
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
		var b = new StringBuilder();
		var valueOpt = this.getRelayChannel().value().asOptional();
		if (valueOpt.isPresent()) {
			b.append(valueOpt.get() ? "ON" : "OFF");
		} else {
			b.append("Unknown");
		}
		b.append("|");
		b.append(this.getActivePowerChannel().value().asString());

		return b.toString();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> this.calculateEnergy();
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE -> {
			this.executeWrite(this.getRelayChannel(), 0);

		}
		}
	}

	private void processHttpResult(JsonElement result, Throwable error) {
		this._setSlaveCommunicationFailed(result == null);

		Integer power = null;
		Integer voltage = null;
		Integer current = null;
		boolean relay0 = false;
		boolean restartRequired = false;

		if (error != null) {
			this.logDebug(this.log, error.getMessage());
		} else {
			try {
				var jsonResponse = getAsJsonObject(result);
				var switch0 = getAsJsonObject(jsonResponse, "switch:0");
				power = round(getAsFloat(switch0, "apower"));
				voltage = round(getAsFloat(switch0, "voltage") * 1000);
				current = round(getAsFloat(switch0, "current") * 1000);
				relay0 = getAsBoolean(switch0, "output");

				var sys = getAsJsonObject(jsonResponse, "sys");
				restartRequired = getAsBoolean(sys, "restart_required");

				if (this.phase != null) {
					switch (this.phase) {
					case L1:
						this._setActivePowerL1(power);
						this._setVoltageL1(voltage);
						this._setCurrentL1(current);
						break;
					case L2:
						this._setActivePowerL2(power);
						this._setVoltageL2(voltage);
						this._setCurrentL2(current);
						break;
					case L3:
						this._setActivePowerL3(power);
						this._setVoltageL3(voltage);
						this._setCurrentL3(current);
						break;
					}
				}
			} catch (OpenemsNamedException e) {
				this.logDebug(this.log, e.getMessage());
			}

			this._setRelay(relay0);
			this._setActivePower(power);
			this.channel(IoShellyPlus1Pm.ChannelId.NEEDS_RESTART).setNextValue(restartRequired);
		}
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
