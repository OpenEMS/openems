package io.openems.edge.controller.evse.cluster;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.edge.common.type.TypeUtils.fitWithin;
import static io.openems.edge.evse.api.EvseConstants.MIN_CURRENT;
import static java.lang.Math.max;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toCollection;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.TreeMap;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableList;

import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.evse.single.ControllerEvseSingle;
import io.openems.edge.controller.evse.single.Params;
import io.openems.edge.evse.api.chargepoint.Mode;
import io.openems.edge.evse.api.chargepoint.Profile;
import io.openems.edge.evse.api.chargepoint.Profile.Command;

public class Utils {

	protected static final int HYSTERESIS = 300; // [s]

	private Utils() {
	}

	protected static record Input(ControllerEvseSingle ctrl, Params params, TreeMap<Instant, Integer> applyHistory,
			Hysteresis hysteresis) {
		public Input(ControllerEvseSingle ctrl, Params params, TreeMap<Instant, Integer> applyHistory) {
			this(ctrl, params, applyHistory, Hysteresis.from(applyHistory));
		}

		@Override
		public final String toString() {
			return toStringHelper(Input.class) //
					.add("ctrl", this.ctrl.id()) //
					.add("params", this.params) //
					.add("applyHistory", this.applyHistory) //
					.toString();
		}

		protected static enum Hysteresis {
			INACTIVE, KEEP_CHARGING, KEEP_ZERO;

			public static Hysteresis from(TreeMap<Instant, Integer> applyHistory) {
				return from(Instant.now(), applyHistory);
			}

			public static Hysteresis from(Instant now, TreeMap<Instant, Integer> applyHistory) {
				if (applyHistory == null || applyHistory.isEmpty()) {
					return Hysteresis.INACTIVE;
				}
				var lastValue = applyHistory.lastEntry().getValue();
				if (lastValue == 0) {
					if (applyHistory.values().stream() //
							.allMatch(v -> v == 0)) {
						return Hysteresis.INACTIVE; // Hysteresis finished
					} else {
						return Hysteresis.KEEP_ZERO;
					}

				} else {
					if (applyHistory.values().stream() //
							.allMatch(v -> v != 0)) {
						return Hysteresis.INACTIVE; // Hysteresis finished
					} else {
						return Hysteresis.KEEP_CHARGING;
					}
				}
			}
		}
	}

	protected static record Output(ControllerEvseSingle ctrl, int current, ImmutableList<Command> commands) {
	}

	protected static ImmutableList<Output> calculate(DistributionStrategy distributionStrategy, Sum sum,
			List<ControllerEvseSingle> ctrls, Map<String, TreeMap<Instant, Integer>> applyHistories,
			Consumer<String> logDebug) {
		var inputs = ctrls.stream() //
				.map(ctrl -> {
					var params = ctrl.getParams();
					if (params == null) {
						return null;
					}
					return new Input(ctrl, params, applyHistories.get(ctrl.id()));
				}) //
				.filter(Objects::nonNull) //
				.collect(toImmutableList());

		final var outputs = ImmutableList.<Output>builder();

		var surplusDistribution = distributeSurplusPower(distributionStrategy, inputs, sum);

		for (var input : inputs) {
			final var ctrl = input.ctrl;
			final var params = input.params;

			// Handle Profile Commands
			final var commands = ImmutableList.<Profile.Command>builder();
			if (params.actualMode() == Mode.Actual.MINIMUM) {
				params.profiles().stream() //
						.filter(Profile.PhaseSwitchToSinglePhase.class::isInstance) //
						.map(Profile.PhaseSwitchToSinglePhase.class::cast) //
						.findFirst().ifPresent(phaseSwitch -> {
							// Switch from THREE to SINGLE phase in MINIMUM mode
							logDebug.accept(ctrl.id() + ": Switch from THREE to SINGLE phase in MINIMUM mode");
							commands.add(phaseSwitch.command());
						});

			} else if (params.actualMode() == Mode.Actual.FORCE) {
				params.profiles().stream() //
						.filter(Profile.PhaseSwitchToThreePhase.class::isInstance) //
						.map(Profile.PhaseSwitchToThreePhase.class::cast) //
						.findFirst().ifPresent(phaseSwitch -> {
							// Switch from SINGLE to THREE phase in FORCE mode
							logDebug.accept(ctrl.id() + ": Switch from SINGLE to THREE phase in FORCE mode");
							commands.add(phaseSwitch.command());
						});
			}

			// Evaluate Charge Current
			var current = switch (params.actualMode()) {
			case ZERO -> 0;
			case MINIMUM -> MIN_CURRENT;
			case SURPLUS -> params.limit().calculateCurrent(surplusDistribution.poll());
			case FORCE -> params.limit().maxCurrent();
			};

			logDebug.accept(ctrl.id() + ": Mode [" + params.actualMode() + "] set current [" + current + "]");
			outputs.add(new Output(ctrl, current, commands.build()));
		}

		return outputs.build();
	}

