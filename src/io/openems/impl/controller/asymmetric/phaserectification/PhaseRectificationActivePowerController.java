package io.openems.impl.controller.asymmetric.phaserectification;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.InvalidValueException;

public class PhaseRectificationActivePowerController extends Controller {

	@ConfigInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ConfigInfo(title = "Grid-Meter", description = "Sets the grid meter.", type = Meter.class)
	public ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this);

	public PhaseRectificationActivePowerController() {
		super();
	}

	public PhaseRectificationActivePowerController(String thingId) {
		super(thingId);
	}

	@Override
	public void run() {
		try {
			Ess ess = this.ess.value();
			Meter meter = this.meter.value();
			long meterL1 = meter.activePowerL1.value() * -1;
			long meterL2 = meter.activePowerL2.value() * -1;
			long meterL3 = meter.activePowerL3.value() * -1;
			long essL1 = ess.activePowerL1.value();
			long essL2 = ess.activePowerL2.value();
			long essL3 = ess.activePowerL3.value();
			long essPowerAvg = (essL1 + essL2 + essL3) / 3;
			essL1 -= essPowerAvg;
			essL2 -= essPowerAvg;
			essL3 -= essPowerAvg;
			long meterPowerAvg = (meterL1 + meterL2 + meterL3) / 3;
			long meterL1Delta = meterPowerAvg - meterL1;
			long meterL2Delta = meterPowerAvg - meterL2;
			long meterL3Delta = meterPowerAvg - meterL3;
			long activePowerL1 = essL1 + meterL1Delta;
			long activePowerL2 = essL2 + meterL2Delta;
			long activePowerL3 = essL3 + meterL3Delta;
			ess.power.setActivePower(activePowerL1, activePowerL2, activePowerL3);
			ess.power.writePower();
		} catch (InvalidValueException e) {
			log.error("can't read value", e);
		}
	}

}
