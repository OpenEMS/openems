package io.openems.edge.io.shelly.shelly25;

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
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.io.generic_http_worker.generic_http_worker;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Shelly.25", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE//
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class Shelly25Impl extends AbstractOpenemsComponent
		implements Shelly25, DigitalOutput, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(Shelly25Impl.class);

	private final BooleanWriteChannel[] digitalOutputChannels;
	//private ShellyApi shellyApi = null;
	private generic_http_worker worker;
	private Config config;
	private String base_url = null;
	private String[] on_url = null;
	private String[] off_url = null;

	public Shelly25Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				Shelly25.ChannelId.values() //
		);
		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.channel(Shelly25.ChannelId.RELAY_1), //
				this.channel(Shelly25.ChannelId.RELAY_2), //
		};
	}
	
	void generate_worker() {
		this.base_url = "http://" + this.config.ip();
		this.on_url = new String[]{this.base_url + "/relay/0?turn=on",this.base_url + "/relay/1?turn=on"}; 	
		this.off_url = new String[]{this.base_url + "/relay/0?turn=off",this.base_url + "/relay/1?turn=off"}; 
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
		var i = 1;
		for (WriteChannel<Boolean> channel : this.digitalOutputChannels) {
			String valueText;
			var valueOpt = channel.value().asOptional();
			if (valueOpt.isPresent()) {
				valueText = valueOpt.get() ? "x" : "-";
			} else {
				valueText = "?";
			}
			b.append(i + valueText);

			// add space for all but the last
			if (++i <= this.digitalOutputChannels.length) {
				b.append(" ");
			}
		}
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
		Boolean relay1IsOn;
		Boolean relay2IsOn;
		try {			
			var json_tmp = this.worker.get_last_by_id(0);
			if(json_tmp == "_undefined_") {
				this.logError(this.log, "Generic HTTP Worker Called on an Undefined Index 0");
				throw this.worker.get_last_error();
			}else if(json_tmp == "_no_value_") {
				this.logInfo(this.log, "Generic HTTP Worker has not recieved Data yet");
				relay1IsOn = null;
				relay2IsOn = null;
			}else if(json_tmp == "_com_error_") {
				this.logError(this.log, "Generic HTTP Worker gave back the State \"Com-Error\"");
				throw this.worker.get_last_error();
			}else {
				var json = JsonUtils.parse(json_tmp);
				var relays = JsonUtils.getAsJsonArray(json, "relays");
				var relay1 = JsonUtils.getAsJsonObject(relays.get(0));
				relay1IsOn = JsonUtils.getAsBoolean(relay1, "ison");
				var relay2 = JsonUtils.getAsJsonObject(relays.get(1));
				relay2IsOn = JsonUtils.getAsBoolean(relay2, "ison");

				this._setSlaveCommunicationFailed(false);		
			}

		} catch (Exception e) {
			relay1IsOn = null;
			relay2IsOn = null;
			this.logError(this.log, "Unable to read from Shelly API: " + e.getMessage());
			this._setSlaveCommunicationFailed(true);
		}
		this._setRelay1(relay1IsOn);
		this._setRelay2(relay2IsOn);
	}

	/**
	 * Execute on Cycle Event "Execute Write".
	 */
	private void eventExecuteWrite() {
		try {
			this.executeWrite(this.getRelay1Channel(), 0);
			this.executeWrite(this.getRelay2Channel(), 1);

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
			this.worker.send_external_url(this.on_url[index], "");
		}else {
			this.worker.send_external_url(this.off_url[index], "");
		}
	}

}