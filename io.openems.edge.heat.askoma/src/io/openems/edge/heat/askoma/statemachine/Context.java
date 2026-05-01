package io.openems.edge.heat.askoma.statemachine;

import static io.openems.edge.heat.askoma.statemachine.AskomaConstants.FAST_HEAT_DURATION;
import static io.openems.edge.heat.askoma.statemachine.AskomaConstants.FAST_HEAT_PAUSE;

import java.time.Clock;
import java.time.Instant;

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

	// --- fast-heat timing helpers (state persisted on HeatAskomaImpl) ---
	/**
	 * Returns the instant when the current fast-heat window started, or
	 * {@code null} if not yet started.
	 *
	 * @return fast-heat start instant or null
	 */
	public Instant getFastHeatStartedAt() {
		return this.getParent().getFastHeatStartedAt();
	}

	/**
	 * Sets the fast-heat window start instant.
	 *
	 * @param instant the start instant, or null to clear
	 */
	public void setFastHeatStartedAt(Instant instant) {
		this.getParent().setFastHeatStartedAt(instant);
	}

	public Instant getFastHeatPowerNotAppliedSince() {
		return this.getParent().getFastHeatPowerNotAppliedSince();
	}

	public void setFastHeatPowerNotAppliedSince(Instant instant) {
		this.getParent().setFastHeatPowerNotAppliedSince(instant);
	}

	/**
	 * Returns the instant when the current fast-heat safety pause started, or
	 * {@code null} if no pause is active.
	 *
	 * @return pause start instant or null
	 */
	public Instant getFastHeatPauseStartedAt() {
		return this.getParent().getFastHeatPauseStartedAt();
	}

	/**
	 * Sets the fast-heat pause start instant.
	 *
	 * @param instant the start instant, or null to clear
	 */
	public void setFastHeatPauseStartedAt(Instant instant) {
		this.getParent().setFastHeatPauseStartedAt(instant);
	}

	public void setFastHeatPowerNotApplied(boolean active) {
		this.getParent().setFastHeatPowerNotApplied(active);
	}

	/**
	 * Resets the FAST_HEAT power-not-applied tracking state.
	 */
	public void resetFastHeatPowerNotAppliedState() {
		this.getParent().setFastHeatPowerNotAppliedSince(null);
		this.getParent().setFastHeatPowerNotApplied(false);
	}

	/**
	 * Returns {@code true} when the fast-heat duration window has elapsed.
	 *
	 * @return true if the fast-heat window is expired
	 */
	public boolean isFastHeatExpired() {
		var startedAt = this.getFastHeatStartedAt();
		if (startedAt == null) {
			return false;
		}
		return !this.clock.instant().isBefore(startedAt.plus(FAST_HEAT_DURATION));
	}

	/**
	 * Returns {@code true} when the fast-heat safety pause has elapsed.
	 *
	 * @return true if the pause is expired
	 */
	public boolean isFastHeatPauseExpired() {
		var pauseStartedAt = this.getFastHeatPauseStartedAt();
		if (pauseStartedAt == null) {
			return false;
		}
		return !this.clock.instant().isBefore(pauseStartedAt.plus(FAST_HEAT_PAUSE));
	}

	/**
	 * Resets all fast-heat timing state (start instant and pause instant). Intended
	 * to be called when switching away from FAST_HEAT mode.
	 */
	public void resetFastHeatState() {
		this.getParent().setFastHeatStartedAt(null);
		this.getParent().setFastHeatPauseStartedAt(null);
		this.resetFastHeatPowerNotAppliedState();
	}
}
