package io.openems.impl.controller.symmetric.powerramp;

import java.util.List;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.ControllerUtils;
import io.openems.core.utilities.Power;

public class PowerRampController extends Controller {

	public ConfigChannel<List<Ess>> esss = new ConfigChannel<List<Ess>>("esss", this, Ess.class);

	public ConfigChannel<Integer> pMax = new ConfigChannel<Integer>("pMax", this, Integer.class);
	public ConfigChannel<Integer> pStep = new ConfigChannel<Integer>("pStep", this, Integer.class);
	public ConfigChannel<Double> cosPhi = new ConfigChannel<Double>("cosPhi", this, Double.class);

	@Override public void run() {
		try {
			for (Ess ess : esss.value()) {
				try {
					Power power = ess.power;
					long activePower = 0L;
					activePower = ess.activePower.value();
					if (Math.abs(activePower + pStep.value()) <= Math.abs(pMax.value())) {
						power.setActivePower(activePower + pStep.value());
					} else {
						power.setActivePower(pMax.value());
					}
					power.setReactivePower(ControllerUtils.calculateReactivePower(activePower, cosPhi.value()));
					power.writePower();
					log.info("Set ActivePower [" + power.getActivePower() + "] Set ReactivePower ["
							+ power.getReactivePower() + "]");
				} catch (InvalidValueException e) {
					log.error("Failed to write fixed P/Q value for Ess " + ess.id, e);
				}
			}
		} catch (InvalidValueException e) {
			log.error("No ess found.", e);
		}
	}

}
