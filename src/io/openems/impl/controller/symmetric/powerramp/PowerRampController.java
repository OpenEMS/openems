package io.openems.impl.controller.symmetric.powerramp;

import java.util.List;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.utilities.ControllerUtils;

public class PowerRampController extends Controller {

	public ConfigChannel<List<Ess>> esss = new ConfigChannel<List<Ess>>("esss", this, Ess.class);

	public ConfigChannel<Integer> pMax = new ConfigChannel<Integer>("pMax", this, Integer.class);
	public ConfigChannel<Integer> pStep = new ConfigChannel<Integer>("pStep", this, Integer.class);
	public ConfigChannel<Double> cosPhi = new ConfigChannel<Double>("cosPhi", this, Double.class);

	@Override public void run() {
		try {
			for (Ess ess : esss.value()) {
				try {
					long activePower = 0L;
					activePower = ess.activePower.value();
					if (Math.abs(activePower + pStep.value()) <= Math.abs(pMax.value())) {
						activePower = activePower + pStep.value();
					} else {
						activePower = pMax.value();
					}
					long reactivePower = ControllerUtils.calculateReactivePower(activePower, cosPhi.value());
					ess.setActivePower.pushWrite(activePower);
					ess.setReactivePower.pushWrite(reactivePower);
					log.info("Set ActivePower [" + activePower + "] Set ReactivePower [" + reactivePower + "]");
				} catch (WriteChannelException | InvalidValueException e) {
					log.error("Failed to write fixed P/Q value for Ess " + ess.id, e);
				}
			}
		} catch (InvalidValueException e) {
			log.error("No ess found.", e);
		}
	}

}
