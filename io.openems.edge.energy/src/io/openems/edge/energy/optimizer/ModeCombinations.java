package io.openems.edge.energy.optimizer;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EshWithDifferentModes;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;

/**
 * Handles combinations of {@link EshWithDifferentModes} Modes.
 * 
 * <p>
 * First {@link ModeCombination} (get via {@link #getDefault()}) is the default
 * of of all ESHs.
 */
public record ModeCombinations(ImmutableList<ModeCombination> combinations) {

	public static final ImmutableList<List<String>> INFEASIBLE_COMBINATIONS = ImmutableList.<List<String>>builder() //
			.add(List.of("Evse.Controller.Single:SURPLUS", "Controller.Ess.Time-Of-Use-Tariff:DELAY_DISCHARGE")) //
			.add(List.of("Evse.Controller.Single:SURPLUS", "Controller.Ess.Time-Of-Use-Tariff:CHARGE_GRID")) //
			.build();

	/**
	 * Holds one Mode.
	 */
	public static record Mode(EnergyScheduleHandler.WithDifferentModes esh, int index, String name) {

		protected static Mode from(EnergyScheduleHandler.WithDifferentModes esh, int index) {
			final var b = new StringBuilder();
			final var factoryPid = esh.getParentFactoryPid();
			if (!factoryPid.isBlank()) {
				b.append(factoryPid).append(":");
			}
			final var name = b.append(esh.modes().getAsString(index)).toString();
			return new Mode(esh, index, name);
		}

		@Override
		public final String toString() {
			return this.name();
		}
	}

	public static record ModeCombination(//
			int index, //
			// mode(0) = eshsWithDifferentModes[0],
			// mode(1) = eshsWithDifferentModes[1], etc.
			ImmutableList<Mode> modes) {

		/**
		 * Gets the {@link Mode} for {@link EnergyScheduleHandler} at index i.
		 * 
		 * @param i index
		 * @return the {@link Mode}
		 */
		public final Mode mode(int i) {
			return this.modes.get(i);
		}
	}

	protected static class Builder {

		private final List<List<String>> infeasible = new ArrayList<List<String>>();
		private final List<ModeCombination> combinations = new ArrayList<ModeCombination>();

		private int nextIndex = 0;

		public Builder addInfeasibles(List<List<String>> infeasibleCombinations) {
			this.infeasible.addAll(infeasibleCombinations);
			return this;
		}

		public Builder addInfeasible(String... infeasibleCombination) {
			this.infeasible.add(Arrays.asList(infeasibleCombination));
			return this;
		}

		public synchronized Builder addCombination(List<Mode> modes) {
			if (this.infeasible.stream() // Is Combination marked as infeasible?
					.anyMatch(ifc -> ifc.stream() //
							.allMatch(c -> modes.stream() //
									.anyMatch(m -> c.equals(m.name))))) {
				return this;
			}
			if (this.combinations.stream() // Is Combination already existing?
					.anyMatch(c -> c.modes.containsAll(modes))) {
				return this;
			}

			var combination = new ModeCombination(this.nextIndex++, ImmutableList.copyOf(modes));
			this.combinations.add(combination);
			return this;
		}

		public ModeCombinations build() {
			return new ModeCombinations(ImmutableList.copyOf(this.combinations));
		}
	}

	/**
	 * Builds all combinations of Modes; excluding the provided
	 * infeasibleCombinations.
	 * 
	 * @param eshs                   the list of {@link EnergyScheduleHandler}s
	 * @param infeasibleCombinations a list of infeasible combinations. String is in
	 *                               the format {@link Mode#name()}.
	 * @return list of {@link ModeCombination}s
	 */
	public static ModeCombinations fromGlobalOptimizationContext(GlobalOptimizationContext goc) {
		final var result = new ModeCombinations.Builder() //
				.addInfeasibles(INFEASIBLE_COMBINATIONS);

		// Set first ModeCombination as default (index = 0) Mode for all ESHs.
		result.addCombination(goc.eshsWithDifferentModes().stream() //
				.filter(esh -> !esh.modes().isEmpty()) //
				.map(esh -> Mode.from(esh, 0)) //
				.toList());

		var cp = Lists.cartesianProduct(//
				goc.eshsWithDifferentModes().stream() //
						.map(esh -> {
							var modes = esh.modes();
							return modes.streamAllIndices() //
									.filter(i -> modes.addToOptimizer(i)) //
									.mapToObj(i -> Mode.from(esh, i)) //
									.collect(toImmutableList());
						}) //
						.collect(toImmutableList())); //
		cp.forEach(mss -> result.addCombination(mss));
		return result.build();
	}

	/**
	 * Gets the default {@link ModeCombinations} at index 0.
	 * 
	 * @return the {@link ModeCombinations} or null if list is empty
	 */
	public ModeCombination getDefault() {
		if (this.combinations.isEmpty()) {
			return null;
		}
		return this.combinations.get(0);
	}

	/**
	 * Gets the {@link ModeCombinations} at given index.
	 * 
	 * @param index the index
	 * @return the {@link ModeCombinations} or null if list is empty
	 */
	public ModeCombination get(int index) {
		if (this.combinations.isEmpty()) {
			return null;
		}
		return this.combinations.get(index);
	}

	/**
	 * Gets the number of possible {@link ModeCombination}s.
	 * 
	 * @return size
	 */
	public int size() {
		return this.combinations.size();
	}

	public boolean isEmpty() {
		return this.combinations.isEmpty();
	}

	/**
	 * Finds the matching 'old' {@link ModeCombination} in the current list of
	 * {@link ModeCombination}s.
	 * 
	 * @param previousModeCombination the 'old' {@link ModeCombination}
	 * @param modeCombinations        the current list of {@link ModeCombination}s
	 * @return matching {@link ModeCombination} or default
	 */
	public ModeCombination getMatchingOrDefault(ModeCombination previousModeCombination) {
		if (previousModeCombination == null) {
			return this.getDefault();
		}
		for (var thisMode : this.combinations) {
			if (previousModeCombination.modes().stream() //
					.allMatch(prev -> thisMode.modes().stream() //
							.anyMatch(m -> m.name().equals(prev.name())))) {
				// Found matching ModeCombination -> return
				return thisMode;
			}
		}
		return this.getDefault();
	}

	/**
	 * Gets the {@link ModeCombination} from the given {@link EnergyScheduleHandler}
	 * indexes.
	 * 
	 * @param indexes the indexes
	 * @return the {@link ModeCombination}
	 */
	public ModeCombination getFromModeIndexesOrDefault(int[] indexes) {
		for (var m : this.combinations) {
			if (Arrays.equals(m.modes.stream().mapToInt(Mode::index).toArray(), indexes)) {
				return m;
			}
		}
		return this.getDefault(); // not found
	}
}