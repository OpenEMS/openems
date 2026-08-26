package io.openems.edge.controller.evse.cluster;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.controller.evse.cluster.LogVerbosity.TRACE;
import static io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection.TO_SINGLE_PHASE;
import static io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection.TO_THREE_PHASE;
import static io.openems.edge.evse.api.common.ApplySetPoint.roundDownToPowerStep;
import static java.lang.Math.max;
import static java.util.stream.Collectors.joining;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.controller.evse.single.Mode;
import io.openems.edge.controller.evse.single.Params;
import io.openems.edge.controller.evse.single.PhaseSwitching;
import io.openems.edge.controller.evse.single.Types;
import io.openems.edge.controller.evse.single.Types.History.AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation;
import io.openems.edge.controller.evse.single.Types.History.AutomaticPhaseSwitchThresholdDirection;
import io.openems.edge.controller.evse.single.Types.Hysteresis;
import io.openems.edge.energy.api.handler.DifferentModes.Modes.JointModes.JointMode;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection;
import io.openems.edge.evse.api.common.ApplySetPoint;

public class RunUtils {

	/**
	 * Max allowed change for increasing power/current. Applied in
	 * {@link #applyChangeLimit(Clock, PowerDistribution)}. A value of 0.03 requires
	 * about 1 minute from 6 A to 32 A.
	 */
	private static final float MAX_PERCENTAGE_CHANGE_PER_SECOND = 0.03F;

	private static final int AUTOMATIC_SINGLE_TO_THREE_PHASE_SWITCH_POWER = 4100;
	private static final int AUTOMATIC_THREE_TO_SNGLE_PHASE_SWITCH_POWER = 3700;
	/**
	 * Window in which the automatic phase switch threshold must be met to trigger a
	 * phase switch. This is to prevent oscillation between single- and three-phase
	 * switching. For this duration the 90avg for the setPointWithoutPhaseLimitation
	 * is checked.
	 */
	private static final Duration AUTOMATIC_PROBABLE_PHASE_SWITCH_WINDOW = Duration.ofSeconds(20);
	private static final int AUTOMATIC_THREE_TO_SINGLE_PHASE_SWITCH_WINDOW_MIN_SAMPLE_COUNT = 20;
	/**
	 * Delay for the EpochSecond probable phase switch evaluation. If a probable
	 * phase switch is detected the next switch is set to now plus this Duration.
	 */
	private static final Duration AUTOMATIC_PROBABLE_PHASE_SWITCH_DELAY = Duration.ofSeconds(100);

	private RunUtils() {
	}

	/**
	 * Holds temporary calculations and power distribution among
	 * {@link ControllerEvseSingle}s.
	 */
	public static class PowerDistribution {

		/**
		 * Holds {@link PowerDistribution} for one single {@link ControllerEvseSingle}.
		 */
		public static class Entry {
			public final ControllerEvseSingle ctrl;
			public final Mode mode;
			public final Params params;
			public final Integer activePower;
			public final ChargePointActions.Builder actions;

			protected int setPointInWatt;
			protected int setPointWithoutPhaseLimitation;

			public Entry(JointMode<Mode> mode, ControllerEvseSingle ctrl, Params params) {
				this.ctrl = ctrl;
				this.params = params;

				this.activePower = params.activePower();
				this.actions = ChargePointActions.from(params.combinedAbilities().chargePointAbilities());

				this.mode = Optional.ofNullable(mode) //
						.map(sm -> sm.getMode(params.ctrlSingleId())) // Mode from EnergyScheduler
						.orElse(params.mode()); // Fallback to fixed Mode
			}

			@Override
			public final String toString() {
				return toStringHelper(Entry.class) //
						.addValue(this.params) //
						.add("activePower", this.activePower) //
						.add("setPointInWatt", this.setPointInWatt) //
						.add("actions", this.actions.getApplySetPoint() == null //
								? "UNDEFINED" //
								: this.actions.build()) //
						.toString();
			}

			private int fitSetPointWithinDistributionBounds(int setPoint) {
				if (this.isAutomaticPhaseSwitching()) {
					return this.fitAutomaticSetPointWithinDistributionBounds(setPoint);
				}

				// Non-automatic phase switchers use only current phase limits
				final var asp = this.params.combinedAbilities().applySetPoint();
				return asp.fitWithin(setPoint);
			}

			private int fitAutomaticSetPointWithinDistributionBounds(int setPoint) {
				final var applySetPointAbility = this.params.combinedAbilities().applySetPoint();
				final var currentPhaseMaxInWatt = applySetPointAbility.toPower(applySetPointAbility.max());
				final var nonNegativeSetPoint = Math.max(0, setPoint);

				final var phaseSwitchAbility = this.params.combinedAbilities().phaseSwitch();
				if (phaseSwitchAbility == null || phaseSwitchAbility.oppositePhaseApplySetPoint() == null
						|| phaseSwitchAbility.oppositePhaseApplySetPoint()
								.equals(ApplySetPoint.Ability.EMPTY_APPLY_SET_POINT_ABILITY)) {
					return Math.min(currentPhaseMaxInWatt, nonNegativeSetPoint);
				}

				final var oppositePhaseApplySetPoint = phaseSwitchAbility.oppositePhaseApplySetPoint();
				final var oppositePhaseMinInWatt = oppositePhaseApplySetPoint.min();
				final var maxSetPointInWatt = Math.max(currentPhaseMaxInWatt, oppositePhaseApplySetPoint.max());
				final var boundedSetPoint = Math.min(maxSetPointInWatt, nonNegativeSetPoint);

				// Prevent allocating inside the invalid phase-switch gap.
				if (boundedSetPoint > currentPhaseMaxInWatt && boundedSetPoint < oppositePhaseMinInWatt) {
					return currentPhaseMaxInWatt;
				}

				return boundedSetPoint;
			}

			private boolean isAutomaticPhaseSwitching() {
				return this.params.phaseSwitching() == PhaseSwitching.AUTOMATIC
						&& Optional.ofNullable(this.params.combinedAbilities().electricVehicleAbilities())
								.map(ev -> ev.canInterrupt()).orElse(false);
			}

