package io.openems.impl.controller.emergencygenerator;

import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.ThingRepository;

public class EmergencyGeneratorController extends Controller {

	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this, Ess.class);

	public ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this, Meter.class);

	public ConfigChannel<Long> minSoc = new ConfigChannel<Long>("minSoc", this, Long.class);
	public ConfigChannel<Long> maxSoc = new ConfigChannel<Long>("maxSoc", this, Long.class);

	private ThingRepository repo = ThingRepository.getInstance();

	public ConfigChannel<String> outputChannelAddress = new ConfigChannel<String>("outputChannelAddress", this,
			String.class).changeListener((channel, newValue, oldValue) -> {
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

	private WriteChannel<Boolean> outputChannel;

	private boolean generatorOn = true;

	public EmergencyGeneratorController() {
		super();
	}

	public EmergencyGeneratorController(String thingId) {
		super(thingId);
	}

	@Override
	public void run() {
		try {
			// Check if grid is available
			if (!meter.value().voltage.valueOptional().isPresent()
					|| !(meter.value().voltage.value() >= 200 && meter.value().voltage.value() <= 260)) {
				if (ess.value().gridMode.labelOptional().equals(Optional.of(EssNature.OFF_GRID)) && !generatorOn
						&& ess.value().soc.value() <= minSoc.value()) {
					// switch generator on
					outputChannel.pushWrite(true);
					generatorOn = true;
				} else if (ess.value().gridMode.labelOptional().equals(Optional.of(EssNature.ON_GRID)) && generatorOn
						&& ess.value().soc.value() >= maxSoc.value()) {
					// switch generator off
					outputChannel.pushWrite(false);
					generatorOn = false;
				}
			}
		} catch (InvalidValueException e) {
			log.error("Failed to read value!", e);
		} catch (WriteChannelException e) {
			log.error("Error due write to output [" + outputChannelAddress.valueOptional().orElse("<none>") + "]", e);
		}
	}

}
