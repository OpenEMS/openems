package io.openems.impl.controller.symmetric.balancingcosphi;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.exception.InvalidValueException;

public class BalancingCosPhiController extends Controller {

	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this, Ess.class);

	public ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this, Meter.class);

	public ConfigChannel<Double> cosPhi = new ConfigChannel<Double>("cosPhi", this, Double.class);

	public BalancingCosPhiController() {
		super();
	}

	public BalancingCosPhiController(String thingId) {
		super(thingId);
	}

	@Override public void run() {
		try {
			double phi = Math.acos(cosPhi.value());
			long q = (long) (meter.value().activePower.value() * Math.tan(phi)) - meter.value().reactivePower.value();
			q += ess.value().reactivePower.value();
			ess.value().power.setReactivePower(q);
			ess.value().power.writePower();
			log.info(ess.id() + " Set ReactivePower [" + ess.value().power.getReactivePower() + "]");
		} catch (InvalidValueException e) {
			log.error("Failed to read value.", e);
		}
	}

}