			private int singlePhaseMinimum() {
				final var applySetPointAbility = this.params.combinedAbilities().applySetPoint();
				if (applySetPointAbility.phase() == SINGLE_PHASE) {
					return applySetPointAbility.toPower(applySetPointAbility.min());
				}

				final var phaseSwitchAbility = this.params.combinedAbilities().phaseSwitch();
				if (phaseSwitchAbility != null && phaseSwitchAbility.direction() == TO_SINGLE_PHASE
						&& phaseSwitchAbility.oppositePhaseApplySetPoint() != null
						&& !phaseSwitchAbility.oppositePhaseApplySetPoint()
								.equals(ApplySetPoint.Ability.EMPTY_APPLY_SET_POINT_ABILITY)) {
					return phaseSwitchAbility.oppositePhaseApplySetPoint().min();
				}

				return ApplySetPoint.MIN_POWER_SINGLE_PHASE;
			}
		}

		public final ImmutableList<Entry> entries;
		public final int totalActivePower;

		public PowerDistribution(ImmutableList<Entry> entries) {
			this.entries = entries;
			this.totalActivePower = this.streamWithParams() //
					.map(e -> e.params.activePower()) //
					.filter(Objects::nonNull) //
					.mapToInt(Integer::intValue) //
					.sum();
		}

		/**
		 * Stream all {@link Entry}s.
		 * 
		 * @return {@link Stream}
		 */
		public final Stream<Entry> streamEntries() {
			return this.entries.stream();
		}

		/**
		 * Stream all {@link Entry}s with non-null {@link Params}.
		 * 
		 * @return {@link Stream}
		 */
		public final Stream<Entry> streamWithParams() {
			return this.streamEntries() //
					.filter(e -> e.params != null && e.params.combinedAbilities().applySetPoint() != null);
		}

		/**
		 * Stream all {@link Entry}s with non-null {@link Params} which are ready for
		 * charging.
		 * 
		 * @return {@link Stream}
		 */
		public final Stream<Entry> streamActives() {
			return this.streamWithParams() //
					.filter(e -> e.params.combinedAbilities().isReadyForCharging()
							&& !e.params.appearsToBeFullyCharged());
		}

		/**
		 * Stream all {@link Entry}s with non-null {@link Params} which are NOT ready
		 * for charging.
		 * 
		 * @return {@link Stream}
		 */
		public final Stream<Entry> streamNonActives() {
			return this.streamWithParams() //
					.filter(e -> !e.params.combinedAbilities().isReadyForCharging()
							|| e.params.appearsToBeFullyCharged());
		}

		/**
		 * Stream all {@link Entry}s with non-null {@link Params} which are ready for
		 * charging and in {@link Mode#SURPLUS} mode.
		 * 
		 * @return {@link Stream}
		 */
		public final Stream<Entry> streamSurplus() {
			return this.streamActives() //
					.filter(e -> switch (e.mode) {
					case FORCE, MINIMUM, ZERO -> false;
					case SURPLUS -> true;
					});
		}

		/**
		 * Stream all {@link Entry}s with non-null {@link Params} which are ready for
		 * charging and in {@link Mode#SURPLUS} or {@link Mode#MINIMUM} mode.
		 * 
		 * @return {@link Stream}
		 */
		public final Stream<Entry> streamSurplusOrMinimum() {
			return this.streamActives() //
					.filter(e -> switch (e.mode) {
					case FORCE, ZERO -> false;
					case SURPLUS, MINIMUM -> true;
					});
		}

		/**
		 * Stream all {@link Entry}s with non-null {@link Params} which are ready for
		 * charging and in {@link Mode#SURPLUS} mode and have a temporary Set-Point > 0.
		 * 
		 * @return {@link Stream}
		 */
		public final Stream<Entry> streamSurplusGreaterZero() {
			return this.streamSurplusOrMinimum()
					// Only the ones that are at least min() after distributeSurplusMinPower()
					.filter(e -> e.setPointInWatt > 0);
		}

		@Override
		public final String toString() {
			return toStringHelper(PowerDistribution.class) //
					.add("totalActivePower", this.totalActivePower) //
					.add("entries", "\n" + this.entries.stream() //
							.map(Entry::toString) //
							.collect(joining("\n"))) //
					.toString();
		}
	}

	/**
	 * Calculate the {@link PowerDistribution} according to the given
	 * {@link DistributionStrategy}.
	 * 
	 * @param clock                the {@link Clock}
	 * @param distributionStrategy the {@link DistributionStrategy}
	 * @param sum                  the {@link Sum} component
	 * @param ctrls                the list of {@link ControllerEvseSingle}
	 * @param mode                 the {@link JointMode} from
	 *                             {@link EnergyScheduleHandler}
	 * @param logVerbosity         the configured {@link LogVerbosity}
	 * @param logger               a log message consumer
	 * @return the {@link PowerDistribution}
	 */
	protected static PowerDistribution calculate(Clock clock, DistributionStrategy distributionStrategy, Sum sum,
			List<ControllerEvseSingle> ctrls, JointMode<Mode> mode, LogVerbosity logVerbosity,
			Consumer<String> logger) {
		// Build PowerDistribution
		var powerDistribution = new PowerDistribution(ctrls.stream() //
				.map(ctrl -> {
					var params = ctrl.getParams();
					if (params == null) {
						return null;
					}
					return new PowerDistribution.Entry(mode, ctrl, params);
				}) //
				.filter(Objects::nonNull) //
				.collect(toImmutableList()));

		initializeSetPoints(powerDistribution);
		distributeSurplusPower(powerDistribution, distributionStrategy, sum);
		permitNonActives(powerDistribution);
		applyChangeLimit(clock, powerDistribution);

		// Build Actions
		powerDistribution.streamWithParams().forEach(e -> {
			handleApplySetPoint(e, logVerbosity, logger);
			handlePhaseSwitch(e, clock, logVerbosity, logger);
		});

		return powerDistribution;
	}

	/**
	 * Initialize the Set-Points for {@link Mode#FORCE}, {@link Mode#MINIMUM} and
	 * {@link Mode#ZERO}.
	 * 
	 * @param powerDistribution the {@link PowerDistribution}
	 */
	private static void initializeSetPoints(PowerDistribution powerDistribution) {
		powerDistribution.streamActives().forEach(e -> {
			var asp = e.params.combinedAbilities().applySetPoint();
			if (e.mode == Mode.MINIMUM && e.isAutomaticPhaseSwitching()) {
				e.setPointInWatt = 0;
				return;
			}
			e.setPointInWatt = switch (e.mode) {
			case MINIMUM -> asp.min();
			case FORCE -> asp.max();
			case SURPLUS, ZERO -> 0;
			};
		});
	}

