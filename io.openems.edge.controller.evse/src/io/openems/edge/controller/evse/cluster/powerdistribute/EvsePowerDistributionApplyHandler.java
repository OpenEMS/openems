package io.openems.edge.controller.evse.cluster.powerdistribute;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.controller.evse.cluster.LogVerbosity.TRACE;
import static io.openems.edge.controller.evse.single.PhaseSwitching.AUTOMATIC_SINGLE_TO_THREE_PHASE_SWITCH_POWER;
import static io.openems.edge.controller.evse.single.PhaseSwitching.AUTOMATIC_THREE_TO_SINGLE_PHASE_SWITCH_POWER;
import static io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection.TO_SINGLE_PHASE;
import static io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection.TO_THREE_PHASE;
import static io.openems.edge.evse.api.common.ApplySetPoint.Ability.EMPTY_APPLY_SET_POINT_ABILITY;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.type.Phase;
import io.openems.edge.controller.evse.cluster.LogVerbosity;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.controller.evse.single.EvseSingleState;
import io.openems.edge.controller.evse.single.Params;
import io.openems.edge.controller.evse.single.PhaseSwitching;
import io.openems.edge.controller.evse.single.Types;
import io.openems.edge.evse.api.chargepoint.Profile;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch;

public record EvsePowerDistributionApplyHandler(PowerDistributionResult powerDistributionResult,
		ControllerEvseSingle ctrl, Instant now, Profile.ChargePointActions.Builder actionsBuilder,
		LogVerbosity logVerbosity) {

	private static Logger log = LoggerFactory.getLogger(EvsePowerDistributionApplyHandler.class);

	/**
	 * Window in which the automatic phase switch threshold must be met to trigger a
	 * phase switch. This is to prevent oscillation between single- and three-phase
	 * switching. For this duration the 90avg for the
	 * setPointInWattWithoutPhaseLimitation is checked.
	 */
	private static final Duration AUTOMATIC_PROBABLE_PHASE_SWITCH_WINDOW = Duration.ofSeconds(20);
	/**
	 * Delay for the EpochSecond probable phase switch evaluation. If a probable
	 * phase switch is detected the next switch is set to now plus this Duration.
	 */
	private static final Duration AUTOMATIC_PROBABLE_PHASE_SWITCH_DELAY = Duration.ofSeconds(100);

	/**
	 * If the charging station is currently in 3-phase state and we could start
	 * charging with 1-phase, this method applies a phase switch to 1-phase to start
	 * charging.
	 * 
	 * @param canStartCharging Predicate that calculates if it's okay to start
	 *                         charging now
	 */
	public void startChargeWithPhaseChangeIfRequired(Predicate<Params> canStartCharging) {
		if (!this.params().canCurrentlyDoPhaseSwitch() || this.params().phaseSwitching() != PhaseSwitching.AUTOMATIC) {
			return;
		}
		if (this.powerDistributionResult.params().state() != EvseSingleState.CHARGE_PAUSED) {
			return;
		}
		if (this.powerDistributionResult.setPointInWatt() != 0) {
			// We can charge already
			return;
		}
		if (this.powerDistributionResult.idealSetPointInWatt() == 0) {
			// We can not charge anyway
			return;
		}
		if (!canStartCharging.test(this.powerDistributionResult.params())) {
			// We are not allowed to start charging yet
			return;
		}

		this.logTrace("Initiate phase switch to start charging");
		this.applyAutoPhaseSwitchIfDirectionMatches(TO_SINGLE_PHASE);
	}

	/**
	 * Applies actions to actionsBuilder.
	 */
	public void applyActions() {
		this.applyPhaseSwitch();
		this.actionsBuilder.setCorrectApplySetPointByWatt(this.powerDistributionResult.setPointInWatt());
		this.actionsBuilder.setIdealSetPointInWatt(this.powerDistributionResult.idealSetPointInWatt());
	}

	private void applyPhaseSwitch() {
		this.cleanupOutdatedProbablePhaseSwitchTimestamp();

		if (this.powerDistributionResult.setPointInWatt() == 0) {
			return;
		}
		if (!this.params().canCurrentlyDoPhaseSwitch()) {
			return;
		}

		switch (this.params().phaseSwitching()) {
		case FORCE_SINGLE_PHASE ->
			this.applyPhaseSwitchIfDirectionMatches(ApplyPhaseSwitch.PhaseSwitchDirection.TO_SINGLE_PHASE);
		case FORCE_THREE_PHASE ->
			this.applyPhaseSwitchIfDirectionMatches(ApplyPhaseSwitch.PhaseSwitchDirection.TO_THREE_PHASE);
		case AUTOMATIC -> this.applyAutoPhaseSwitch();
		case DISABLE -> doNothing();
		}
	}

	private void applyAutoPhaseSwitch() {
		if (this.powerDistributionResult.idealSetPointInWatt() == 0) {
			return;
		}
		if (this.params().history().isAutomaticPhaseSwitchInCooldown(this.now())) {
			return;
		}

		final var phaseSwitchAbility = this.params().combinedAbilities().phaseSwitch();
		if (phaseSwitchAbility == null //
				|| phaseSwitchAbility.direction() == null //
				|| phaseSwitchAbility.oppositePhaseApplySetPoint() == null //
				|| phaseSwitchAbility.oppositePhaseApplySetPoint().equals(EMPTY_APPLY_SET_POINT_ABILITY)) {
			return;
		}

		switch (phaseSwitchAbility.direction()) {
		case TO_THREE_PHASE -> {
			var singleToThreeEvaluation = this.handleAutomaticSingleToThreePhaseSwitch();
			this.logAutomaticPhaseSwitchSummary(Phase.SingleOrThreePhase.THREE_PHASE, phaseSwitchAbility,
					singleToThreeEvaluation, null, null, false);
		}
		case TO_SINGLE_PHASE -> {
			final var threeToSingleResult = this.handleAutomaticThreeToSinglePhaseSwitch();
			this.logAutomaticPhaseSwitchSummary(Phase.SingleOrThreePhase.SINGLE_PHASE, phaseSwitchAbility, null,
					threeToSingleResult.evaluation, threeToSingleResult.singlePhaseFeasibilityEvaluation,
					threeToSingleResult.singlePhaseFeasibleInRecentWindow);
		}
		}
	}

	private boolean applyPhaseSwitchIfDirectionMatches(ApplyPhaseSwitch.PhaseSwitchDirection targetDirection) {
		final var phaseSwitchAbility = this.params().combinedAbilities().phaseSwitch();
		final var shouldBeCurrentPhase = targetDirection.getTargetPhase().getOppositePhase();

		if (phaseSwitchAbility != null && phaseSwitchAbility.direction() == targetDirection
				&& this.params().combinedAbilities().applySetPoint().phase() == shouldBeCurrentPhase) {
			this.actionsBuilder.setPhaseSwitch(phaseSwitchAbility);
			this.logTrace("Force switch to " + targetDirection.name() + " phase");
			setValue(this.ctrl, ControllerEvseSingle.ChannelId.PROBABLE_NEXT_PHASE_SWITCH_EPOCH_SECONDS, null);
			return true;
		}

		return false;
	}

	private void applyAutoPhaseSwitchIfDirectionMatches(ApplyPhaseSwitch.PhaseSwitchDirection targetDirection) {
		if (this.applyPhaseSwitchIfDirectionMatches(targetDirection)) {
			this.params().history().setAutomaticPhaseSwitchCooldown(this.now());
		}
	}

	private Types.History.AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation handleAutomaticSingleToThreePhaseSwitch() {
		final var singleToThreeEvaluation = this.params().history()
				.evaluateAutomaticPhaseSwitchSetPointWithoutPhaseLimitation(this.now(),
						this.powerDistributionResult.idealSetPointInWatt(),
						AUTOMATIC_SINGLE_TO_THREE_PHASE_SWITCH_POWER,
						Types.History.AutomaticPhaseSwitchThresholdDirection.ABOVE);

		final var shortWindowEvaluation = this.params().history()
				.evaluateAutomaticPhaseSwitchSetPointWithoutPhaseLimitationForWindow(this.now(),
						AUTOMATIC_SINGLE_TO_THREE_PHASE_SWITCH_POWER,
						Types.History.AutomaticPhaseSwitchThresholdDirection.ABOVE,
						AUTOMATIC_PROBABLE_PHASE_SWITCH_WINDOW, this.powerDistributionResult.idealSetPointInWatt());

		if (singleToThreeEvaluation.shouldSwitch()) {
			this.setProbableNextPhaseSwitchEpochSecondsIfUnset(shortWindowEvaluation);
			this.applyAutoPhaseSwitchIfDirectionMatches(TO_THREE_PHASE);
		}
		return singleToThreeEvaluation;
	}

	private ThreeToSinglePhaseSwitchResult handleAutomaticThreeToSinglePhaseSwitch() {
		final var threeToSingleEvaluation = this.params().history()
				.evaluateAutomaticPhaseSwitchSetPointWithoutPhaseLimitation(this.now(),
						this.powerDistributionResult.idealSetPointInWatt(),
						AUTOMATIC_THREE_TO_SINGLE_PHASE_SWITCH_POWER,
						Types.History.AutomaticPhaseSwitchThresholdDirection.BELOW);

		final var shortWindowEvaluation = this.params().history()
				.evaluateAutomaticPhaseSwitchSetPointWithoutPhaseLimitationForWindow(this.now(),
						AUTOMATIC_THREE_TO_SINGLE_PHASE_SWITCH_POWER,
						Types.History.AutomaticPhaseSwitchThresholdDirection.BELOW,
						AUTOMATIC_PROBABLE_PHASE_SWITCH_WINDOW, this.powerDistributionResult.idealSetPointInWatt());

		final var singlePhaseSetPoint = this.params().combinedAbilities().phaseSwitch().oppositePhaseApplySetPoint();
		final var singlePhaseMinInWatt = singlePhaseSetPoint.toPower(singlePhaseSetPoint.min());

		final var singlePhaseFeasibilityEvaluation = this.params().history()
				.evaluateAutomaticPhaseSwitchSetPointWithoutPhaseLimitationForWindow(this.now, singlePhaseMinInWatt,
						Types.History.AutomaticPhaseSwitchThresholdDirection.ABOVE,
						AUTOMATIC_PROBABLE_PHASE_SWITCH_WINDOW, this.powerDistributionResult.idealSetPointInWatt());

		final var singlePhaseFeasibleInRecentWindow = singlePhaseFeasibilityEvaluation.shouldSwitch();

		if (threeToSingleEvaluation.shouldSwitch() && singlePhaseFeasibleInRecentWindow) {
			this.setProbableNextPhaseSwitchEpochSecondsIfUnset(shortWindowEvaluation);
			this.applyAutoPhaseSwitchIfDirectionMatches(TO_SINGLE_PHASE);
		}

		return new ThreeToSinglePhaseSwitchResult(threeToSingleEvaluation, singlePhaseFeasibilityEvaluation,
				singlePhaseFeasibleInRecentWindow);
	}

	private record ThreeToSinglePhaseSwitchResult(
			Types.History.AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation evaluation,
			Types.History.AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation singlePhaseFeasibilityEvaluation,
			boolean singlePhaseFeasibleInRecentWindow) {
	}

	private void logAutomaticPhaseSwitchSummary(Phase.SingleOrThreePhase activePhase,
			ApplyPhaseSwitch phaseSwitchAbility,
			Types.History.AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation singleToThreeEvaluation,
			Types.History.AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation threeToSingleEvaluation,
			Types.History.AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation singlePhaseFeasibilityEvaluation,
			boolean singlePhaseFeasibleInRecentWindow) {

		final var cooldownUntil = this.params().history().getAutomaticPhaseSwitchCooldownUntil();
		final var cooldownActive = cooldownUntil != null && this.now().isBefore(cooldownUntil);
		final var probableNextSwitchEpochSeconds = this.getProbableNextPhaseSwitchEpochSeconds();

		this.logTrace("AutoPhaseSwitch " //
				+ "phase[" + activePhase + "] " //
				+ "dir[" + phaseSwitchAbility.direction() + "] " //
				+ "set[" + this.powerDistributionResult.setPointInWatt() + "W] " //
				+ "raw[" + this.powerDistributionResult.idealSetPointInWatt() + "W] " //
				+ "cooldown[" + cooldownActive + "] " //
				+ "probableNext[" + probableNextSwitchEpochSeconds + "] " //
				+ "1to3{" + formatAutomaticPhaseSwitchEvaluation(singleToThreeEvaluation) + "} " //
				+ "3to1{" + formatAutomaticPhaseSwitchEvaluation(threeToSingleEvaluation) + "} " //
				+ "1pFeasible{" + formatAutomaticPhaseSwitchEvaluation(singlePhaseFeasibilityEvaluation) + "} " //
				+ "1pFeasibleWindowReached[" + singlePhaseFeasibleInRecentWindow + "]");
	}

	private static String formatAutomaticPhaseSwitchEvaluation(
			Types.History.AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation evaluation) {
		if (evaluation == null) {
			return "n/a";
		}
		return "windowActive=" + evaluation.windowActive() //
				+ ",sampleCount=" + evaluation.sampleCount() //
				+ ",threshold=" + evaluation.thresholdInWatt() //
				+ ",avg90=" + Math.round(evaluation.directionalNinetyPercentAverage()) //
				+ ",percent=" + Math.round(evaluation.currentPercentage()) //
				+ ",switch=" + evaluation.shouldSwitch();
	}

	private boolean cleanupOutdatedProbablePhaseSwitchTimestamp() {
		final var probableNextSwitchEpochSeconds = this.getProbableNextPhaseSwitchEpochSeconds();
		if (probableNextSwitchEpochSeconds == null) {
			return false;
		}
		if (probableNextSwitchEpochSeconds > this.now().getEpochSecond()) {
			return false;
		}
		setValue(this.ctrl, ControllerEvseSingle.ChannelId.PROBABLE_NEXT_PHASE_SWITCH_EPOCH_SECONDS, null);
		return true;
	}

	private void setProbableNextPhaseSwitchEpochSecondsIfUnset(
			Types.History.AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation evaluation) {
		if (!evaluation.shouldSwitch()) {
			return;
		}
		if (this.getProbableNextPhaseSwitchEpochSeconds() != null) {
			return;
		}

		final var nextProbableSwitchEpochSeconds = this.now().plus(AUTOMATIC_PROBABLE_PHASE_SWITCH_DELAY)
				.getEpochSecond();
		setValue(this.ctrl, ControllerEvseSingle.ChannelId.PROBABLE_NEXT_PHASE_SWITCH_EPOCH_SECONDS,
				nextProbableSwitchEpochSeconds);
	}

	private Long getProbableNextPhaseSwitchEpochSeconds() {
		final var channel = this.ctrl.channel(ControllerEvseSingle.ChannelId.PROBABLE_NEXT_PHASE_SWITCH_EPOCH_SECONDS);
		final Long nextValue = (Long) channel.getNextValue().get();
		if (nextValue != null) {
			return nextValue;
		}
		return (Long) channel.value().get();
	}

	private Params params() {
		return this.powerDistributionResult.params();
	}

	private void logTrace(String msg) {
		if (this.logVerbosity == TRACE) {
			log.info("{}: {}", this.ctrl.id(), msg);
		}
	}

}
