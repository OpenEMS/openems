package io.openems.impl.controller.symmetric.balancingcurrent;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.InvalidValueException;

public class BalancingCurrentController extends Controller {

	@ConfigInfo(title = "The storage, which should be controlled", type = Ess.class)
	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ConfigInfo(title = "The meter which meassures the power from/to the grid", type = Meter.class)
	public final ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this);

	@ConfigInfo(title = "The current to hold on the grid meter.", type = Integer.class)
	public final ConfigChannel<Integer> currentOffset = new ConfigChannel<>("CurrentOffset", this);

	public BalancingCurrentController() {
		super();
	}

	public BalancingCurrentController(String thingId) {
		super(thingId);
	}

	@Override
	public void run() {
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
		long currentL1 = meter.value().currentL1.value();
		if (meter.value().activePowerL1.value() < 0) {
			currentL1 *= -1;
		}
		long powerL1 = ((currentL1 - currentOffset.value() / 3) / 1000) * (meter.value().voltageL1.value() / 1000);
		long currentL2 = meter.value().currentL2.value();
		if (meter.value().activePowerL2.value() < 0) {
			currentL2 *= -1;
		}
		long powerL2 = ((currentL2 - currentOffset.value() / 3) / 1000) * (meter.value().voltageL2.value() / 1000);
		long currentL3 = meter.value().currentL3.value();
		if (meter.value().activePowerL3.value() < 0) {
			currentL3 *= -1;
		}
		long powerL3 = ((currentL3 - currentOffset.value() / 3) / 1000) * (meter.value().voltageL3.value() / 1000);
		return powerL1 + powerL2 + powerL3;
	}

}
