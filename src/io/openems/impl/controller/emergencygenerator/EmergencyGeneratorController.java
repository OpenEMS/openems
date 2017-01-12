package io.openems.impl.controller.emergencygenerator;

import java.util.Optional;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.exception.InvalidValueException;

public class EmergencyGeneratorController extends Controller {

	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this, Ess.class);

	public ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this, Meter.class);

	public ConfigChannel<Long> minSoc = new ConfigChannel<Long>("minSoc", this, Long.class);
	public ConfigChannel<Long> maxSoc = new ConfigChannel<Long>("maxSoc", this, Long.class);
	private boolean generatorOn = true;

	public EmergencyGeneratorController() {
		super();
		// TODO Auto-generated constructor stub
	}

	public EmergencyGeneratorController(String thingId) {
		super(thingId);
		// TODO Auto-generated constructor stub
	}

	@Override public void run() {
		try {
			// Check if grid is available
			if (meter.value().voltage.valueOptional().isPresent()
					&& !(meter.value().voltage.value() >= 200 && meter.value().voltage.value() <= 260)) {
				if (ess.value().gridMode.labelOptional().equals(Optional.of(EssNature.OFF_GRID)) && !generatorOn
						&& ess.value().soc.value() <= minSoc.value()) {
					// TODO switch generator on
					generatorOn = true;
				} else if (ess.value().gridMode.labelOptional().equals(Optional.of(EssNature.ON_GRID)) && generatorOn
						&& ess.value().soc.value() >= maxSoc.value()) {
					// TODO switch generator off
					generatorOn = false;
				}
			}
		} catch (InvalidValueException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
