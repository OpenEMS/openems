package io.openems.edge.io.shelly.shellyplug;

import java.util.Objects;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.io.generic_http_worker.generic_http_worker;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

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
public class ShellyPlugImpl extends AbstractOpenemsComponent
		implements ShellyPlug, DigitalOutput, SymmetricMeter, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(ShellyPlugImpl.class);

	private final BooleanWriteChannel[] digitalOutputChannels;
	//private ShellyApi shellyApi = null;
	private generic_http_worker worker;
	private Config config;
	private String base_url = null;
	private String on_url = null;
	private String off_url = null;
	private MeterType meterType = null;

	public ShellyPlugImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				ShellyPlug.ChannelId.values() //
		);
		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.channel(ShellyPlug.ChannelId.RELAY) //
		};
	}
	
	
	void generate_worker() {
		this.base_url = "http://" + this.config.ip();
		this.on_url = this.base_url + "/relay/0?turn=on"; 	
		this.off_url = this.base_url + "/relay/0?turn=off"; 
		String state_url = this.base_url + "/status";
		int timeout = 5000; //Default Timeout
		String tmp[] = {state_url};
		this.worker = new generic_http_worker(tmp,timeout);
	}
	void destroy_worker() {
		this.worker.deactivate();
	}
	void activate_worker() {
		this.worker.activate("Shelly-Plug Worker");
		this.worker.triggerNextRun();
	}
	
	

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		generate_worker();
		activate_worker();
		this.meterType = config.type();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.destroy_worker();
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
			var json_tmp = this.worker.get_last_by_id(0);
			if(json_tmp == "_undefined_") {
				this.logError(this.log, "Generic HTTP Worker Called on an Undefined Index 0");
				throw this.worker.get_last_error();
			}else if(json_tmp == "_no_value_") {
				this.logInfo(this.log, "Generic HTTP Worker has not recieved Data yet");
				relayIson = null;
				power = null;
				energy = null;
			}else if(json_tmp == "_com_error_") {
				this.logError(this.log, "Generic HTTP Worker gave back the State \"Com-Error\"");
				throw this.worker.get_last_error();
			}else {
				//Es ist kein Fehler und auch nicht der Erwartete Wert zurück gekommen daher wird false geschrieben
				var json = JsonUtils.parse(json_tmp);
				var relays = JsonUtils.getAsJsonArray(json, "relays");
				var relay1 = JsonUtils.getAsJsonObject(relays.get(0));
				relayIson = JsonUtils.getAsBoolean(relay1, "ison");
				var meters = JsonUtils.getAsJsonArray(json, "meters");
				var meter1 = JsonUtils.getAsJsonObject(meters.get(0));
				power = Math.round(JsonUtils.getAsFloat(meter1, "power"));
				energy = JsonUtils.getAsLong(meter1, "total") /* Unit: Wm */ / 60 /* Wh */;
			}
			

			this._setSlaveCommunicationFailed(false);

		} catch (Exception e) {
			this.logError(this.log, "Unable to read from Shelly API: " + e.getMessage());
			this._setSlaveCommunicationFailed(true);
		}
		this._setRelay(relayIson);
		this._setActivePower(power);
		this._setActiveProductionEnergy(energy);
		this.worker.triggerNextRun();
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
		if(writeValue.get()) {
			this.worker.send_external_url(this.on_url, "");
		}else {
			this.worker.send_external_url(this.off_url, "");
		}
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

}