	/**
	 * Distribute excess power to Controllers in {@link Mode#SURPLUS} mode with an
	 * explicit total distributable power value.
	 *
	 * <p>
	 * This overload is package-private to allow tests to inject a fixed
	 * {@code totalDistributablePower} instead of deriving it from {@link Sum}.
	 *
	 * @param powerDistribution    the {@link PowerDistribution}
	 * @param sum                  the {@link Sum} component
	 * @param distributionStrategy the {@link DistributionStrategy}
	 */
	static void distributeSurplusPower(PowerDistribution powerDistribution, DistributionStrategy distributionStrategy,
			Sum sum) {
		var totalExcessPower = calculateTotalExcessPower(powerDistribution, sum);
		var totalFixedPower = powerDistribution.streamActives() //
				.mapToInt(e -> e.setPointInWatt) // initialized before via initializeSetPoints()
				.sum();
		var totalDistributablePower = Math.max(0, totalExcessPower - totalFixedPower);
		var remainingDistributablePower = distributeSurplusMinPower(powerDistribution, totalDistributablePower);
		distributeSurplusRemainingPower(powerDistribution, distributionStrategy, remainingDistributablePower);

		captureAutomaticPvLimitAfterDistribution(powerDistribution);
		distributeToApplySetPointStep(powerDistribution);
	}

	/**
	 * For EVs that are Non-Active but not configured as {@link Mode#ZERO}, still
	 * set the minimum Set-Point to allow pre-heating, etc.
	 * 
	 * <p>
	 * This applies to
	 * 
	 * <ul>
	 * <li>not {@link ChargePointAbilities#isReadyForCharging()}
	 * <li>{@link Params#appearsToBeFullyCharged()}
	 * </ul>
	 * 
	 * @param powerDistribution the {@link PowerDistribution}
	 */
	private static void permitNonActives(PowerDistribution powerDistribution) {
		powerDistribution.streamNonActives().forEach(e -> {
			if (e.isAutomaticPhaseSwitching()) {
				return;
			}
			e.setPointInWatt = switch (e.mode) {
			case MINIMUM, FORCE, SURPLUS -> e.params.combinedAbilities().applySetPoint().min();
			case ZERO -> 0;
			};
		});
	}

	/**
	 * Applies a change limit for set-points.
	 *
	 * <ul>
	 * <li>Rising values: limited by {@link #MAX_PERCENTAGE_CHANGE_PER_SECOND} using
	 * {@link #findFirstEntryWithSameSetPoint(Types.History)} to find the reference
	 * point in history, avoiding rounding issues when setpoints remain constant.
	 * <li>Declining values: no limit
	 * </ul>
	 *
	 * @param clock             the {@link Clock}
	 * @param powerDistribution the {@link PowerDistribution}
	 */
	static void applyChangeLimit(Clock clock, PowerDistribution powerDistribution) {
		powerDistribution.streamActives().forEach(e -> {
			final var applySetPointAbility = e.params.combinedAbilities().applySetPoint();
			final var fallbackLimit = applySetPointAbility.toPower(applySetPointAbility.min());

			final int limit;
			final var lastEntry = e.params.history().getLastEntry();
			if (lastEntry == null) {
				// No history -> limit to min
				limit = fallbackLimit;

			} else {
				final var lastSetPoint = lastEntry.getValue().setPoint();
				if (lastSetPoint > e.setPointInWatt) {
					// Reduced set-point -> no limit
					return;
				} else if (lastSetPoint == 0) {
					// last set-point was zero-> limit to min
					limit = fallbackLimit;
				} else {
					var firstEntryWithSameSetPoint = findFirstEntryWithSameSetPoint(e.params.history());
					var duration = Duration.between(firstEntryWithSameSetPoint.getKey(), Instant.now(clock)).toMillis();
					if (duration < 1) {
						// history value is not in the past -> limit to min
						limit = fallbackLimit;
					} else {
						limit = lastSetPoint
								+ (int) Math.ceil(lastSetPoint * MAX_PERCENTAGE_CHANGE_PER_SECOND * (duration / 1000f));
					}
				}
			}
			e.setPointInWatt = Math.min(e.setPointInWatt, limit);
		});
	}

	/**
	 * Finds the first history entry with the same setpoint as the last entry.
	 *
	 * <p>
	 * This method is used in {@link #applyChangeLimit(Clock, PowerDistribution)} to
	 * correctly apply the {@link #MAX_PERCENTAGE_CHANGE_PER_SECOND} ramp
	 * constraint.
	 *
	 * <p>
	 * <b>Problem solved:</b> When a setpoint remained constant over multiple
	 * cycles, using the last entry as reference caused rounding issues that
	 * prevented proper power ramping. For example, starting from 6 A and trying to
	 * ramp to 16 A would fail due to accumulated rounding errors and fall back to
	 * the minimum (6 A).
	 *
	 * <p>
	 * <b>Solution:</b> By finding the first entry with the same setpoint value, the
	 * ramp calculation starts from when the setpoint first changed to that value,
	 * allowing gradual increases like: 6 → 6 → 6 → 7 → 7 → 7 → 8 (in Ampere units).
	 *
	 * <p>
	 * <b>History filtering:</b> Only considers entries where:
	 * <ul>
	 * <li>activePower is not null and not 0</li>
	 * <li>isReadyForCharging is true</li>
	 * </ul>
	 * For example, if the last entry with setpoint 7 A has invalid activePower or
	 * isReadyForCharging false, the method searches backwards to find the first
	 * valid entry with 7 A.
	 *
	 * @param history the {@link Types.History} to search
	 * @return a {@link Map.Entry} containing the timestamp and history entry of the
	 *         first occurrence with the same setpoint as the last entry
	 */

	static Map.Entry<Instant, Types.History.Entry> findFirstEntryWithSameSetPoint(Types.History history) {
		var entries = history.streamAllWithActivePowerAndReadyForCharging().toList();
		var lastEntry = history.getLastEntry();
		var lastSetPoint = lastEntry.getValue().setPoint();
		var firstEntryWithSameSetPoint = lastEntry;
		// go through history backwards until set-point changes
		for (int i = entries.size() - 1; i >= 0; i--) {
			if (entries.get(i).getValue().setPoint() != lastSetPoint) {
				break;
			}
			firstEntryWithSameSetPoint = entries.get(i);
		}
		return firstEntryWithSameSetPoint;
	}

