package io.openems.edge.controller.ess.cycle.statemachine;

import java.time.Clock;
import java.time.LocalDateTime;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.controller.ess.cycle.Config;
import io.openems.edge.controller.ess.cycle.ControllerEssCycle;
import io.openems.edge.controller.ess.cycle.HybridEssMode;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class Context extends AbstractContext<ControllerEssCycle> {

	protected final Config config;
	protected final ManagedSymmetricEss ess;
	protected final int allowedChargePower;
	protected final int allowedDischargePower;
	protected final Clock clock;
	protected final LocalDateTime parsedStartTime;

	private static LocalDateTime lastStateChangeTime;

	public Context(ControllerEssCycle parent, Config config, Clock clock, ManagedSymmetricEss ess,
			int allowedChargePower, int allowedDischargePower, LocalDateTime parsedStartTime) {
		super(parent);
		this.config = config;
		this.clock = clock;
		this.ess = ess;
		this.allowedChargePower = allowedChargePower;
		this.allowedDischargePower = allowedDischargePower;
		this.parsedStartTime = parsedStartTime;
	}

	/**
	 * Helper for a state change.
	 * 
	 * @param nextState which will be switched to.
	 * @return {@link State#WAIT_FOR_STATE_CHANGE}.
	 */
	protected State changeToNextState(State nextState) {
		this.getParent().setNextState(nextState);
		return State.WAIT_FOR_STATE_CHANGE;
	}

	/**
	 * Gets the required AC power set-point for AC- or Hybrid-ESS.
	 * 
	 * @param ess           the {@link ManagedSymmetricEss}; checked for
	 *                      {@link HybridEss}
	 * @param hybridEssMode the {@link HybridEssMode}
	 * @param power         the configured target power
	 * @return the AC power set-point
	 */
	protected int getAcPower(ManagedSymmetricEss ess, HybridEssMode hybridEssMode, int power) {
		return switch (this.config.hybridEssMode()) {
		case TARGET_AC -> this.config.power();
		case TARGET_DC -> {
			if (ess instanceof HybridEss) {
				var pv = ess.getActivePower().orElse(0) - ((HybridEss) ess).getDcDischargePower().orElse(0);
				yield pv + this.config.power();
			}
			yield this.config.power();
		}
		};
	}

	/**
	 * Helper to activate the full cycle activated or not.
	 * 
	 * @return true if the Controller should be executed now
	 * @throws OpenemsNamedException on error.
	 */
	public boolean initializeTime() {
		var now = LocalDateTime.now(this.clock);
		var afterNow = now.isAfter(this.parsedStartTime.minusSeconds(1));
		var beforeNow = now.isBefore(this.parsedStartTime.plusSeconds(59));
		if (afterNow && beforeNow) {
			return true;
		}
		return false;
	}

	/**
	 * Is ESS SoC defined ?.
	 *
	 * <ul>
	 * <li>true - if {@link ChannelId#SOC} is defined.
	 * </ul>
	 * 
	 * @return true if SoC is defined.
	 */
	public boolean isEssSocDefined() {
		if (this.ess.getSoc().isDefined()) {
			return true;
		}
		return false;
	}

	/**
	 * Gets the time when {@link StateMachine} {@link State} changed.
	 * 
	 * @return {@link LocalDateTime} last state changed time.
	 */
	public static LocalDateTime getLastStateChangeTime() {
		return lastStateChangeTime;
	}

	/**
	 * Sets the time when {@link StateMachine} {@link State} changed.
	 *
	 * @param time {@link LocalDateTime} last state changed time.
	 */
	public static void setLastStateChangeTime(LocalDateTime time) {
		lastStateChangeTime = time;
	}
}