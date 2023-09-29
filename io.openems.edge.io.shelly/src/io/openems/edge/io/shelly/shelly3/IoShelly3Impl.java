package io.openems.edge.io.shelly.shelly3;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.io.shelly.common.ShellyApi;
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

import java.util.Objects;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Shelly.3", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE//
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class IoShelly3Impl extends AbstractOpenemsComponent
		implements IoShelly3, DigitalOutput, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(IoShelly3Impl.class);
	private final BooleanWriteChannel[] digitalOutputChannels;

	private ShellyApi shellyApi = null;

	public IoShelly3Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				IoShelly3.ChannelId.values() //
		);
		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.channel(IoShelly3.ChannelId.RELAY_1), //
				this.channel(IoShelly3.ChannelId.RELAY_2), //
				this.channel(IoShelly3.ChannelId.RELAY_3), //
		};
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.shellyApi = new ShellyApi(config.ip());
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
		Boolean relay3IsOn;
		try {
			var json = this.shellyApi.getStatus();
			var relays = JsonUtils.getAsJsonArray(json, "relays");
			var relay1 = JsonUtils.getAsJsonObject(relays.get(0));
			relay1IsOn = JsonUtils.getAsBoolean(relay1, "ison");
			var relay2 = JsonUtils.getAsJsonObject(relays.get(1));
			relay2IsOn = JsonUtils.getAsBoolean(relay2, "ison");
			var relay3 = JsonUtils.getAsJsonObject(relays.get(2));
			relay3IsOn = JsonUtils.getAsBoolean(relay3, "ison");

			this._setSlaveCommunicationFailed(false);

		} catch (OpenemsNamedException | IndexOutOfBoundsException e) {
			relay1IsOn = null;
			relay2IsOn = null;
			relay3IsOn = null;
			this.logError(this.log, "Unable to read from Shelly API: " + e.getMessage());
			this._setSlaveCommunicationFailed(true);
		}
		this._setRelay1(relay1IsOn);
		this._setRelay2(relay2IsOn);
		this._setRelay3(relay3IsOn);
	}

	/**
	 * Execute on Cycle Event "Execute Write".
	 */
	private void eventExecuteWrite() {
		try {
			this.executeWrite(this.getRelay1Channel(), 0);
			this.executeWrite(this.getRelay2Channel(), 1);
			this.executeWrite(this.getRelay3Channel(), 2);

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
		this.shellyApi.setRelayTurn(index, writeValue.get());
	}
}