package io.openems.edge.io.shelly.shellyplug;

import java.util.Objects;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.HttpBridge;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.io.shelly.common.ShellyApi;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SinglePhase;
import io.openems.edge.meter.api.SinglePhaseMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Shelly.Plug", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE//
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class IoShellyPlugImpl extends AbstractOpenemsComponent
		implements IoShellyPlug, DigitalOutput, SinglePhaseMeter, ElectricityMeter, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(IoShellyPlugImpl.class);
	private final BooleanWriteChannel[] digitalOutputChannels;

	@Reference
	private HttpBridge httpBridge;
	
	private ShellyApi shellyApi = null;
	private MeterType meterType = null;
	private SinglePhase phase = null;

	public IoShellyPlugImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				IoShellyPlug.ChannelId.values() //
		);
		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.channel(IoShellyPlug.ChannelId.RELAY) //
		};

		SinglePhaseMeter.calculateSinglePhaseFromActivePower(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.shellyApi = new ShellyApi(config.ip(), httpBridge);
		this.meterType = config.type();
		this.phase = config.phase();
	}

	@Override
	@Deactivate
	protected void deactivate() {
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
			b.append(valueOpt.get() ? "On" : "Off");
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
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.eventBeforeProcessImage();
			break;

		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this.eventExecuteWrite();
			break;
		}
	}

	/**
	 * Execute on Cycle Event "Before Process Image".
	 */
	private void eventBeforeProcessImage() {
		Boolean relayIson = null;
		Integer power = null;
		Long energy = null;
		try {
			var json = this.shellyApi.getState();
			var relays = JsonUtils.getAsJsonArray(json, "relays");
			var relay1 = JsonUtils.getAsJsonObject(relays.get(0));
			relayIson = JsonUtils.getAsBoolean(relay1, "ison");
			var meters = JsonUtils.getAsJsonArray(json, "meters");
			var meter1 = JsonUtils.getAsJsonObject(meters.get(0));
			power = Math.round(JsonUtils.getAsFloat(meter1, "power"));
			energy = JsonUtils.getAsLong(meter1, "total") /* Unit: Wm */ / 60 /* Wh */;

			this._setSlaveCommunicationFailed(false);

		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Unable to read from Shelly API: " + e.getMessage());
			this._setSlaveCommunicationFailed(true);
		}
		this._setRelay(relayIson);
		this._setActivePower(power);
		this._setActiveProductionEnergy(energy);
	}

	/**
	 * Execute on Cycle Event "Execute Write".
	 */
	private void eventExecuteWrite() {
		try {
			this.executeWrite(this.getRelayChannel(), 0);

			this._setSlaveCommunicationFailed(false);
		} catch (OpenemsNamedException e) {
			this._setSlaveCommunicationFailed(true);
		}
	}

	private void executeWrite(BooleanWriteChannel channel, int index) throws OpenemsNamedException {
		var readValue = channel.value().get();
		var writeValue = channel.getNextWriteValueAndReset();
		if (!writeValue.isPresent()) {
			// no write value
			return;
		}
		if (Objects.equals(readValue, writeValue.get())) {
			// read value = write value
			return;
		}
		this.shellyApi.setRelayState(index, writeValue.get());
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