package io.openems.impl.controller.symmetric.balancingcurrent;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.exception.InvalidValueException;
import io.openems.core.utilities.ControllerUtils;

public class BalancingCurrentController extends Controller {

	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this, Ess.class);

	public final ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this, Meter.class);

	public final ConfigChannel<Integer> currentOffset = new ConfigChannel<>("CurrentOffset", this, Integer.class);
	public final ConfigChannel<Double> cosPhi = new ConfigChannel<>("cosPhi", this, Integer.class);

	public BalancingCurrentController() {
		super();
		// TODO Auto-generated constructor stub
	}

	public BalancingCurrentController(String thingId) {
		super(thingId);
		// TODO Auto-generated constructor stub
	}

	@Override public void run() {
		try {
			Ess ess = this.ess.value();
			// Calculate required sum values
			long calculatedApparentPower = (meter.value().currentL1.value() - currentOffset.value() / 3)
					* meter.value().voltageL1.value()
					+ (meter.value().currentL2.value() - currentOffset.value() / 3) * meter.value().voltageL2.value()
					+ (meter.value().currentL3.value() - currentOffset.value() / 3) * meter.value().voltageL3.value()
					+ ess.apparentPower.value();
			long maxChargePower = ess.setActivePower.writeMin().orElse(0L);
			long maxDischargePower = ess.setActivePower.writeMax().orElse(0L);
			long activePower = 0L;
			long reactivePower = 0L;
			if (ControllerUtils.isCharge(ess.activePower.value() + meter.value().getActivePower(),
					ess.reactivePower.value() + meter.value().getReactivePower())) {
				/*
				 * Charge
				 */
				if (calculatedApparentPower < maxChargePower) {
					calculatedApparentPower = maxChargePower;
				}
				activePower = ControllerUtils.calculateActivePowerFromApparentPower(calculatedApparentPower,
						cosPhi.value()) * -1;
				reactivePower = ControllerUtils.calculateReactivePower(activePower, cosPhi.value()) * -1;
			} else {
				/*
				 * Discharge
				 */
				if (calculatedApparentPower > maxDischargePower) {
					calculatedApparentPower = maxDischargePower;
				}
				activePower = ControllerUtils.calculateActivePowerFromApparentPower(calculatedApparentPower,
						cosPhi.value());
				reactivePower = ControllerUtils.calculateReactivePower(activePower, cosPhi.value());
			}
			ess.power.setActivePower(activePower);
			ess.power.setReactivePower(reactivePower);
			ess.power.writePower();
			log.info(ess.id() + " Set ActivePower [" + ess.power.getActivePower() + "], ReactivePower ["
					+ ess.power.getReactivePower() + "]");
		} catch (InvalidValueException e) {
			log.error(e.getMessage());
		}
	}

}
