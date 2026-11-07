package io.openems.edge.controller.evse.cluster.powerdistribute;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.evse.api.common.ApplySetPoint.Ability.EMPTY_APPLY_SET_POINT_ABILITY;
import static java.lang.Math.max;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import io.openems.common.timedata.DurationUnit;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.type.Phase;
import io.openems.edge.controller.evse.cluster.DistributionStrategy;
import io.openems.edge.controller.evse.cluster.LogVerbosity;
import io.openems.edge.controller.evse.cluster.powerdistribute.ramp.PowerDistributionRamp;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.controller.evse.single.EvseSingleState;
import io.openems.edge.controller.evse.single.Mode;
import io.openems.edge.controller.evse.single.Params;
import io.openems.edge.energy.api.handler.DifferentModes.Modes.JointModes;
import io.openems.edge.evse.api.chargepoint.Profile;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch;

public class EvsePowerDistributionManager {
	private final Logger log = LoggerFactory.getLogger(EvsePowerDistributionManager.class);

	private final List<Params> allParams;
	private final DistributionStrategy distributionStrategy;
	private final JointModes.JointMode<Mode> eshModes;
	private final PowerDistributionRamp ramp;
	private final LogVerbosity logVerbosity;

	private final Sum sum;
	private final Clock clock;

	protected static final DurationUnit DELAY_BETWEEN_SURPLUS_CHARGE_STATE_CHANGE = DurationUnit.ofMinutes(5);
	protected static final DurationUnit DELAY_BEFORE_CHARGE_PAUSE = DurationUnit.ofSeconds(60);
	protected static final DurationUnit DELAY_BEFORE_CHARGE_START = DurationUnit.ofSeconds(20);

	protected static final DurationUnit DURATION_TO_CHECK_FOR_INITIAL_CHARGE_SINGLE_PHASE_SWITCH = DurationUnit
			.ofSeconds(10);

	/**
	 * Max allowed change for increasing power/current. A value of 0.03 requires
	 * about 1 minute from 6 A to 32 A.
	 */
	public static final float MAX_PERCENTAGE_CHANGE_PER_SECOND = 0.03F;

	public EvsePowerDistributionManager(List<Params> params, DistributionStrategy distributionStrategy,
			JointModes.JointMode<Mode> eshModes, PowerDistributionRamp ramp, LogVerbosity logVerbosity, Sum sum,
			Clock clock) {
		this.allParams = params;
		this.distributionStrategy = distributionStrategy;
		this.eshModes = eshModes;
		this.ramp = ramp;
		this.logVerbosity = logVerbosity;
		this.sum = sum;
		this.clock = clock;
	}

	/**
	 * Runs power distribution for all EVSEs and applies resulting actions and modes
	 * to matching controllers.
	 *
	 * @param ctrls the EVSE single controllers to update
	 */
	public void runAndApply(List<ControllerEvseSingle> ctrls) {
		var results = this.run(ctrls);
		for (var result : results) {
			result.ctrl().apply(result.mode(), result.actions());
		}
	}

	/**
	 * Runs power distribution for all EVSEs and returns resulting actions and modes
	 * to matching controllers.
	 *
	 * @param ctrls the EVSE single controllers
	 * @return Returns the calculation results for each controller, including the
	 *         calculated mode and actions to apply.
	 */
	public List<CalculationResult> run(List<ControllerEvseSingle> ctrls) {
		final var distributionResults = this.runPowerDistribution().stream()
				.collect(Collectors.toMap(x -> x.params().ctrlSingleId(), x -> x));

		final var resultBuilder = ImmutableList.<CalculationResult>builder();
		for (var ctrl : ctrls) {
			var distributionResult = distributionResults.get(ctrl.id());
			if (distributionResult == null) {
				continue;
			}

			final var actionsBuilder = Profile.ChargePointActions
					.from(distributionResult.params().combinedAbilities().chargePointAbilities());
			final var applyHelper = new EvsePowerDistributionApplyHandler(distributionResult, ctrl,
					this.clock.instant(), actionsBuilder, this.logVerbosity);

			applyHelper.applyActions();
			applyHelper.startChargeWithPhaseChangeIfRequired(this::canStartSurplusChargingWith1Phase);

			resultBuilder.add(new CalculationResult(ctrl, this.calculateMode(distributionResult.params()),
					actionsBuilder.build()));
		}

		return resultBuilder.build();
	}

