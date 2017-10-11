package io.openems.impl.controller.asymmetric.phaserectification;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.utilities.AsymmetricPower.ReductionType;
import io.openems.core.utilities.AvgFiFoQueue;

@ThingInfo(title = "PhaseRectificationActivePowerController", description = "Sets the ess to the required activepower to get all three phases on the meter to the same level.")
public class PhaseRectificationActivePowerController extends Controller {

	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ChannelInfo(title = "Grid-Meter", description = "Sets the grid meter.", type = Meter.class)
	public ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this);

	private AvgFiFoQueue meterL1 = new AvgFiFoQueue(2, 1.5);
	private AvgFiFoQueue meterL2 = new AvgFiFoQueue(2, 1.5);
	private AvgFiFoQueue meterL3 = new AvgFiFoQueue(2, 1.5);
	private AvgFiFoQueue essL1 = new AvgFiFoQueue(2, 1.5);
	private AvgFiFoQueue essL2 = new AvgFiFoQueue(2, 1.5);
	private AvgFiFoQueue essL3 = new AvgFiFoQueue(2, 1.5);

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

			meterL1.add(meter.activePowerL1.value() * -1);
			meterL2.add(meter.activePowerL2.value() * -1);
			meterL3.add(meter.activePowerL3.value() * -1);
			this.essL1.add(ess.activePowerL1.value());
			this.essL2.add(ess.activePowerL2.value());
			this.essL3.add(ess.activePowerL3.value());
			long essL1 = this.essL1.avg();
			long essL2 = this.essL2.avg();
			long essL3 = this.essL3.avg();
			long essPowerAvg = (essL1 + essL2 + essL3) / 3;
			essL1 -= essPowerAvg;
			essL2 -= essPowerAvg;
			essL3 -= essPowerAvg;
			long meterPowerAvg = (meterL1.avg() + meterL2.avg() + meterL3.avg()) / 3;
			long meterL1Delta = meterPowerAvg - meterL1.avg();
			long meterL2Delta = meterPowerAvg - meterL2.avg();
			long meterL3Delta = meterPowerAvg - meterL3.avg();
			long activePowerL1 = essL1 + meterL1Delta;
			long activePowerL2 = essL2 + meterL2Delta;
			long activePowerL3 = essL3 + meterL3Delta;
			ess.power.setActivePower(activePowerL1, activePowerL2, activePowerL3);
			ess.power.writePower(ReductionType.PERSUM);
		} catch (InvalidValueException e) {
			log.error("can't read value", e);
		} catch (WriteChannelException e) {
			log.warn("write failed.", e);
		}
	}

}
