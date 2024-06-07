package io.openems.edge.evcs.keba.kecontact.state;

import io.openems.edge.evcs.keba.kecontact.EvcsKebaKeContact;
import io.openems.edge.evcs.keba.kecontact.EvcsKebaKeContactImpl;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.evcs.api.Phases;
import java.time.Instant;
import java.time.Duration;

/**
 * Handler for managing the phase switch states and operations.
 */
public class PhaseSwitchHandler {
	private State currentState;
	private State inaktivState;
	private State runningOnePhaseState;
	private State runningThreePhaseState;
	private State switchToOnePhaseState;
	private State switchToThreePhaseState;
	private EvcsKebaKeContactImpl context;
	private Instant lastPhaseChangeTime = Instant.MIN;
	private static final long PHASE_SWITCH_COOLDOWN_SECONDS = 310;

	/**
	 * Constructor for PhaseSwitchHandler.
	 * 
	 * @param context The context for the EVCS.
	 */
	public PhaseSwitchHandler(EvcsKebaKeContactImpl context) {
		this.context = context;
		this.inaktivState = new Inaktiv();
		this.runningOnePhaseState = new RunningOnePhase();
		this.runningThreePhaseState = new RunningThreePhase();
		this.switchToOnePhaseState = new SwitchToOnePhase();
		this.switchToThreePhaseState = new SwitchToThreePhase();
		this.currentState = this.inaktivState; // Initial state
	}

	/**
	 * Applies the power and manages the state transitions.
	 * 
	 * @param power The power to be applied.
	 */
	public void applyPower(int power) {
		if (this.isCooldownPeriod()) {
			Duration timeUntilNextSwitch = Duration.between(Instant.now(),
					this.lastPhaseChangeTime.plusSeconds(PHASE_SWITCH_COOLDOWN_SECONDS));
			long secondsUntilNextSwitch = timeUntilNextSwitch.getSeconds();

			this.context.channel(EvcsKebaKeContact.ChannelId.PHASE_SWITCH_COOLDOWN)
					.setNextValue(secondsUntilNextSwitch);
			this.context.log.info("Phase switch cooldown period has not passed. Time before next switch: "
					+ secondsUntilNextSwitch + " seconds.");
		} else {
			final var phases = this.context.getPhases();
			IntegerReadChannel maxCurrentCannel = this.context.channel(EvcsKebaKeContact.ChannelId.DIP_SWITCH_MAX_HW);
			var preferredPhases = Phases.preferredPhaseBehavior(power, phases, this.context.getMinHwCurrent(),
					maxCurrentCannel.value().orElse(EvcsKebaKeContactImpl.DEFAULT_MAXIMUM_HARDWARE_CURRENT));

			if (phases != preferredPhases) {
				this.currentState = (preferredPhases == Phases.ONE_PHASE) ? this.switchToOnePhaseState
						: this.switchToThreePhaseState;
				this.currentState.switchPhase(this.context);
			} else {
				this.currentState.handlePower(power, this.context);
			}
		}
	}

	/**
	 * Switches the phase.
	 */
	public void switchPhase() {
		this.currentState.switchPhase(this.context);
	}

	/**
	 * Sets the current state.
	 * 
	 * @param state The state to be set.
	 */
	public void setState(State state) {
		this.currentState = state;
	}

	public State getSwitchToOnePhaseState() {
		return this.switchToOnePhaseState;
	}

	public State getSwitchToThreePhaseState() {
		return this.switchToThreePhaseState;
	}

	public State getRunningOnePhaseState() {
		return this.runningOnePhaseState;
	}

	public State getRunningThreePhaseState() {
		return this.runningThreePhaseState;
	}

	public State getInaktivState() {
		return this.inaktivState;
	}

	private boolean isCooldownPeriod() {
		Instant now = Instant.now();
		return this.lastPhaseChangeTime.plusSeconds(PHASE_SWITCH_COOLDOWN_SECONDS).isAfter(now);
	}

	/**
	 * Updates the phase change time.
	 */
	public void updatePhaseChangeTime() {
		this.lastPhaseChangeTime = Instant.now();
	}

	/**
	 * Handles the phase switch based on the preferred phases.
	 * 
	 * @param preferredPhases The preferred phases.
	 */
	public void handlePhaseSwitch(Phases preferredPhases) {

		if (!this.context.phaseSwitchActive()) {
			this.context.log.info("Phase switch not active.");
			return;
		}

		if (preferredPhases == Phases.TWO_PHASE) {
			// Set KEBA to two phases is not possible
			preferredPhases = Phases.THREE_PHASE;
		}
		String command = preferredPhases == Phases.ONE_PHASE ? "x2 0" : "x2 1";
		if (this.context.send(command)) {
			this.updatePhaseChangeTime(); // Update the cooldown timer regardless of the phase switch direction
			this.context.log.info(
					"Switched to " + (preferredPhases == Phases.ONE_PHASE ? "1 phase" : "3 phases") + " successfully.");
		} else {
			this.context.log.warn(
					"Failed to switch to " + (preferredPhases == Phases.ONE_PHASE ? "1 phase" : "3 phases") + ".");
		}
	}
}
