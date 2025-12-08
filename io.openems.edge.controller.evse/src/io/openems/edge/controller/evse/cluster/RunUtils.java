package io.openems.edge.controller.evse.cluster;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.controller.evse.cluster.LogVerbosity.TRACE;
import static io.openems.edge.evse.api.chargepoint.Profile.PhaseSwitch.TO_SINGLE_PHASE;
import static io.openems.edge.evse.api.chargepoint.Profile.PhaseSwitch.TO_THREE_PHASE;
import static io.openems.edge.evse.api.common.ApplySetPoint.roundDownToPowerStep;
import static java.lang.Math.max;
import static java.util.stream.Collectors.joining;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.evse.cluster.EnergyScheduler.SingleModes;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.controller.evse.single.Params;
import io.openems.edge.controller.evse.single.Types.Hysteresis;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.chargepoint.Profile.PhaseSwitch;
import io.openems.edge.evse.api.common.ApplySetPoint;

public class RunUtils {

	/**
	 * Max allowed change for increasing power/current. Applied in
	 * {@link #applyChangeLimit(PowerDistribution)}. A value of 0.03 requires about
	 * 1 minute from 6 A to 32 A.
	 */
	private static final float MAX_PERCENTAGE_CHANGE_PER_SECOND = 0.03F;

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
			public final Mode.Actual actualMode;
			public final Params params;
			public final Integer activePower;
			public final ChargePointActions.Builder actions;

			protected int setPointInWatt;

			public Entry(ControllerEvseSingle ctrl, Params params) {
				this.ctrl = ctrl;
				this.params = params;

				this.activePower = params.activePower();
				this.actions = ChargePointActions.from(params.combinedAbilities().chargePointAbilities());

				this.actualMode = switch (params.mode()) {
				case FORCE, MINIMUM, SURPLUS, ZERO -> params.mode().actual;
				// TODO evaluate params smartConfig
				case SMART -> Mode.Actual.SURPLUS;
				};
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
		}

