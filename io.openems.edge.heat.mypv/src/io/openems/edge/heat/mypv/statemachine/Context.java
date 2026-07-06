package io.openems.edge.heat.mypv.statemachine;

import java.time.Clock;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.ChannelUtils;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.heat.api.ManagedHeatElement;
import io.openems.edge.heat.mypv.Config;
import io.openems.edge.heat.mypv.HeatMyPv;
import io.openems.edge.heat.mypv.HeatMyPvImpl;

public class Context extends AbstractContext<HeatMyPvImpl> {

	protected final Config config;
	protected final Clock clock;
	protected final Sum sum;

	public Context(HeatMyPvImpl parent, Config config, Clock clock, Sum sum) {
		super(parent);
		this.config = config;
		this.clock = clock;
		this.sum = sum;
	}

	/**
	 * Sets the target active power on the heat element. Delegates to
	 * {@link HeatMyPvImpl#setTargetActivePowerForHeatElement(Integer)}.
	 *
	 * @param power the requested power in watts
	 * @throws OpenemsNamedException on error
	 */
	public void setTargetActivePowerForHeatElement(int power) throws OpenemsNamedException {
		this.getParent().setTargetActivePowerForHeatElement(power);
	}

	/**
	 * Gets the currently requested TARGET_ACTIVE_POWER value.
	 *
	 * <p>
	 * Prefers a pending write-value of the current cycle and falls back to the last
	 * read channel value.
	 *
	 * @return the requested target active power in watts or null if undefined
	 */
	public Integer getRequestedTargetActivePower() {
		IntegerWriteChannel channel = this.getParent().channel(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER);
		return channel.getNextWriteValue().orElse(channel.value().get());
	}

	public int getActivePower() {
		// zero is needed otherwise it can produce NPE by arithmetic operations in the
		// state machine handlers
		return this.getParent().getActivePower().orElse(0);
	}

	/**
	 * Returns the current grid active power in watts, defaulting to 0 if undefined.
	 *
	 * @return grid active power [W]
	 */
	public int getGridActivePower() {
		if (this.sum == null) {
			return 0;
		}
		return this.sum.getGridActivePower().orElse(0);
	}

	/**
	 * Returns the current surplus setpoint in watts.
	 *
	 * <p>
	 * Grid feed-in is negative grid active power. The available surplus for the
	 * heating element is that feed-in plus the element's own active power, reduced
	 * by ESS discharge (heating has lower priority than battery charging). The
	 * result is clamped to {@code [0, maxHeatPower]}.
	 *
	 * @return surplus target active power [W]
	 */
	public int determineSurplusTargetActivePower() {
		return calculateSurplusTargetActivePower(//
				this.getGridActivePower(), //
				this.getActivePower(), //
				this.getEssDischargePower(), //
				this.config.maxHeatPower());
	}

	static int calculateSurplusTargetActivePower(//
			int gridActivePower, //
			int activePower, //
			int essDischargePower, //
			int maxHeatPower) { //
		// Heating element has lower priority than ESS: subtract ESS discharge from
		// available surplus.
		var surplusPower = Math.max(0, -gridActivePower + activePower);
		var essDischargeReduction = Math.max(0, essDischargePower);
		var resultPower = surplusPower - essDischargeReduction;
		return Math.min(resultPower, maxHeatPower);
	}

	private int getEssDischargePower() {
		if (this.sum == null) {
			return 0;
		}
		return this.sum.getEssDischargePower().orElse(0);
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
		ChannelUtils.setValue(this.getParent(), HeatMyPv.ChannelId.FAST_HEAT_POWER_NOT_APPLIED, active);
	}

}