	protected static Queue<Integer> distributeSurplusPower(DistributionStrategy distributionStrategy,
			ImmutableList<Input> inputs, Sum sum) {
		var totalExcessPower = calculateTotalExcessPower(inputs, sum);
		var totalFixedPower = calculateTotalFixedPower(inputs);
		var surplusInputs = inputs.stream() //
				.filter(i -> switch (i.params.actualMode()) {
				case FORCE, MINIMUM, ZERO -> false;
				case SURPLUS -> true;
				}) //
				.toArray(Input[]::new);
		return stream(
				distributePower(distributionStrategy, surplusInputs, Math.max(0, totalExcessPower - totalFixedPower))) //
				.mapToObj(Integer::valueOf) //
				.collect(toCollection(LinkedList::new));
	}

	/**
	 * Distributes power among surplus EVs by strategy.
	 * 
	 * @param strategy                the {@link DistributionStrategy}
	 * @param surplusInputs           the {@link Input}s; only SURPLUS allowed!
	 * @param totalDistributablePower the power to be distributed
	 * @return array of distributed powers
	 */
	protected static int[] distributePower(DistributionStrategy strategy, Input[] surplusInputs,
			int totalDistributablePower) {
		var d = distributeMinPower(surplusInputs, totalDistributablePower);
		if (d.noOfNonZeroPowers > 0) {
			switch (strategy) {
			case EQUAL_POWER -> distributePowerEqual(surplusInputs, d);
			case BY_PRIORITY -> distributePowerByPriority(surplusInputs, d);
			}
		}
		return d.powers;
	}

	protected static void distributePowerEqual(Input[] surplusInputs, DistributedMinPower d) {
		var p = d.remainingPower / d.noOfNonZeroPowers;
		for (var i = 0; i < d.powers.length; i++) {
			if (d.powers[i] > 0) {
				var input = surplusInputs[i];
				var params = input.params;
				var limit = params.limit();
				d.powers[i] = fitWithin(limit.getMinPower(), limit.getMaxPower(), d.powers[i] + p);
			}
		}
	}

	protected static void distributePowerByPriority(Input[] surplusInputs, DistributedMinPower d) {
		var remaining = d.remainingPower;
		for (var i = 0; i < d.powers.length; i++) {
			if (d.powers[i] > 0) {
				var input = surplusInputs[i];
				var params = input.params;
				var limit = params.limit();
				var before = d.powers[i];
				var after = fitWithin(limit.getMinPower(), limit.getMaxPower(), d.powers[i] + remaining);
				d.powers[i] = after;
				remaining -= after - before;
			}
		}
	}

	private record DistributedMinPower(int[] powers, int remainingPower, int noOfNonZeroPowers) {
		@Override
		public final String toString() {
			return toStringHelper(DistributedMinPower.class) //
					.add("powers", Arrays.toString(this.powers)) //
					.add("remainingPower", this.remainingPower) //
					.add("noOfNonZeroPowers", this.noOfNonZeroPowers) //
					.toString();
		}
	}

	private static DistributedMinPower distributeMinPower(Input[] surplusInputs, int totalDistributablePower) {
		var powers = new int[surplusInputs.length];
		var remaining = totalDistributablePower;
		var noOfNonZeroPowers = 0;
		// TODO apply Hysteresis
		for (var i = 0; i < surplusInputs.length; i++) {
			var input = surplusInputs[i];
			var param = input.params;
			if (!param.isReadyForCharging()) {
				continue;
			}
			var power = param.limit().getMinPower();
			if (power > remaining) {
				continue;
			}
			noOfNonZeroPowers++;
			remaining -= power;
			powers[i] = power;
		}
		return new DistributedMinPower(powers, remaining, noOfNonZeroPowers);
	}

	/**
	 * Calculates the total excess power, depending on the current PV production and
	 * house consumption.
	 * 
	 * @param inputs the {@link Input}s
	 * @param sum    the {@link Sum} component
	 * @return the available additional excess power for charging
	 */
	protected static int calculateTotalExcessPower(ImmutableList<Input> inputs, Sum sum) {
		var buyFromGrid = sum.getGridActivePower().orElse(0);
		var essDischarge = sum.getEssDischargePower().orElse(0);
		var evseCharge = inputs.stream() //
				.map(Input::params) //
				.map(p -> p.activePower()) //
				.filter(Objects::nonNull) //
				.mapToInt(Integer::intValue) //
				.sum();

		return max(0, evseCharge - buyFromGrid - essDischarge);
	}

	/**
	 * Calculates the total fixed power.
	 * 
	 * @param inputs the {@link Input}s
	 * @return the fixed required power for MINIMUM and FORCE mode
	 */
	protected static int calculateTotalFixedPower(ImmutableList<Input> inputs) {
		return inputs.stream() //
				.map(Input::params) //
				.filter(p -> p.isReadyForCharging()) //
				.map(p -> switch (p.actualMode()) {
				case FORCE -> p.limit().getMaxPower();
				case MINIMUM -> p.limit().getMinPower();
				case SURPLUS, ZERO -> null;
				}) //
				.filter(Objects::nonNull) //
				.mapToInt(Integer::intValue) //
				.sum();
	}
}