	/**
	 * Runs the configured set-point power distribution and calculates the ideal
	 * set-point distribution.
	 *
	 * @return ordered distribution results per EVSE parameter set
	 */
	public ImmutableList<PowerDistributionResult> runPowerDistribution() {
		final var setPointEntries = mapEntriesToMap(this.runSetPointPowerDistribution());
		final var idealSetPointEntries = mapEntriesToMap(
				this.runPowerDistribution(this.createEntriesForIdealSetPoint()));

		final var result = ImmutableList.<PowerDistributionResult>builder();
		for (var params : this.allParams) {
			result.add(new PowerDistributionResult(params, //
					setPointEntries.get(params.ctrlSingleId()).setPointInWatt, //
					idealSetPointEntries.get(params.ctrlSingleId()).setPointInWatt //
			));
		}
		return result.build();
	}

	private PowerDistributionEntries runPowerDistribution(PowerDistributionEntries entries) {
		var powerDistribution = new PowerDistribution(entries, this.distributionStrategy,
				this.calculateTotalExcessPower(entries));
		powerDistribution.distributeSurplusPower();

		return entries;
	}

	private static Map<String, PowerDistributionEntry> mapEntriesToMap(PowerDistributionEntries entries) {
		return entries.getAllEntries().stream() //
				.collect(Collectors.toMap(//
						e -> ((PowerDistributionKey.Evse) e.key).ctrlSingleId(), //
						e -> e));
	}

	private PowerDistributionEntries runSetPointPowerDistribution() {
		var entries = this.createEntriesForSetPoint();

		var powerDistribution = new PowerDistribution(entries, this.distributionStrategy,
				this.calculateTotalExcessPower(entries));

		powerDistribution.distributeSurplusPower();
		if (this.ramp != null) {
			powerDistribution.applyRamp(this.ramp);
			powerDistribution.storeLastSetPointsInRamp(this.ramp);
		}

		return entries;
	}

	private int calculateTotalExcessPower(PowerDistributionEntries entries) {
		final var buyFromGrid = this.sum.getGridActivePower().orElse(0);
		final var essDischarge = this.sum.getEssDischargePower().orElse(0);
		final var evseCharge = entries.getTotalActivePower();

		return max(0, evseCharge - buyFromGrid - essDischarge);
	}

	protected PowerDistributionEntries createEntriesForSetPoint() {
		var entries = this.allParams.stream() //
				.map(this::mapDistributionEntryForSetPoint) //
				.collect(ImmutableList.toImmutableList());
		return new PowerDistributionEntries(entries);
	}

	protected PowerDistributionEntry mapDistributionEntryForSetPoint(Params params) {
		return new PowerDistributionEntry(new PowerDistributionKey.Evse(params.ctrlSingleId()),
				mapDistributionStatus(params), this.applyOnOffDelayToMode(params), params.activePower(),
				params.combinedAbilities().applySetPoint());
	}

	protected PowerDistributionEntries createEntriesForIdealSetPoint() {
		var entries = this.allParams.stream() //
				.map(this::mapDistributionEntryForIdealSetPoint) //
				.collect(ImmutableList.toImmutableList());
		return new PowerDistributionEntries(entries);
	}

	protected PowerDistributionEntry mapDistributionEntryForIdealSetPoint(Params params) {
		return new PowerDistributionEntry(new PowerDistributionKey.Evse(params.ctrlSingleId()),
				mapDistributionStatus(params), this.calculateMode(params), params.activePower(),
				params.createApplySetPointForAllPhases());
	}

	protected static PowerDistributionEntry.Status mapDistributionStatus(Params params) {
		if (!params.combinedAbilities().isReadyForCharging()) {
			return PowerDistributionEntry.Status.NOT_AVAILABLE;
		} else if (params.history().getAppearsToBeFullyCharged()) {
			return PowerDistributionEntry.Status.CANNOT_ACCEPT_ENERGY;
		} else {
			return PowerDistributionEntry.Status.ACTIVE;
		}
	}

