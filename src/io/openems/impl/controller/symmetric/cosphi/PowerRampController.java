package io.openems.impl.controller.symmetric.cosphi;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.utilities.ControllerUtils;

public class PowerRampController extends Controller {

	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this, Ess.class);

	public ConfigChannel<Double> cosPhi = new ConfigChannel<Double>("cosPhi", this, Double.class);

	@Override public void run() {
		try {
			if (ess.value().setActivePower.peekWrite().isPresent()) {
				try {
					ess.value().setReactivePower.pushWrite(ControllerUtils
							.calculateReactivePower(ess.value().setActivePower.peekWrite().get(), cosPhi.value()));
				} catch (WriteChannelException e) {
					log.error("Failed to set ReactivePower", e);
				}
			} else {
				log.error(ess.id() + " no ActivePower is Set.");
			}
		} catch (InvalidValueException e) {
			log.error("No ess found.", e);
		}
	}

}
