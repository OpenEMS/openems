package io.openems.edge.io.gpio;

import java.util.stream.Collectors;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.io.gpio.hardware.HardwarePlatform;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Gpio", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		scope = ServiceScope.SINGLETON //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
})
public class IoGpioImpl extends AbstractOpenemsComponent
		implements IoGpio, OpenemsComponent, EventHandler, DigitalInput, DigitalOutput {

	private final Logger log = LoggerFactory.getLogger(IoGpioImpl.class);

	private HardwarePlatform hardwarePlatform;

	public IoGpioImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				IoGpio.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		// Initialize Hardware
		this.hardwarePlatform = config.hardwareType().createInstance(config.gpioPath());
		var channelIds = this.hardwarePlatform.getAllChannelIds();
		this.hardwarePlatform.createPinObjects(channelIds);
		this.addChannels(channelIds.toArray(io.openems.edge.common.channel.ChannelId[]::new));
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String debugLog() {
		final var readChannelIds = this.hardwarePlatform.getReadChannelIds();
		final var writeChannelIds = this.hardwarePlatform.getWriteChannelIds();
		return new StringBuilder()//
				.append("IO Read[")//
				.append(readChannelIds.stream()//
						.map(t -> this.hardwarePlatform.getGpioValueByChannelId(t).map(v -> v ? "x" : "-").orElse("?"))
						.collect(Collectors.joining()))//
				.append("]Write[")//
				.append(writeChannelIds.stream()//
						.map(t -> this.hardwarePlatform.getGpioValueByChannelId(t).map(v -> v ? "x" : "-").orElse("?"))
						.collect(Collectors.joining()))//
				.append("]")//
				.toString();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> {
			for (var channelId : this.hardwarePlatform.getReadChannelIds()) {
				final var nextChannelValue = this.hardwarePlatform.getGpioValueByChannelId(channelId);
				this.channel(channelId).setNextValue(nextChannelValue);
			}

			for (var channelId : this.hardwarePlatform.getWriteChannelIds()) {
				BooleanWriteChannel writeChannel = this.channel(channelId);
				final var nextValue = writeChannel.getNextWriteValueAndReset();
				try {
					this.hardwarePlatform.setGpio(channelId, nextValue.orElse(false));
				} catch (OpenemsException e) {
					this.log.error("Error while setting GPIO " + channelId.name());
				}
			}
		}
		}
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		var writeChannels = this.hardwarePlatform.getWriteChannelIds();
		return writeChannels.stream() //
				.map((t -> this.channel(t))) //
				.toArray(BooleanWriteChannel[]::new);
	}

	@Override
	public BooleanReadChannel[] digitalInputChannels() {
		var readChannels = this.hardwarePlatform.getReadChannelIds();
		return readChannels.stream() //
				.map((t -> this.channel(t))) //
				.toArray(BooleanReadChannel[]::new);
	}
}