		/**
		 * Creates {@link PowerDistribution} from a list of
		 * {@link ControllerEvseSingle}.
		 * 
		 * @param ctrls the list of {@link ControllerEvseSingle}
		 * @return a {@link PowerDistribution}
		 */
		protected static PowerDistribution of(List<ControllerEvseSingle> ctrls) {
			return new PowerDistribution(ctrls.stream() //
					.map(ctrl -> new PowerDistribution.Entry(ctrl, ctrl.getParams())) //
					.collect(toImmutableList()));
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
		 * charging and in {@link Mode.Actual#SURPLUS} mode.
		 * 
		 * @return {@link Stream}
		 */
		public final Stream<Entry> streamSurplus() {
			return this.streamActives() //
					.filter(e -> switch (e.actualMode) {
					case FORCE, MINIMUM, ZERO -> false;
					case SURPLUS -> true;
					});
		}

		/**
		 * Stream all {@link Entry}s with non-null {@link Params} which are ready for
		 * charging and in {@link Mode.Actual#SURPLUS} mode and have a temporary
		 * Set-Point > 0.
		 * 
		 * @return {@link Stream}
		 */
		public final Stream<Entry> streamSurplusGreaterZero() {
			return this.streamSurplus()
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
	 * @param singleModes          the {@link SingleModes}
	 * @param logVerbosity         the configured {@link LogVerbosity}
	 * @param logger               a log message consumer
	 * @return the {@link PowerDistribution}
	 */
	protected static PowerDistribution calculate(Clock clock, DistributionStrategy distributionStrategy, Sum sum,
			List<ControllerEvseSingle> ctrls, SingleModes singleModes, LogVerbosity logVerbosity,
			Consumer<String> logger) {
		// Build PowerDistribution
		var powerDistribution = new PowerDistribution(ctrls.stream() //
				.map(ctrl -> {
					var params = ctrl.getParams();
					if (params == null) {
						return null;
					}
					return new PowerDistribution.Entry(ctrl, params);
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
			handlePhaseSwitch(e, logVerbosity, logger);
		});

		return powerDistribution;
	}

	/**
	 * Initialize the Set-Points for {@link Mode.Actual#FORCE},
	 * {@link Mode.Actual#MINIMUM} and {@link Mode.Actual#ZERO}.
	 * 
	 * @param powerDistribution the {@link PowerDistribution}
	 */
	private static void initializeSetPoints(PowerDistribution powerDistribution) {
		powerDistribution.streamActives().forEach(e -> {
			var asp = e.params.combinedAbilities().applySetPoint();
			e.setPointInWatt = switch (e.actualMode) {
			case MINIMUM -> asp.min();
			case FORCE -> asp.max();
			case SURPLUS, ZERO -> 0;
			};
		});
	}

	/**
	 * Distribute excess power to Controllers in {@link Mode.Actual#SURPLUS} mode.
	 * 
	 * <p>
	 * First distributes minimum required power to each Controller (e.g. 6 A on
	 * single-/three-phase); then distributes remaining excess power as per given
	 * {@link DistributionStrategy}.
	 * 
	 * @param powerDistribution    the {@link PowerDistribution}
	 * @param distributionStrategy the {@link DistributionStrategy}
	 * @param sum                  the {@link Sum} component
	 */
	private static void distributeSurplusPower(PowerDistribution powerDistribution,
			DistributionStrategy distributionStrategy, Sum sum) {
		var totalExcessPower = calculateTotalExcessPower(powerDistribution, sum);
		var totalFixedPower = powerDistribution.streamActives() //
				.mapToInt(e -> e.setPointInWatt) // initialized before via initializeSetPoints()
				.sum();
		var totalDistributablePower = Math.max(0, totalExcessPower - totalFixedPower);
		var remainingDistributablePower = distributeSurplusMinPower(powerDistribution, totalDistributablePower);
		distributeSurplusRemainingPower(powerDistribution, distributionStrategy, remainingDistributablePower);

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
			e.setPointInWatt = switch (e.actualMode) {
			case MINIMUM, FORCE, SURPLUS -> e.params.combinedAbilities().applySetPoint().min();
			case ZERO -> 0;
			};
		});
	}

	/**
	 * Applies a change limit for set-points.
	 * 
	 * <ul>
	 * <li>Rising values: see See {@link #MAX_PERCENTAGE_CHANGE_PER_SECOND}.
	 * <li>Declining values: no limit
	 * </ul>
	 * 
	 * @param clock             the {@link Clock}
	 * @param powerDistribution the {@link PowerDistribution}
	 */
	private static void applyChangeLimit(Clock clock, PowerDistribution powerDistribution) {
		powerDistribution.streamActives().forEach(e -> {
			final var fallbackLimit = e.params.combinedAbilities().applySetPoint().min();

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
					var duration = Duration.between(lastEntry.getKey(), Instant.now(clock)).toMillis();
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
			if (hysteresis == Hysteresis.KEEP_ZERO) {
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
		var entries = powerDistribution.streamSurplusGreaterZero() //
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
				.filter(e -> e.setPointInWatt < e.params.combinedAbilities().applySetPoint().max()) //
				.toList();
		if (entries.size() == 0) {
			return; // avoid divide by zero
		}

		final var equalPower = initialDistributablePower / entries.size();
		var remaining = initialDistributablePower;
		for (var e : entries) {
			var before = e.setPointInWatt;
			var after = e.params.combinedAbilities().applySetPoint().fitWithin(before + equalPower);
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
			var after = e.params.combinedAbilities().applySetPoint().fitWithin(before + remaining);

			remaining -= after - before;
			e.setPointInWatt = after;
		}
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
						+ "Mode [" + e.actualMode + "] " //
						+ "ChargePointCapability is null " //
						+ params);
			}
			return;
		}

		var value = params.combinedAbilities().chargePointAbilities().applySetPoint().fromPower(e.setPointInWatt);

		if (logVerbosity == TRACE) {
			logger.accept(ctrl.id() + ": " //
					+ "Mode [" + e.actualMode + "] " //
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
	 * Handles a {@link PhaseSwitch} Action for one {@link ControllerEvseSingle}.
	 * 
	 * @param e            the PowerDistribution Entry
	 * @param logVerbosity the configured {@link LogVerbosity}
	 * @param logger       a log message consumer
	 */
	private static void handlePhaseSwitch(PowerDistribution.Entry e, LogVerbosity logVerbosity,
			Consumer<String> logger) {
		final var ctrl = e.ctrl;
		final var params = e.params;
		final var phaseSwitchAbility = params.combinedAbilities().phaseSwitch();
		if (phaseSwitchAbility == null) {
			// Phase-Switching is not available with ChargePoint and/or ElectricVehicle
			return;
		}

		final var actions = e.actions;
		switch (params.phaseSwitching()) { // Evse.Controller.Single wants...
		case DISABLE -> {
			// ...no phase switching -> do not set any PhaseSwitch action
			doNothing();
		}
		case FORCE_SINGLE_PHASE -> {
			// ...force switch to Single-Phase...
			if (phaseSwitchAbility == TO_SINGLE_PHASE) {
				// ...and ChargePoint and ElectricVehicle support switch to Single-Phase
				if (logVerbosity == TRACE) {
					logger.accept(ctrl.id() + ": Force switch to SINGLE phase");
				}
				actions.setPhaseSwitch(TO_SINGLE_PHASE);
			}
		}
		case FORCE_THREE_PHASE -> {
			// ... force switch to Three-Phase
			if (phaseSwitchAbility == TO_THREE_PHASE) {
				// ...and ChargePoint and ElectricVehicle support switch to Three-Phase
				if (logVerbosity == TRACE) {
					logger.accept(ctrl.id() + ": Force switch to THREE phase");
				}
				actions.setPhaseSwitch(TO_THREE_PHASE);
			}
		}
		}
	}
}
