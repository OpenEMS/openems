package io.openems.edge.simulator.io;

import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;

import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.IO.DigitalInputOutput", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
)
public class DigitalInputOutput extends AbstractOpenemsComponent
		implements DigitalInput, DigitalOutput, OpenemsComponent {

	public static final String CHANNEL_NAME = "InputOutput%d";

	private final Logger log = LoggerFactory.getLogger(DigitalInputOutput.class);

	private WriteChannel<Boolean>[] channels = new BooleanWriteChannel[0];

	public DigitalInputOutput() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());

		// Generate OutputChannels
		this.channels = new BooleanWriteChannel[config.numberOfOutputs()];
		for (int i = 0; i < config.numberOfOutputs(); i++) {
			String channelName = String.format(CHANNEL_NAME, i);
			BooleanWriteChannel channel = new BooleanWriteChannel(this, new MyChannelId(channelName));
			// default to OFF
			channel.setNextValue(false);
			this.logInfo(log, "Creating simulated DigitalOutput [" + channel.address() + "]");
			// register listener for write-events on the channel to set its new value
			channel.onSetNextWrite(value -> {
				this.logInfo(log, "DigitalOutput [" + channel.address() + "] was turned " + (value ? "ON" : "OFF"));
				channel.setNextValue(value);
			});
			this.addChannel(channel);
			this.channels[i] = channel;
		}
	}

	@Override
	public Channel<Boolean>[] digitalInputChannels() {
		return this.channels;
	}

	@Override
	public WriteChannel<Boolean>[] digitalOutputChannels() {
		return this.channels;
	}

	@Override
	public String debugLog() {
		StringBuilder b = new StringBuilder();
		for (WriteChannel<Boolean> channel : this.channels) {
			Optional<Boolean> valueOpt = channel.value().asOptional();
			if (valueOpt.isPresent()) {
				b.append(valueOpt.get() ? "x" : "-");
			} else {
				b.append("?");
			}
		}
		return b.toString();
	}

}
