package io.openems.impl.controller.symmetric.balancingcurrent;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.exception.InvalidValueException;

public class BalancingCurrentController extends Controller {

	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this, Ess.class);

	public final ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this, Meter.class);

	public final ConfigChannel<Integer> currentOffset = new ConfigChannel<>("CurrentOffset", this, Integer.class);

	public BalancingCurrentController() {
		super();
	}

	public BalancingCurrentController(String thingId) {
		super(thingId);
	}

	@Override public void run() {
		try {
			Ess ess = this.ess.value();
			// Calculate required sum values
			long power = calculatePower() + ess.activePower.value();
			ess.power.setActivePower(power);
			ess.power.writePower();
			log.info(ess.id() + " Set ActivePower [" + ess.power.getActivePower() + "]");
		} catch (InvalidValueException e) {
			log.error(e.getMessage());
		}
	}

	private long calculatePower() throws InvalidValueException {
		long powerL1 = ((meter.value().currentL1.value() - currentOffset.value() / 3) / 1000)
				* (meter.value().voltageL1.value() / 1000);
		if (meter.value().activePowerL1.value() < 0) {
			powerL1 *= -1;
		}
		long powerL2 = ((meter.value().currentL2.value() - currentOffset.value() / 3) / 1000)
				* (meter.value().voltageL2.value() / 1000);
		if (meter.value().activePowerL2.value() < 0) {
			powerL2 *= -1;
		}
		long powerL3 = ((meter.value().currentL3.value() - currentOffset.value() / 3) / 1000)
				* (meter.value().voltageL3.value() / 1000);
		if (meter.value().activePowerL3.value() < 0) {
			powerL3 *= -1;
		}
		return powerL1 + powerL2 + powerL3;
	}

}