	protected Mode applyOnOffDelayToMode(Params params) {
		var mode = this.calculateMode(params);
		if (mode == Mode.SURPLUS && params.state() != null) {
			switch (params.state()) {
			case EvseSingleState.CHARGING -> {
				var blockReason = this.canPauseSurplusCharging(params);
				if (blockReason != null) {
					this.logTrace(params, "Charge pause is currently not allowed: " + blockReason);
					return Mode.MINIMUM;
				}
			}
			case EvseSingleState.CHARGE_PAUSED -> {
				var blockReason = this.canStartSurplusCharging(params);
				if (blockReason != null) {
					this.logTrace(params, "Charge start is currently not allowed: " + blockReason);
					return Mode.ZERO;
				}
			}
			default -> doNothing();
			}
		}

		return mode;
	}

	protected Mode calculateMode(Params params) {
		if (this.eshModes != null) {
			var eshMode = this.eshModes.getMode(params.ctrlSingleId());
			if (eshMode != null) {
				return eshMode;
			}
		}

		return params.mode();
	}

	private OptionalInt determinateMinChargePowerFor3Phase(Params params) {
		var currentApplySetPoint = params.combinedAbilities().applySetPoint();
		if (currentApplySetPoint.phase() == Phase.SingleOrThreePhase.THREE_PHASE) {
			return OptionalInt.of(currentApplySetPoint.toPower(currentApplySetPoint.min()));
		}

		final var phaseSwitch = params.combinedAbilities().phaseSwitch();
		if (phaseSwitch != null && phaseSwitch.direction() == ApplyPhaseSwitch.PhaseSwitchDirection.TO_THREE_PHASE
				&& phaseSwitch.oppositePhaseApplySetPoint() != null //
				&& !phaseSwitch.oppositePhaseApplySetPoint().equals(EMPTY_APPLY_SET_POINT_ABILITY)) {
			return OptionalInt.of(phaseSwitch.oppositePhaseApplySetPoint().toPower(//
					phaseSwitch.oppositePhaseApplySetPoint().min()));
		}

		return OptionalInt.empty();
	}

	private boolean canStartSurplusChargingWith1Phase(Params params) {
		final var min3PhasePower = this.determinateMinChargePowerFor3Phase(params);
		if (min3PhasePower.isEmpty()) {
			return false;
		}

		final int min3PhasePowerInt = min3PhasePower.getAsInt();
		return this.canStartSurplusCharging(params,
				DURATION_TO_CHECK_FOR_INITIAL_CHARGE_SINGLE_PHASE_SWITCH.getDuration(),
				x -> x != 0 && x <= min3PhasePowerInt) == null;
	}

	private BlockReason canStartSurplusCharging(Params params) {
		return this.canStartSurplusCharging(params, DELAY_BEFORE_CHARGE_START.getDuration(), x -> x != 0);
	}

	private BlockReason canStartSurplusCharging(Params params, Duration checkDuration,
			IntPredicate idealSetPointCheck) {
		var hysteresisActive = this.isHysteresisActive(params);
		if (hysteresisActive != null) {
			return hysteresisActive;
		}

		final var fromTime = this.clock.instant().minus(checkDuration);
		final var expectedFromTime = fromTime.plus(3, ChronoUnit.SECONDS);
		final var entries = params.history().getLastEntriesSince(fromTime);
		if (entries.isEmpty()) {
			return new BlockReason.NotEnoughHistoryEntries(0, null, fromTime);
		}
		if (!entries.getLast().time().isBefore(expectedFromTime)) {
			return new BlockReason.NotEnoughHistoryEntries(entries.size(), entries.getLast().time(), expectedFromTime);
		}

		final var last10SecondsTime = this.clock.instant().minus(10, ChronoUnit.SECONDS);
		if (!entries.stream().filter(e -> e.time().isAfter(last10SecondsTime))
				.allMatch(e -> idealSetPointCheck.test(e.idealSetPoint()))) {
			// There was no surplus in the last 10 seconds -> we should not start
			return new BlockReason.GotNoSurplusInLast10Seconds();
		}

		final var amountOfEntriesThatReachedThreshold = entries.stream()
				.filter(e -> idealSetPointCheck.test(e.idealSetPoint())).count();
		if (amountOfEntriesThatReachedThreshold < (entries.size() * 0.9)) {
			// Less than 90% of the entries have charge -> we should not start
			float reachedThresholdPercentage = (float) amountOfEntriesThatReachedThreshold / entries.size();
			return new BlockReason.ThresholdNotReached((int) Math.floor(reachedThresholdPercentage * 100), 90);
		}

		return null;
	}

