package io.openems.impl.controller.channelthreshold;

import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.hysteresis.Hysteresis;

public class ChannelThresholdController extends Controller {

	private ThingRepository repo = ThingRepository.getInstance();

	@SuppressWarnings("unchecked")
	@ConfigInfo(title = "the address of the channel, which indicates the switching by the min and max threshold.", type = String.class)
	public ConfigChannel<String> thresholdChannelName = new ConfigChannel<String>("thresholdChannelAddress", this)
			.addChangeListener((channel, newValue, oldValue) -> {
				Optional<String> channelAddress = (Optional<String>) newValue;
				if (channelAddress.isPresent()) {
					Optional<Channel> ch = repo.getChannelByAddress(channelAddress.get());
					if (ch.isPresent()) {
						thresholdChannel = (ReadChannel<Long>) ch.get();
					} else {
						log.error("Channel " + channelAddress.get() + " not found");
					}
				} else {
					log.error("'outputChannelAddress' is not configured!");
				}
			});

	@SuppressWarnings("unchecked")
	@ConfigInfo(title = "the address of the digital output, which should be switched.", type = String.class)
	public ConfigChannel<String> outputChannelName = new ConfigChannel<String>("outputChannelAddress", this)
			.addChangeListener((channel, newValue, oldValue) -> {
				Optional<String> channelAddress = (Optional<String>) newValue;
				if (channelAddress.isPresent()) {
					Optional<Channel> ch = repo.getChannelByAddress(channelAddress.get());
					if (ch.isPresent()) {
						outputChannel = (WriteChannel<Boolean>) ch.get();
					} else {
						log.error("Channel " + channelAddress.get() + " not found");
					}
				} else {
					log.error("'outputChannelAddress' is not configured!");
				}
			});
	@ConfigInfo(title = "value of the lower threshold where the output should be switched on.", type = Long.class)
	public ConfigChannel<Long> lowerThreshold = new ConfigChannel<Long>("lowerThreshold", this)
			.addChangeListener((channel, newValue, oldValue) -> {
				if (newValue.isPresent()) {
					createHysteresis();
				}
			});
	@ConfigInfo(title = "value of the upper threshold where the output should be switched off.", type = Long.class)
	public ConfigChannel<Long> upperThreshold = new ConfigChannel<Long>("upperThreshold", this)
			.addChangeListener((channel, newValue, oldValue) -> {
				if (newValue.isPresent()) {
					createHysteresis();
				}
			});

	private ReadChannel<Long> thresholdChannel;
	private WriteChannel<Boolean> outputChannel;
	private Hysteresis thresholdHysteresis;
	private boolean isActive = false;

	public ChannelThresholdController() {
		super();
	}

	public ChannelThresholdController(String thingId) {
		super(thingId);
	}

	@Override
	public void run() {
		try {
			if (thresholdHysteresis != null) {
				thresholdHysteresis.apply(thresholdChannel.value(), (state, multiplier) -> {
					try {
						switch (state) {
						case ABOVE:
							outputChannel.pushWrite(false);
							isActive = false;
							break;
						case ASC:
							if (isActive) {
								outputChannel.pushWrite(true);
							} else {
								outputChannel.pushWrite(false);
							}
							break;
						case BELOW:
							outputChannel.pushWrite(true);
							isActive = true;
							break;
						case DESC:
							if (isActive) {
								outputChannel.pushWrite(true);
							} else {
								outputChannel.pushWrite(false);
							}
							break;
						default:
							break;
						}
					} catch (WriteChannelException e) {
						log.error("failed to write outputChannel[" + outputChannel.id() + "]", e);
					}
				});
			}
		} catch (InvalidValueException e) {
			log.error("thresholdChannel has no valid value!");
		}
	}

	private void createHysteresis() {
		try {
			thresholdHysteresis = new Hysteresis(lowerThreshold.value(), upperThreshold.value());
		} catch (InvalidValueException e) {
			log.error("lower or upper Threshold is invalid! Can't create Hysteresis!");
		}
	}

}
