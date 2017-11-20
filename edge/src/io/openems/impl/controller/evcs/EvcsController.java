package io.openems.impl.controller.evcs;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.utilities.AvgFiFoQueue;

@ThingInfo(title = "Electric Vehicle Charging Station control", description = "Controls an EVCS for optimized energy self-consumption.")
public class EvcsController extends Controller {
	private final AvgFiFoQueue sellToGridPowerQueue = new AvgFiFoQueue(30, 1.5);
	private final AvgFiFoQueue essActivePowerQueue = new AvgFiFoQueue(30, 1.5);

	/*
	 * Constructors
	 */
	public EvcsController() {
		super();
	}

	public EvcsController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Evcs", description = "Sets the EVCS device.", type = Evcs.class)
	public ConfigChannel<Evcs> evcs = new ConfigChannel<Evcs>("evcs", this);

	@ChannelInfo(title = "Ess", description = "Sets the Ess.", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ChannelInfo(title = "Grid-Meter", description = "Sets the grid meter.", type = Meter.class)
	public ConfigChannel<Meter> meter = new ConfigChannel<Meter>("meter", this);

	@Override
	protected void run() {
		// get sell-to-grid power
		try {
			this.sellToGridPowerQueue.add(this.meter.value().getActivePowerSum());
			this.essActivePowerQueue.add(this.ess.value().getActivePowerSum());
		} catch (InvalidValueException e) {
			log.error(e.getMessage());
		}

		// calculate excess power
		long sellToGridPower = this.sellToGridPowerQueue.avg();
		long essActivePower = this.essActivePowerQueue.avg();
		if (sellToGridPower > 0 /* Buying from grid */ || essActivePower > 0 /* Discharging */) {
			return;
		}
		long excessPower = Math.abs(sellToGridPower) + Math.abs(essActivePower);

		// set evcs charging current
		int currentMilliAmp = (int) Math.round((excessPower / 692.820323) * 1000);
		try {
			log.info("Set EVCS to [" + currentMilliAmp + "mA]");
			this.evcs.value().setCurrent.pushWrite(currentMilliAmp);
		} catch (WriteChannelException | InvalidValueException e) {
			log.error("Unable to set EVCS current: " + e.getMessage());
		}
	}
}
