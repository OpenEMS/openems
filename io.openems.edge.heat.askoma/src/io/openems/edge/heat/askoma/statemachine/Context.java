package io.openems.edge.heat.askoma.statemachine;

import java.time.Clock;

import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.heat.askoma.Config;
import io.openems.edge.heat.askoma.HeatAskomaImpl;

public class Context extends AbstractContext<HeatAskomaImpl> {

	protected final Config config;
	protected final Clock clock;
	protected final Sum sum;

	public Context(HeatAskomaImpl parent, Config config, Clock clock, Sum sum) {
		super(parent);
		this.config = config;
		this.clock = clock;
		this.sum = sum;
	}

	/**
	 * Sets the target active power on the heat element. Delegates to
	 * {@link HeatAskomaImpl#setTargetActivePowerForHeatElement(Integer)}.
	 *
	 * @param power the requested power in watts
	 */
	public void setTargetActivePowerForHeatElement(int power) {
		this.getParent().setTargetActivePowerForHeatElement(power);
	}

	public Integer getRequestedTargetGridActivePower() {
		return this.getParent().getRequestedTargetGridActivePower();
	}

	public Integer getActivePower() {
		return this.getParent().getActivePower().get();
	}

	/**
	 * Returns the current grid active power in watts, defaulting to 0 if undefined.
	 *
	 * @return grid active power [W]
	 */
	public int getGridActivePower() {
		return this.sum.getGridActivePower().orElse(0);
	}

	/**
	 * Returns the configured maximum heat power [W].
	 *
	 * @return max heat power [W]
	 */
	public int getMaxHeatPower() {
		return this.config.maxHeatPower();
	}

	public void setFastHeatPowerNotApplied(boolean active) {
		this.getParent().setFastHeatPowerNotApplied(active);
	}
}