	private BlockReason canPauseSurplusCharging(Params params) {
		var hysteresisActive = this.isHysteresisActive(params);
		if (hysteresisActive != null) {
			return hysteresisActive;
		}

		final var fromTime = this.clock.instant().minus(DELAY_BEFORE_CHARGE_PAUSE.getDuration());
		final var expectedFromTime = fromTime.plus(3, ChronoUnit.SECONDS);
		final var entries = params.history().getLastEntriesSince(fromTime);
		if (entries.isEmpty()) {
			return new BlockReason.NotEnoughHistoryEntries(0, null, fromTime);
		}
		if (!entries.getLast().time().isBefore(expectedFromTime)) {
			return new BlockReason.NotEnoughHistoryEntries(entries.size(), entries.getLast().time(), expectedFromTime);
		}

		final var last10SecondsTime = this.clock.instant().minus(10, ChronoUnit.SECONDS);
		if (!entries.stream().filter(e -> e.time().isAfter(last10SecondsTime)).allMatch(e -> e.idealSetPoint() == 0)) {
			// There was a surplus in the last 10 seconds -> we should not stop
			return new BlockReason.GotSurplusInLast10Seconds();
		}

		final var amountOfEntriesThatReachedThreshold = entries.stream().filter(e -> e.idealSetPoint() == 0).count();
		if (amountOfEntriesThatReachedThreshold < (entries.size() * 0.9)) {
			// Less than 90% of the entries have no surplus -> we should not stop
			float reachedThresholdPercentage = (float) amountOfEntriesThatReachedThreshold / entries.size();
			return new BlockReason.ThresholdNotReached((int) Math.floor(reachedThresholdPercentage * 100), 90);
		}

		return null;
	}

	private BlockReason.HysteresisActive isHysteresisActive(Params params) {
		final var lastChargeStateChangeTriggeredBySurplus = params.history()
				.getLastChargeStateChangeTriggeredBySurplus();
		if (lastChargeStateChangeTriggeredBySurplus == null) {
			return null;
		}

		final var hysteresisEndTime = lastChargeStateChangeTriggeredBySurplus
				.plus(DELAY_BETWEEN_SURPLUS_CHARGE_STATE_CHANGE.getDuration());
		if (hysteresisEndTime.isAfter(this.clock.instant())) {
			return new BlockReason.HysteresisActive(
					(int) Duration.between(this.clock.instant(), hysteresisEndTime).toSeconds());
		}

		return null;
	}

	private void logTrace(Params params, String msg) {
		if (this.logVerbosity == LogVerbosity.TRACE) {
			this.log.info("{}: {}", params.ctrlSingleId(), msg);
		}
	}

	protected sealed interface BlockReason {

		record HysteresisActive(int remainingSeconds) implements BlockReason {
		}

		record NotEnoughHistoryEntries(int amount, Instant oldestTimeInHistory, Instant expectedFromTime)
				implements BlockReason {
		}

		record GotSurplusInLast10Seconds() implements BlockReason {
		}

		record GotNoSurplusInLast10Seconds() implements BlockReason {
		}

		record ThresholdNotReached(int reachedThreshold, int expectedThreshold) implements BlockReason {
		}
	}

	public record CalculationResult(ControllerEvseSingle ctrl, Mode mode, Profile.ChargePointActions actions) {
	}

	interface CanStartSurplusChargeOptions {
		Duration getCheckDuration();

	}
}