	/**
	 * Calculates the total excess power, depending on the current PV production and
	 * house consumption.
	 * 
	 * @param powerDistribution the {@link PowerDistribution}
	 * @param sum               the {@link Sum} component
	 * @return the available additional excess power for charging
	 */
	protected static int calculateTotalExcessPower(PowerDistribution powerDistribution, Sum sum) {
		var buyFromGrid = sum.getGridActivePower().orElse(0);
		var essDischarge = sum.getEssDischargePower().orElse(0);
		var evseCharge = powerDistribution.totalActivePower;

		return max(0, evseCharge - buyFromGrid - essDischarge);
	}

	/**
	 * Distribute minimum required power to each Controller (e.g. 6 A on
	 * single-/three-phase).
	 * 
	 * @param powerDistribution  the {@link PowerDistribution}
	 * @param distributablePower the total distributable power (i.e. the excess
	 *                           power)
	 * @return the remaining distributable power
	 */
	private static int distributeSurplusMinPower(PowerDistribution powerDistribution, int distributablePower) {
		var remaining = distributablePower;
		for (var e : powerDistribution.streamSurplus().toList()) {
			final var p = e.params;
			var hysteresis = p.hysteresis();
			if (hysteresis == Hysteresis.KEEP_ZERO || e.isAutomaticPhaseSwitching()) {
				continue;
			}
			final var combinedAbilities = p.combinedAbilities();
			final var asp = combinedAbilities.applySetPoint();
			var power = asp.toPower(asp.min());
			if (hysteresis != Hysteresis.KEEP_CHARGING && power > remaining) {
				continue;
			}
			e.setPointInWatt = power;
			remaining -= power;
		}
		return remaining;
	}

	/**
	 * Distribute distributablePower (i.e. remaining excess power) as per given
	 * {@link DistributionStrategy}.
	 * 
	 * @param powerDistribution    the {@link PowerDistribution}
	 * @param distributionStrategy the {@link DistributionStrategy}
	 * @param distributablePower   the total distributable power (i.e. remaining
	 *                             excess power)
	 */
	protected static void distributeSurplusRemainingPower(PowerDistribution powerDistribution,
			DistributionStrategy distributionStrategy, int distributablePower) {
		var distributionCandidates = powerDistribution.streamSurplusOrMinimum() //
				.toList();

		var entries = distributionCandidates.stream().filter(e -> e.setPointInWatt > 0 || e.isAutomaticPhaseSwitching()) //
				.toList();
		if (entries.isEmpty()) {
			return;
		}
		switch (distributionStrategy) {
		case EQUAL_POWER -> distributePowerEqual(entries, distributablePower);
		case BY_PRIORITY -> distributePowerByPriority(entries, distributablePower);
		}
	}

	/**
	 * Distribute power equally among Controllers.
	 * 
	 * @param initialEntries            the PowerDistribution Entries
	 * @param initialDistributablePower the distributable power
	 */
	protected static void distributePowerEqual(final List<PowerDistribution.Entry> initialEntries,
			final int initialDistributablePower) {
		var entries = initialEntries.stream() //
				// Only entries that do not already apply max set-point
				.filter(e -> e.setPointInWatt < e.params.combinedAbilities()
						.getDistributionMaxSetPointInWatt(e.isAutomaticPhaseSwitching())) //
				.toList();
		if (entries.isEmpty()) {
			return; // avoid divide by zero
		}

		final var equalPower = initialDistributablePower / entries.size();
		var remaining = initialDistributablePower;
		for (var e : entries) {
			var before = e.setPointInWatt;
			var after = e.fitSetPointWithinDistributionBounds(before + equalPower);
			remaining -= after - before;
			e.setPointInWatt = after;
		}

		if (initialDistributablePower != remaining) {
			// Recursive call to distribute remaining power
			distributePowerEqual(entries, remaining);
		}
	}

	/**
	 * Distribute power by priority among Controllers.
	 * 
	 * @param entries            the PowerDistribution Entries
	 * @param distributablePower the distributable power
	 */
	protected static void distributePowerByPriority(List<PowerDistribution.Entry> entries, int distributablePower) {
		var remaining = distributablePower;
		for (var e : entries) {
			var before = e.setPointInWatt;
			var after = e.fitSetPointWithinDistributionBounds(before + remaining);

			remaining -= after - before;
			e.setPointInWatt = after;
		}
	}

	private static void captureAutomaticPvLimitAfterDistribution(PowerDistribution powerDistribution) {
		powerDistribution.streamSurplusOrMinimum() //
				.filter(PowerDistribution.Entry::isAutomaticPhaseSwitching)
				.forEach(e -> e.setPointWithoutPhaseLimitation = Math.max(0, e.setPointInWatt));
	}

	/**
	 * This last step distributes the power according to the 'steps' defined in the
	 * {@link ApplySetPoint.Ability}.
	 * 
	 * <p>
	 * Example: if a {@link EvseChargePoint} only supports
	 * {@link ApplySetPoint.Ability.Ampere}, its set-point is adjusted (reduced) to
	 * match the step. The gained power is again distributed among the Controllers.
	 * 
	 * @param powerDistribution the {@link PowerDistribution}
	 */
	private static void distributeToApplySetPointStep(PowerDistribution powerDistribution) {
		var entries = powerDistribution.streamSurplusGreaterZero() //
				.toList();
		var distributablePower = 0;
		for (var e : entries.reversed()) {
			var set = roundDownToPowerStep(e.params.combinedAbilities().applySetPoint(), e.setPointInWatt);
			distributablePower += e.setPointInWatt - set;
			e.setPointInWatt = set;
		}
		for (var e : entries) {
			if (distributablePower < 1) {
				break;
			}
			var set = roundDownToPowerStep(e.params.combinedAbilities().applySetPoint(),
					e.setPointInWatt + distributablePower);
			distributablePower -= set - e.setPointInWatt;
			e.setPointInWatt = set;
		}
	}

