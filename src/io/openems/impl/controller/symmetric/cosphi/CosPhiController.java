package io.openems.impl.controller.symmetric.cosphi;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.ControllerUtils;

public class CosPhiController extends Controller {
	@ConfigInfo(title = "The storage, which should hold a specific cosPhi.", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ConfigInfo(title = "The cosPhi to hold on the storage.", type = Double.class)
	public ConfigChannel<Double> cosPhi = new ConfigChannel<Double>("cosPhi", this);

	public CosPhiController() {
		super();
	}

	public CosPhiController(String thingId) {
		super(thingId);
	}

	@Override
	public void run() {
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
