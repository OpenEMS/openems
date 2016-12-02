package io.openems.impl.controller.symmetric.cosphi;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.ControllerUtils;

public class CosPhiController extends Controller {

	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this, Ess.class);

	public ConfigChannel<Double> cosPhi = new ConfigChannel<Double>("cosPhi", this, Double.class);

	public CosPhiController() {
		super();
	}

	public CosPhiController(String thingId) {
		super(thingId);
	}

	@Override public void run() {
		try {
			if (ess.value().setActivePower.peekWrite().isPresent()) {
				ess.value().power.setReactivePower(ControllerUtils
						.calculateReactivePower(ess.value().setActivePower.peekWrite().get(), cosPhi.value()));
				ess.value().power.writePower();
				log.info("Set ReactivePower [" + ess.value().power.getReactivePower() + "]");
			} else {
				log.error(ess.id() + " no ActivePower is Set.");
			}
		} catch (InvalidValueException e) {
			log.error("No ess found.", e);
		}
	}

}