	/**
	 * Takes the PowerDistribution Entries of one {@link ControllerEvseSingle} and
	 * sets the {@link ApplySetPoint.Action}.
	 * 
	 * @param e            the PowerDistribution Entry
	 * @param logVerbosity the configured {@link LogVerbosity}
	 * @param logger       a log message consumer
	 */
	private static void handleApplySetPoint(PowerDistribution.Entry e, LogVerbosity logVerbosity,
			Consumer<String> logger) {
		final var ctrl = e.ctrl;
		final var params = e.params;
		final var combinedAbilities = params.combinedAbilities();
		final var chargePointAbilities = combinedAbilities.chargePointAbilities();

		if (chargePointAbilities == null) {
			if (logVerbosity == TRACE) {
				logger.accept(ctrl.id() + ": " //
						+ "Mode [" + e.mode + "] " //
						+ "ChargePointCapability is null " //
						+ params);
			}
			return;
		}

		if (e.isAutomaticPhaseSwitching()) {
			final var applySetPointAbility = params.combinedAbilities().applySetPoint();
			final var minSetPointInWatt = e.singlePhaseMinimum();
			if (e.mode == Mode.ZERO) {
				e.setPointInWatt = 0;
			} else if (e.mode == Mode.SURPLUS && e.setPointWithoutPhaseLimitation < minSetPointInWatt) {
				e.setPointInWatt = params.hysteresis() == Hysteresis.KEEP_CHARGING ? minSetPointInWatt : 0;
			} else if (e.mode == Mode.MINIMUM) {
				e.setPointInWatt = fitAutomaticMinimumSetPointInWatt(applySetPointAbility, e.setPointInWatt,
						e.setPointWithoutPhaseLimitation);
			} else {
				e.setPointInWatt = applySetPointAbility.fitWithin(e.setPointInWatt);
			}

		}
		var value = params.combinedAbilities().chargePointAbilities().applySetPoint().fromPower(e.setPointInWatt);

		if ((e.mode == Mode.SURPLUS || e.mode == Mode.MINIMUM) && e.isAutomaticPhaseSwitching()) {
			e.actions.setSetPointWithoutPhaseLimitation(e.setPointWithoutPhaseLimitation);
		}

		if (logVerbosity == TRACE) {
			logger.accept(ctrl.id() + ": " //
					+ "Mode [" + e.mode + "] " //
					+ "Set [" + e.setPointInWatt + " W -> " + value + "] " //
					+ params);
		}

		switch (combinedAbilities.chargePointAbilities().applySetPoint()) {
		case ApplySetPoint.Ability.MilliAmpere ma -> e.actions.setApplySetPointInMilliAmpere(value);
		case ApplySetPoint.Ability.Ampere a -> e.actions.setApplySetPointInAmpere(value);
		case ApplySetPoint.Ability.Watt w -> e.actions.setApplySetPointInWatt(value);
		}
	}

	/**
	 * Handles a {@link PhaseSwitchDirection} Action for one
	 * {@link ControllerEvseSingle}.
	 * 
	 * @param e            the PowerDistribution Entry
	 * @param clock        the {@link Clock}
	 * @param logVerbosity the configured {@link LogVerbosity}
	 * @param logger       a log message consumer
	 */
	private static void handlePhaseSwitch(PowerDistribution.Entry e, Clock clock, LogVerbosity logVerbosity,
			Consumer<String> logger) {
		final var ctrl = e.ctrl;
		final var params = e.params;
		final var now = Instant.now(clock);
		final var probableSwitchTimestampWasCleared = cleanupOutdatedProbablePhaseSwitchTimestamp(ctrl, now);
		if (e.mode == Mode.ZERO) {
			return;
		}
		final var phaseSwitchAbility = params.combinedAbilities().phaseSwitch();
		if (phaseSwitchAbility == null) {
			// Phase-Switching is not available with ChargePoint and/or ElectricVehicle
			return;
		}

		final var actions = e.actions;
		switch (params.phaseSwitching()) {
		// Evse.Controller.Single wants...
		case DISABLE -> doNothing(); //
		case FORCE_SINGLE_PHASE -> //
			applyPhaseSwitchIfDirectionMatches(ctrl, actions, phaseSwitchAbility, //
					TO_SINGLE_PHASE, "SINGLE", logVerbosity, logger);
		case FORCE_THREE_PHASE -> //
			applyPhaseSwitchIfDirectionMatches(ctrl, actions, phaseSwitchAbility, //
					TO_THREE_PHASE, "THREE", logVerbosity, logger);
		case AUTOMATIC -> handleAutomaticPhaseSwitch(e, clock, phaseSwitchAbility, probableSwitchTimestampWasCleared,
				logVerbosity, logger);
		}
	}

	private static void applyAutomaticPhaseSwitchIfDirectionMatches(Clock clock, ControllerEvseSingle ctrl,
			Params params, ChargePointActions.Builder actions, ApplyPhaseSwitch phaseSwitchAbility,
			PhaseSwitchDirection targetDirection, LogVerbosity logVerbosity, Consumer<String> logger) {
		final var now = Instant.now(clock);
		final var history = params.history();
		if (history.isAutomaticPhaseSwitchInCooldown(now)) {
			return;
		}
		if (applyPhaseSwitchIfDirectionMatches(ctrl, actions, phaseSwitchAbility, targetDirection,
				targetDirection.name(), logVerbosity, logger)) {
			history.setAutomaticPhaseSwitchCooldown(now);
		}
	}

	/**
	 * Handles automatic phase switching based on mode and power requirements.
	 * 
	 * @param e                                 the PowerDistribution Entry
	 * @param clock                             the {@link Clock}
	 * @param phaseSwitchAbility                the available phase switch ability
	 * @param probableSwitchTimestampWasCleared true if an outdated probable switch
	 *                                          timestamp was cleared in this cycle
	 * @param logVerbosity                      the configured {@link LogVerbosity}
	 * @param logger                            a log message consumer
	 */
	private static void handleAutomaticPhaseSwitch(PowerDistribution.Entry e, Clock clock,
			ApplyPhaseSwitch phaseSwitchAbility, boolean probableSwitchTimestampWasCleared, LogVerbosity logVerbosity,
			Consumer<String> logger) {
		final var ctrl = e.ctrl;
		final var actions = e.actions;
		final var mode = e.mode;

		switch (mode) {
		case FORCE -> applyAutomaticPhaseSwitchIfDirectionMatches(clock, ctrl, e.params, actions, phaseSwitchAbility, //
				TO_THREE_PHASE, logVerbosity, logger);

		case MINIMUM -> {
			final var chargePointAbilities = e.params.combinedAbilities().chargePointAbilities();
			final var electricVehicleAbilities = e.params.combinedAbilities().electricVehicleAbilities();

			if (chargePointAbilities != null && electricVehicleAbilities != null) {
				final var asp = chargePointAbilities.applySetPoint();
				final var singlePhaseMinInWatt = resolveAutomaticPhaseSwitchTargetPhaseMinPowerInWatt(
						phaseSwitchAbility, TO_SINGLE_PHASE);
				if (asp.phase() == THREE_PHASE && phaseSwitchAbility.direction() == TO_SINGLE_PHASE
						&& e.setPointWithoutPhaseLimitation < singlePhaseMinInWatt) {
					applyAutomaticPhaseSwitchIfDirectionMatches(clock, ctrl, e.params, actions, phaseSwitchAbility,
							TO_SINGLE_PHASE, logVerbosity, logger);
				} else {
					optimizePhaseForSurplus(e, clock, phaseSwitchAbility, probableSwitchTimestampWasCleared,
							logVerbosity, logger);
				}
			} else {
				applyAutomaticPhaseSwitchIfDirectionMatches(clock, ctrl, e.params, actions, phaseSwitchAbility, //
						TO_SINGLE_PHASE, logVerbosity, logger);
			}
		}
		case SURPLUS -> optimizePhaseForSurplus(e, clock, phaseSwitchAbility, probableSwitchTimestampWasCleared,
				logVerbosity, logger);

		case ZERO -> doNothing();
		}
	}

