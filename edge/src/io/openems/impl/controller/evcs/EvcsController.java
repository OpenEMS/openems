package io.openems.impl.controller.evcs;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.common.session.Role;
import io.openems.core.utilities.AvgFiFoQueue;

@ThingInfo(title = "Electric Vehicle Charging Station control", description = "Controls an EVCS for optimized energy self-consumption.")
public class EvcsController extends Controller {

	private final Logger log = LoggerFactory.getLogger(EvcsController.class);

	/**
	 * Delay adjustment of EVCS current to CONTROL_LAG cycles
	 */
	private final int CONTROL_LAG = 300; // every 5 minutes
	private final int WAIT_FOR_VALUE_SET = 5;

	private final int DEFAULT_MIN_CURRENT = 6000;
	private final boolean DEFAULT_FORCE_CHARGE = false;

	/**
	 * Calculate average Grid Active Power
	 *
	 * <ul>
	 * <li>positive: buy-from-grid
	 * <li>negative: sell-to-grid
	 * </ul>
	 */
	private final AvgFiFoQueue gridActivePowerQueue = new AvgFiFoQueue(CONTROL_LAG, 1.5);
	/**
	 * Calculate average ESS Active Power
	 *
	 * <ul>
	 * <li>positive: discharge
	 * <li>negative: charge
	 * </ul>
	 */
	private final AvgFiFoQueue essActivePowerQueue = new AvgFiFoQueue(CONTROL_LAG, 1.5);
	private Optional<Integer> lastCurrentMilliAmp = Optional.empty();
	private int lagCountdown = 0;
	private int waitForValueSet = WAIT_FOR_VALUE_SET;
	private ThingStateChannels thingState = new ThingStateChannels(this);

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

	@ChannelInfo(title = "MinCurrent", description = "Sets the minimum current.", type = Integer.class, writeRoles = {
			Role.OWNER })
	public ConfigChannel<Integer> minCurrent = new ConfigChannel<Integer>("minCurrent", this)
	.defaultValue(DEFAULT_MIN_CURRENT).addChangeListener((channel, newValue, oldValue) -> {
		this.lagCountdown = 0; // force immediate action
	});

	@ChannelInfo(title = "ForceCharge", description = "Activates the force-charge mode.", type = Boolean.class, writeRoles = {
			Role.OWNER })
	public ConfigChannel<Boolean> forceCharge = new ConfigChannel<Boolean>("forceCharge", this)
	.defaultValue(DEFAULT_FORCE_CHARGE).addChangeListener((channel, newValue, oldValue) -> {
		this.lagCountdown = 0; // force immediate action
	});

	@Override
	protected void run() {
		Evcs evcs;
		try {
			evcs = this.evcs.value();
		} catch (InvalidValueException e1) {
			log.error("EVCS is not set. Aborting");
			return;
		}

		// get sell-to-grid power
		try {
			this.gridActivePowerQueue.add(this.meter.value().getActivePowerSum());
			this.essActivePowerQueue.add(this.ess.value().getActivePowerSum());
		} catch (InvalidValueException e) {
			log.error(e.getMessage());
		}

		// check if last currentTimeMilliAmp
		if (lastCurrentMilliAmp.isPresent() && evcs.userCurrent.valueOptional().isPresent()) {
			if (!evcs.userCurrent.valueOptional().get().equals(lastCurrentMilliAmp.get())) {
				// lastCurrentMilliAmp was not applied
				if (this.waitForValueSet-- <= 0) {
					this.waitForValueSet = WAIT_FOR_VALUE_SET;
					log.info("Retry setting EVCS current [" + lastCurrentMilliAmp.get() + "]");
					this.setCurrentMilliAmp(lastCurrentMilliAmp.get());
				}
			}
		}

		// did we wait long enough?
		if (this.lagCountdown-- <= 0) {
			// reset lag countdown
			this.lagCountdown = CONTROL_LAG;

			int currentMilliAmp;
			if (forceCharge.valueOptional().orElse(DEFAULT_FORCE_CHARGE)) {
				// set to max for KEBA
				currentMilliAmp = 63000;
			} else {
				// calculate excess power = feed-to-grid + ess charge
				long essChargePower = this.essActivePowerQueue.avg() * -1;
				long gridSellPower = this.gridActivePowerQueue.avg() * -1;
				long evcsCurrent = evcs.userCurrent.getValue();
				currentMilliAmp = (int) Math.round(
						// convert power to current
						((gridSellPower + essChargePower) / 692.820323) * 1000
						// add actual evcs charging current
						+ evcsCurrent);
				log.info("Calculation: essChargePower [" + essChargePower + "] + gridSellPower [" + gridSellPower
						+ "]) + userCurrent [" + evcsCurrent + "] = " + currentMilliAmp);
				if (currentMilliAmp < 0) {
					currentMilliAmp = 0;
				}

				// set evcs charging current
				if (currentMilliAmp < minCurrent.valueOptional().orElse(DEFAULT_MIN_CURRENT)) {
					currentMilliAmp = minCurrent.valueOptional().orElse(DEFAULT_MIN_CURRENT);
					log.info("Reset: currentMilliAmp [" + currentMilliAmp + "]");
				}
			}
			this.setCurrentMilliAmp(currentMilliAmp);
		}
	}

	private void setCurrentMilliAmp(int currentMilliAmp) {
		log.info("currentMilliAmp: " + currentMilliAmp);
		this.lastCurrentMilliAmp = Optional.ofNullable(currentMilliAmp);
		try {
			log.info("Set EVCS to [" + currentMilliAmp + "mA]");
			this.evcs.value().setCurrent.pushWrite(currentMilliAmp);
			if (currentMilliAmp == 0) {
				this.evcs.value().setEnabled.pushWrite(false);
			} else {
				this.evcs.value().setEnabled.pushWrite(true);
			}
		} catch (WriteChannelException | InvalidValueException e) {
			log.error("Unable to set EVCS current: " + e.getMessage());
		}
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}
}
