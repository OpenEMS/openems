package io.openems.edge.simulator.io;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.IO.DigitalInputOutput", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class SimulatorIoDigitalInputOutputImpl extends AbstractOpenemsComponent
		implements SimulatorIoDigitalInputOutput, DigitalInput, DigitalOutput, OpenemsComponent {

	public static final String CHANNEL_NAME = "INPUT_OUTPUT%d";

	private final Logger log = LoggerFactory.getLogger(SimulatorIoDigitalInputOutputImpl.class);

	private BooleanWriteChannel[] writeChannels = {};
	private BooleanReadChannel[] readChannels = {};

	public SimulatorIoDigitalInputOutputImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				DigitalInput.ChannelId.values(), //
				SimulatorIoDigitalInputOutput.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		// Generate OutputChannels
		this.writeChannels = new BooleanWriteChannel[config.numberOfOutputs()];
		this.readChannels = new BooleanReadChannel[config.numberOfOutputs()];
		for (var i = 0; i < config.numberOfOutputs(); i++) {
			var channelName = String.format(CHANNEL_NAME, i);
			var doc = new BooleanDoc() //
					.accessMode(AccessMode.READ_WRITE);
			var channel = (BooleanWriteChannel) this.addChannel(new MyChannelId(channelName, doc));

			// default to OFF
			channel.setNextValue(false);
			this.logInfo(this.log, "Creating simulated DigitalOutput [" + channel.address() + "]");
			// register listener for write-events on the channel to set its new value
			channel.onSetNextWrite(value -> {
				this.logInfo(this.log,
						"DigitalOutput [" + channel.address() + "] was turned " + (value ? "ON" : "OFF"));
				channel.setNextValue(value);
			});
			this.readChannels[i] = channel;
			this.writeChannels[i] = channel;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public BooleanReadChannel[] digitalInputChannels() {
		return this.readChannels;
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.writeChannels;
	}

	@Override
	public String debugLog() {
		var b = new StringBuilder();
		for (BooleanReadChannel channel : this.readChannels) {
			var valueOpt = channel.value().asOptional();
			if (valueOpt.isPresent()) {
				b.append(valueOpt.get() ? "x" : "-");
			} else {
				b.append("?");
			}
		}
		return b.toString();
	}

}
