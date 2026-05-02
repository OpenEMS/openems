package io.openems.edge.energy.api.simulation;

import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.annotations.VisibleForTesting;

import io.openems.edge.energy.api.handler.EnergyScheduleHandler;

/**
 * Utils for {@link GlobalOptimizationContext}.
 */
public final class GocUtils {

	private GocUtils() {
	}

	/**
	 * Calculates the index when Period duration switches from one-hour intervals to
	 * quarter-hour intervals.
	 *
	 * <p>
	 * The index is calculated as "6 hours" plus remaining quarters of the current
	 * hour.
	 *
	 * @param time Start-Timestamp of the Schedule
	 * @return the index
	 */
	// TODO this should be set depending on the actual calculation time and
	// quality of the best schedule result
	public static int calculatePeriodDurationHourFromIndex(ZonedDateTime time) {
		var minute = time.get(MINUTE_OF_HOUR);
		if (minute == 0) {
			minute = 60;
		}
		return 6 * 4 + (60 - minute) / 15;
	}

	/**
	 * Normalizes the preference ranks of all modes added to optimization for each
	 * ESH in the given {@link GlobalOptimizationContext}.
	 *
	 * <p>
	 * For each ESH in the context, this method:
	 * <ol>
	 * <li>Filters the modes that are considered for optimization (via
	 * {@code addToOptimizer}).</li>
	 * <li>Builds a map from mode index to its raw preference rank.</li>
	 * <li>Normalizes the ranks to a double value between 0.0 and 1.0.</li>
	 * </ol>
	 *
	 * <p>
	 * The result is a list of maps, one per ESH, mapping mode indices to their
	 * normalized preference scores.
	 *
	 * @param eshs the {@link EnergyScheduleHandler.WithDifferentModes}s containing
	 *             their modes
	 * @return a list of maps, each map corresponding to an ESH and mapping mode
	 *         indices to normalized preference ranks between 0.0 and 1.0
	 */
	public static List<Map<Integer, Double>> normalizeEshModePreferenceRanks(
			List<EnergyScheduleHandler.WithDifferentModes> eshs) {
		return eshs.stream()//
				.map(esh -> {
					final Map<Integer, Integer> modeIndexToPreferenceRank = esh.modes().streamAllIndices()//
							.filter(i -> esh.modes().addToOptimizer(i))//
							.boxed()//
							.collect(//
									HashMap::new, //
									(m, i) -> m.put(i, esh.modes().getPreferenceRank(i)), //
									Map::putAll);

					return normalizeModePreferenceRanks(modeIndexToPreferenceRank);
				})//
				.toList();
	}

	@VisibleForTesting
	static Map<Integer, Double> normalizeModePreferenceRanks(Map<Integer, Integer> modeIndexToPreferenceRank) {
		if (modeIndexToPreferenceRank.isEmpty()) {
			return Map.of();
		}

		final Function<Integer, Integer> nullToMax = v -> v != null ? v : Integer.MAX_VALUE;

		final var sortedUniquePreferenceRanks = modeIndexToPreferenceRank.values().stream()//
				.map(nullToMax)//
				.distinct()//
				.sorted()//
				.toList();

		final var preferenceRankToDenseRank = IntStream.range(0, sortedUniquePreferenceRanks.size())//
				.boxed()//
				.collect(Collectors.toMap(//
						sortedUniquePreferenceRanks::get, //
						Function.identity()));

		final int maxDenseRank = Math.max(1, preferenceRankToDenseRank.values().stream()//
				.max(Integer::compareTo)//
				.orElse(0));

		return modeIndexToPreferenceRank.entrySet().stream()//
				.collect(Collectors.toMap(//
						Map.Entry::getKey, //
						e -> preferenceRankToDenseRank.get(nullToMax.apply(e.getValue())) / (double) maxDenseRank));
	}
}
