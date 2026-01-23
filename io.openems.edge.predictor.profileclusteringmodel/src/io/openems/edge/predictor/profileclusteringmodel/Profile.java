package io.openems.edge.predictor.profileclusteringmodel;

import java.util.Arrays;
import java.util.stream.IntStream;

import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public record Profile(int clusterIndex, Series<Integer> values) {

	public static final int LENGTH = 96;

	public Profile {
		validateLength(values.size());
	}

	/**
	 * Creates a {@link Profile} from an array of values.
	 *
	 * @param clusterIndex the cluster index
	 * @param values       the array of {@value #LENGTH} values
	 * @return a new {@link Profile} instance
	 * @throws IllegalArgumentException if the array length is not {@value #LENGTH}
	 */
	public static Profile fromArray(int clusterIndex, double[] values) {
		validateLength(values.length);
		var index = IntStream.range(0, LENGTH).boxed().toList();
		var valuesList = Arrays.stream(values).boxed().toList();
		return new Profile(clusterIndex, new Series<Integer>(index, valuesList));
	}

	private static void validateLength(int actualLength) {
		if (actualLength != LENGTH) {
			throw new IllegalArgumentException(
					"Profile must contain exactly " + LENGTH + " values (15-min resolution), but got " + actualLength);
		}
	}
}