	/**
	 * Optimizes phase switching for SURPLUS mode based on available power. Switches
	 * to three-phase if sufficient power is available, otherwise uses single-phase.
	 * Considers the maximum power available in both phase modes.
	 *
	 * @param e                                 the PowerDistribution Entry
	 * @param clock                             the {@link Clock}
	 * @param phaseSwitchAbility                the available phase switch ability
	 * @param probableSwitchTimestampWasCleared true if an outdated probable switch
	 *                                          timestamp was cleared in this cycle
	 * @param logVerbosity                      the configured {@link LogVerbosity}
	 * @param logger                            a log message consumer
	 */
	private static void optimizePhaseForSurplus(PowerDistribution.Entry e, Clock clock,
			ApplyPhaseSwitch phaseSwitchAbility, boolean probableSwitchTimestampWasCleared, LogVerbosity logVerbosity,
			Consumer<String> logger) {
		final var ctrl = e.ctrl;
		final var actions = e.actions;
		final var params = e.params;
		final var setPointInWatt = e.setPointInWatt;
		final var setPointWithoutPhaseLimitation = e.setPointWithoutPhaseLimitation;
		final var now = Instant.now(clock);

		final var chargePointAbilities = params.combinedAbilities().chargePointAbilities();
		final var electricVehicleAbilities = params.combinedAbilities().electricVehicleAbilities();

		if (chargePointAbilities == null || electricVehicleAbilities == null) {
			return;
		}

		final var activeAbility = chargePointAbilities.applySetPoint();
		final int maxSinglePhase = calculateMaxPowerForPhase(chargePointAbilities, electricVehicleAbilities,
				SINGLE_PHASE);
		final int maxThreePhase = calculateMaxPowerForPhase(chargePointAbilities, electricVehicleAbilities,
				THREE_PHASE);

		AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation singleToThreeEvaluation = null;

		final var automaticPhaseSwitchContext = new AutomaticPhaseSwitchContext(clock, ctrl, params, actions,
				phaseSwitchAbility, probableSwitchTimestampWasCleared, setPointWithoutPhaseLimitation);

		if (activeAbility.phase() == SINGLE_PHASE && phaseSwitchAbility.direction() == TO_THREE_PHASE) {
			singleToThreeEvaluation = handleAutomaticSingleToThreePhaseSwitch(automaticPhaseSwitchContext, now,
					logVerbosity, logger);
			logAutomaticPhaseSwitchSummary(ctrl, params, now, activeAbility.phase(), phaseSwitchAbility, setPointInWatt,
					setPointWithoutPhaseLimitation, singleToThreeEvaluation, null, null, false, logVerbosity, logger);
			return;
		}

		if (activeAbility.phase() == THREE_PHASE && phaseSwitchAbility.direction() == TO_SINGLE_PHASE) {
			final var threeToSingleResult = handleAutomaticThreeToSinglePhaseSwitch(automaticPhaseSwitchContext, now,
					logVerbosity, logger);
			logAutomaticPhaseSwitchSummary(ctrl, params, now, activeAbility.phase(), phaseSwitchAbility, setPointInWatt,
					setPointWithoutPhaseLimitation, null, threeToSingleResult.evaluation,
					threeToSingleResult.singlePhaseFeasibilityEvaluation,
					threeToSingleResult.singlePhaseFeasibleInRecentWindow, logVerbosity, logger);
			return;
		}

		// Fallback for unexpected combinations.
		if (maxThreePhase > maxSinglePhase && setPointInWatt >= maxSinglePhase) {
			applyAutomaticPhaseSwitchIfDirectionMatches(clock, ctrl, params, actions, phaseSwitchAbility, //
					TO_THREE_PHASE, logVerbosity, logger);
		} else {
			applyAutomaticPhaseSwitchIfDirectionMatches(clock, ctrl, params, actions, phaseSwitchAbility, //
					TO_SINGLE_PHASE, logVerbosity, logger);
		}
		logAutomaticPhaseSwitchSummary(ctrl, params, now, activeAbility.phase(), phaseSwitchAbility, setPointInWatt,
				setPointWithoutPhaseLimitation, null, null, null, false, logVerbosity, logger);
	}

	private static AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation handleAutomaticSingleToThreePhaseSwitch(
			AutomaticPhaseSwitchContext context, Instant now, LogVerbosity logVerbosity, Consumer<String> logger) {
		final var singleToThreeThreshold = AUTOMATIC_SINGLE_TO_THREE_PHASE_SWITCH_POWER;
		final var singleToThreeEvaluation = context.params().history()
				.evaluateAutomaticPhaseSwitchSetPointWithoutPhaseLimitation(now,
						context.setPointWithoutPhaseLimitation(), singleToThreeThreshold,
						AutomaticPhaseSwitchThresholdDirection.ABOVE);
		final var shortWindowEvaluation = context.params().history()
				.evaluateAutomaticPhaseSwitchSetPointWithoutPhaseLimitationForWindow(now, singleToThreeThreshold,
						AutomaticPhaseSwitchThresholdDirection.ABOVE, AUTOMATIC_PROBABLE_PHASE_SWITCH_WINDOW,
						context.setPointWithoutPhaseLimitation());
		if (singleToThreeEvaluation.shouldSwitch()) {
			setProbableNextPhaseSwitchEpochSecondsIfUnset(context.ctrl(), now, shortWindowEvaluation,
					context.probableSwitchTimestampWasCleared());
			applyAutomaticPhaseSwitchIfDirectionMatches(context.clock(), context.ctrl(), context.params(),
					context.actions(), context.phaseSwitchAbility(), TO_THREE_PHASE, logVerbosity, logger);
		}
		return singleToThreeEvaluation;
	}

