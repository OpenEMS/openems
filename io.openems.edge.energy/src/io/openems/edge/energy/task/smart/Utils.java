package io.openems.edge.energy.task.smart;

import java.util.ArrayList;
import java.util.List;

import io.openems.edge.common.type.TypeUtils;

public class Utils {

	private Utils() {
	}

	/**
	 * Sums quarterly values (array of 96 Integer values) to hourly values (array of
	 * 24 Integer values).
	 * 
	 * @param values the values
	 * @return array of Integers
	 */
	public static Integer[] sumQuartersToHours(Integer[] values) {
		List<Integer> result = new ArrayList<>();
		Integer sum = null;
		for (var i = 0; i < values.length; i++) {
			if (i % 4 == 0) {
				sum = null;
			}
			sum = TypeUtils.sum(sum, values[i]);
			if (i % 4 == 3) {
				result.add(sum);
			}
		}
		return result.stream().toArray(Integer[]::new);
	}

	/**
	 * Averages quarterly values (array of 96 Float values) to hourly values (array
	 * of 24 Float values).
	 * 
	 * @param values the values
	 * @return array of Floats
	 */
	public static Float[] avgQuartersToHours(Float[] values) {
		List<Float> result = new ArrayList<>();
		Float sum = 0F;
		for (var i = 0; i < values.length; i++) {
			if (i % 4 == 0) {
				sum = 0F;
			}
			sum = TypeUtils.sum(sum, values[i]);
			if (i % 4 == 3) {
				if (sum == null) {
					result.add(null);
				} else {
					result.add(sum / 4);
				}
			}
		}
		return result.stream().toArray(Float[]::new);
	}

}
