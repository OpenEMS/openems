package io.openems.impl.controller.channelthreshold;

import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.Controller;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.ThingRepository;

public class ChannelThresholdController extends Controller {

	private ThingRepository repo = ThingRepository.getInstance();

	public ConfigChannel<String> thresholdChannelName = new ConfigChannel<String>("thresholdChannelAddress", this,
			String.class).changeListener((channel, newValue, oldValue) -> {
				try {
					String channelAddress = ((ReadChannel<String>) channel).value();
					Optional<Channel> ch = repo.getChannelByAddress(channelAddress);
					if (ch.isPresent()) {
						thresholdChannel = (ReadChannel<Long>) ch.get();
					} else {
						log.error("Channel " + channelAddress + " not found");
					}
				} catch (InvalidValueException e) {
					log.error("channelName is empty!");
				}
			});

	public ConfigChannel<String> outputChannelName = new ConfigChannel<String>("outputChannelAddress", this,
			String.class).changeListener((channel, newValue, oldValue) -> {
				try {
					String channelAddress = ((ReadChannel<String>) channel).value();
					Optional<Channel> ch = repo.getChannelByAddress(channelAddress);
					if (ch.isPresent()) {
						outputChannel = (WriteChannel<Boolean>) ch.get();
					} else {
						log.error("Channel " + channelAddress + " not found");
					}
				} catch (InvalidValueException e) {
					log.error("channelName is empty!");
				}
			});

	public ConfigChannel<Long> lowerThreshold = new ConfigChannel<>("lowerThreshold", this, Long.class);
	public ConfigChannel<Long> upperThreshold = new ConfigChannel<>("upperThreshold", this, Long.class);

	private ReadChannel<Long> thresholdChannel;
	private WriteChannel<Boolean> outputChannel;

	public ChannelThresholdController() {
		super();
	}

	public ChannelThresholdController(String thingId) {
		super(thingId);
	}

	@Override public void run() {
		try {
			if (thresholdChannel != null && thresholdChannel.value() <= lowerThreshold.value()
					&& !outputChannel.value()) {
				outputChannel.pushWrite(true);
			} else if (thresholdChannel != null && thresholdChannel.value() >= upperThreshold.value()
					&& outputChannel.value()) {
				outputChannel.pushWrite(false);
			}
		} catch (InvalidValueException e) {
			log.error(e.getMessage());
		} catch (WriteChannelException e) {
			e.printStackTrace();
		}
	}

}