	private static ThreeToSinglePhaseSwitchResult handleAutomaticThreeToSinglePhaseSwitch(
			AutomaticPhaseSwitchContext context, Instant now, LogVerbosity logVerbosity, Consumer<String> logger) {
		final var threeToSingleThreshold = AUTOMATIC_THREE_TO_SNGLE_PHASE_SWITCH_POWER;
		final var threeToSingleEvaluation = context.params().history()
				.evaluateAutomaticPhaseSwitchSetPointWithoutPhaseLimitation(now,
						context.setPointWithoutPhaseLimitation(), threeToSingleThreshold,
						AutomaticPhaseSwitchThresholdDirection.BELOW);
		final var shortWindowEvaluation = context.params().history()
				.evaluateAutomaticPhaseSwitchSetPointWithoutPhaseLimitationForWindow(now, threeToSingleThreshold,
						AutomaticPhaseSwitchThresholdDirection.BELOW, AUTOMATIC_PROBABLE_PHASE_SWITCH_WINDOW,
						context.setPointWithoutPhaseLimitation());
		final var singlePhaseMinInWatt = resolveAutomaticPhaseSwitchTargetPhaseMinPowerInWatt(
				context.phaseSwitchAbility(), TO_SINGLE_PHASE);
		final var singlePhaseFeasibilityEvaluation = context.params().history()
				.evaluateAutomaticPhaseSwitchSetPointWithoutPhaseLimitationForWindow(now, singlePhaseMinInWatt,
						AutomaticPhaseSwitchThresholdDirection.ABOVE, AUTOMATIC_PROBABLE_PHASE_SWITCH_WINDOW,
						context.setPointWithoutPhaseLimitation());
		final var singlePhaseFeasibleInRecentWindow = isAutomaticPhaseSwitchWindowThresholdReached(
				singlePhaseFeasibilityEvaluation, AUTOMATIC_THREE_TO_SINGLE_PHASE_SWITCH_WINDOW_MIN_SAMPLE_COUNT);
		if (threeToSingleEvaluation.shouldSwitch() && singlePhaseFeasibleInRecentWindow) {
			setProbableNextPhaseSwitchEpochSecondsIfUnset(context.ctrl(), now, shortWindowEvaluation,
					context.probableSwitchTimestampWasCleared());
			applyAutomaticPhaseSwitchIfDirectionMatches(context.clock(), context.ctrl(), context.params(),
					context.actions(), context.phaseSwitchAbility(), TO_SINGLE_PHASE, logVerbosity, logger);
		}
		return new ThreeToSinglePhaseSwitchResult(threeToSingleEvaluation, singlePhaseFeasibilityEvaluation,
				singlePhaseFeasibleInRecentWindow);
	}

	private record AutomaticPhaseSwitchContext(Clock clock, ControllerEvseSingle ctrl, Params params,
			ChargePointActions.Builder actions, ApplyPhaseSwitch phaseSwitchAbility,
			boolean probableSwitchTimestampWasCleared, int setPointWithoutPhaseLimitation) {
	}

	private record ThreeToSinglePhaseSwitchResult(
			AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation evaluation,
			AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation singlePhaseFeasibilityEvaluation,
			boolean singlePhaseFeasibleInRecentWindow) {
	}

	private static void logAutomaticPhaseSwitchSummary(ControllerEvseSingle ctrl, Params params, Instant now,
			io.openems.edge.common.type.Phase.SingleOrThreePhase activePhase, ApplyPhaseSwitch phaseSwitchAbility,
			int setPointInWatt, int setPointWithoutPhaseLimitation,
			AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation singleToThreeEvaluation,
			AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation threeToSingleEvaluation,
			AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation singlePhaseFeasibilityEvaluation,
			boolean singlePhaseFeasibleInRecentWindow, LogVerbosity logVerbosity, Consumer<String> logger) {
		if (logVerbosity != TRACE) {
			return;
		}

		final var cooldownUntil = params.history().getAutomaticPhaseSwitchCooldownUntil();
		final var cooldownActive = cooldownUntil != null && now.isBefore(cooldownUntil);
		final var probableNextSwitchEpochSeconds = getProbableNextPhaseSwitchEpochSeconds(ctrl);

		logger.accept(ctrl.id() + ": " //
				+ "AutoPhaseSwitch " //
				+ "phase[" + activePhase + "] " //
				+ "dir[" + phaseSwitchAbility.direction() + "] " //
				+ "set[" + setPointInWatt + "W] " //
				+ "raw[" + setPointWithoutPhaseLimitation + "W] " //
				+ "cooldown[" + cooldownActive + "] " //
				+ "probableNext[" + probableNextSwitchEpochSeconds + "] " //
				+ "1to3{" + formatAutomaticPhaseSwitchEvaluation(singleToThreeEvaluation) + "} " //
				+ "3to1{" + formatAutomaticPhaseSwitchEvaluation(threeToSingleEvaluation) + "} " //
				+ "1pFeasible{" + formatAutomaticPhaseSwitchEvaluation(singlePhaseFeasibilityEvaluation) + "} " //
				+ "1pFeasibleWindowReached[" + singlePhaseFeasibleInRecentWindow + "]");
	}

	private static String formatAutomaticPhaseSwitchEvaluation(
			AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation evaluation) {
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

	private static boolean applyPhaseSwitchIfDirectionMatches(//
			ControllerEvseSingle ctrl, //
			ChargePointActions.Builder actions, //
			ApplyPhaseSwitch phaseSwitchAbility, //
			PhaseSwitchDirection targetDirection, //
			String targetPhaseName, //
			LogVerbosity logVerbosity, //
			Consumer<String> logger) {
		if (phaseSwitchAbility.direction() != targetDirection) {
			return false;
		}
		if (logVerbosity == TRACE) {
			logger.accept(ctrl.id() + ": Force switch to " + targetPhaseName + " phase");
		}
		actions.setPhaseSwitch(phaseSwitchAbility);
		setValue(ctrl, ControllerEvseSingle.ChannelId.PROBABLE_NEXT_PHASE_SWITCH_EPOCH_SECONDS, null);
		return true;
	}

	private static boolean cleanupOutdatedProbablePhaseSwitchTimestamp(ControllerEvseSingle ctrl, Instant now) {
		final var probableNextSwitchEpochSeconds = getProbableNextPhaseSwitchEpochSeconds(ctrl);
		if (probableNextSwitchEpochSeconds == null) {
			return false;
		}
		if (probableNextSwitchEpochSeconds > now.getEpochSecond()) {
			return false;
		}
		setValue(ctrl, ControllerEvseSingle.ChannelId.PROBABLE_NEXT_PHASE_SWITCH_EPOCH_SECONDS, null);
		return true;
	}

	private static void setProbableNextPhaseSwitchEpochSecondsIfUnset(ControllerEvseSingle ctrl, Instant now,
			AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation evaluation,
			boolean probableSwitchTimestampWasCleared) {
		if (probableSwitchTimestampWasCleared || !evaluation.shouldSwitch()) {
			return;
		}
		if (getProbableNextPhaseSwitchEpochSeconds(ctrl) != null) {
			return;
		}
		final var nextProbableSwitchEpochSeconds = now.plus(AUTOMATIC_PROBABLE_PHASE_SWITCH_DELAY).getEpochSecond();
		setValue(ctrl, ControllerEvseSingle.ChannelId.PROBABLE_NEXT_PHASE_SWITCH_EPOCH_SECONDS,
				nextProbableSwitchEpochSeconds);
	}

	private static int resolveAutomaticPhaseSwitchTargetPhaseMinPowerInWatt(ApplyPhaseSwitch phaseSwitchAbility,
			PhaseSwitchDirection targetDirection) {
		if (phaseSwitchAbility == null) {
			return switch (targetDirection) {
			case TO_SINGLE_PHASE -> ApplySetPoint.MIN_POWER_SINGLE_PHASE;
			case TO_THREE_PHASE -> ApplySetPoint.MIN_POWER_THREE_PHASE;
			};
		}

		final var oppositePhaseApplySetPoint = phaseSwitchAbility.oppositePhaseApplySetPoint();
		if (oppositePhaseApplySetPoint != null
				&& !oppositePhaseApplySetPoint.equals(ApplySetPoint.Ability.EMPTY_APPLY_SET_POINT_ABILITY)) {
			return oppositePhaseApplySetPoint.min();
		}
		return switch (targetDirection) {
		case TO_SINGLE_PHASE -> ApplySetPoint.MIN_POWER_SINGLE_PHASE;
		case TO_THREE_PHASE -> ApplySetPoint.MIN_POWER_THREE_PHASE;
		};
	}

	private static int fitAutomaticMinimumSetPointInWatt(ApplySetPoint.Ability applySetPointAbility, int setPointInWatt,
			int setPointWithoutPhaseLimitation) {
		final var minSetPointInWatt = applySetPointAbility.toPower(applySetPointAbility.min());
		if (applySetPointAbility.phase() == THREE_PHASE && setPointWithoutPhaseLimitation < minSetPointInWatt) {
			return minSetPointInWatt;
		}
		return Math.max(minSetPointInWatt, applySetPointAbility.fitWithin(setPointInWatt));
	}

	private static boolean isAutomaticPhaseSwitchWindowThresholdReached(
			AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation evaluation, int minSampleCount) {
		return evaluation.windowActive() && evaluation.sampleCount() >= minSampleCount
				&& isAutomaticPhaseSwitchThresholdReached(evaluation);
	}

	private static boolean isAutomaticPhaseSwitchThresholdReached(
			AutomaticPhaseSwitchSetPointWithoutPhaseLimitationEvaluation evaluation) {
		return switch (evaluation.direction()) {
		case ABOVE -> evaluation.directionalNinetyPercentAverage() >= evaluation.thresholdInWatt();
		case BELOW -> evaluation.directionalNinetyPercentAverage() <= evaluation.thresholdInWatt();
		};
	}

	private static Long getProbableNextPhaseSwitchEpochSeconds(ControllerEvseSingle ctrl) {
		final var channel = ctrl.channel(ControllerEvseSingle.ChannelId.PROBABLE_NEXT_PHASE_SWITCH_EPOCH_SECONDS);
		final Long nextValue = (Long) channel.getNextValue().get();
		if (nextValue != null) {
			return nextValue;
		}
		return (Long) channel.value().get();
	}

	/**
	 * Calculates the maximum power available for a specific phase configuration.
	 *
	 * @param chargePointAbilities     the ChargePointAbilities
	 * @param electricVehicleAbilities the ElectricVehicleAbilities
	 * @param targetPhase              the target phase (SINGLE_PHASE or
	 *                                 THREE_PHASE)
	 * @return the maximum power in watts for the given phase configuration
	 */
	private static int calculateMaxPowerForPhase(ChargePointAbilities chargePointAbilities,
			io.openems.edge.evse.api.electricvehicle.Profile.ElectricVehicleAbilities electricVehicleAbilities,
			io.openems.edge.common.type.Phase.SingleOrThreePhase targetPhase) {
		if (chargePointAbilities == null || electricVehicleAbilities == null) {
			return 0;
		}

		final var cpAbility = chargePointAbilities.applySetPoint();
		final var cpMax = cpAbility.toPower(cpAbility.max());
		final int cpPhaseMax = switch (targetPhase) {
		case SINGLE_PHASE -> cpAbility.phase() == SINGLE_PHASE ? cpMax : cpMax / 3;
		case THREE_PHASE -> cpAbility.phase() == THREE_PHASE ? cpMax : cpMax * 3;
		};

		final int evPhaseMax = switch (targetPhase) {
		case SINGLE_PHASE -> {
			if (!electricVehicleAbilities.singlePhaseLimit()
					.equals(ApplySetPoint.Ability.EMPTY_APPLY_SET_POINT_ABILITY)) {
				yield electricVehicleAbilities.singlePhaseLimit().max();
			}
			if (!electricVehicleAbilities.threePhaseLimit()
					.equals(ApplySetPoint.Ability.EMPTY_APPLY_SET_POINT_ABILITY)) {
				yield electricVehicleAbilities.threePhaseLimit().max() / 3;
			}
			yield 0;
		}
		case THREE_PHASE -> {
			if (!electricVehicleAbilities.threePhaseLimit()
					.equals(ApplySetPoint.Ability.EMPTY_APPLY_SET_POINT_ABILITY)) {
				yield electricVehicleAbilities.threePhaseLimit().max();
			}
			if (!electricVehicleAbilities.singlePhaseLimit()
					.equals(ApplySetPoint.Ability.EMPTY_APPLY_SET_POINT_ABILITY)) {
				yield electricVehicleAbilities.singlePhaseLimit().max() * 3;
			}
			yield 0;
		}
		};

		return evPhaseMax < 1 ? cpPhaseMax : Math.min(cpPhaseMax, evPhaseMax);
	}
}
