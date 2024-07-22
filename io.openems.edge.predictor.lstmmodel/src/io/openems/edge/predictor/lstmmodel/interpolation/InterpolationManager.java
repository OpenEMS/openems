package io.openems.edge.predictor.lstmmodel.interpolation;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;

public class InterpolationManager {

	private ArrayList<Double> interpolated = new ArrayList<Double>();
	private ArrayList<OffsetDateTime> newDates = new ArrayList<OffsetDateTime>();

	public ArrayList<Double> getInterpolatedData() {
		return this.interpolated;
	}

	public ArrayList<OffsetDateTime> getNewDates() {
		return this.newDates;
	}

	public final static Function<ArrayList<Double>, ArrayList<Double>> nanReplacer = InterpolationManager::replaceNullWithNaN;
	public final static Function<ArrayList<Double>, Double> meanCalculator = InterpolationManager::calculateMean;

	public InterpolationManager(double[] data, HyperParameters hyperParameters) {
		var d = UtilityConversion.to1DArrayList(data);
		this.makeInterpolation(d);
	}

	public InterpolationManager(ArrayList<Double> data, HyperParameters hyperParameters) {
		this.makeInterpolation(data);
	}

	private void makeInterpolation(ArrayList<Double> data) {
		ArrayList<Double> dataDouble = replaceNullWithNaN(data);
		double mean = calculateMean(dataDouble);

		// TODO why 96
		int groupSize = 96;

		List<ArrayList<Double>> groupedData = group(dataDouble, groupSize);

		CubicalInterpolation inter = new CubicalInterpolation();

		List<ArrayList<Double>> interpolatedGroupedData = groupedData.stream()//
				.map(currentGroup -> {
					if (this.interpolationDecision(currentGroup)) {
						this.handleFirstAndLastDataPoint(currentGroup, mean);
						inter.setData(currentGroup);
						return inter.canInterpolate() ? inter.compute() : LinearInterpolation.interpolate(currentGroup);
					} else {
						return currentGroup;
					}
				}).collect(Collectors.toList());

		this.interpolated = unGroup(interpolatedGroupedData);

	}

	private void handleFirstAndLastDataPoint(ArrayList<Double> currentGroup, double mean) {
		if (Double.isNaN(currentGroup.get(0))) {
			currentGroup.set(0, mean);
		}
		if (Double.isNaN(currentGroup.get(currentGroup.size() - 1))) {
			currentGroup.set(currentGroup.size() - 1, mean);
		}
	}

	/**
	 * Checks whether interpolation is needed based on the presence of NaN values in
	 * the provided list.
	 *
	 * @param data The list of Double values to be checked.
	 * @return true if interpolation is needed (contains at least one NaN value),
	 *         false otherwise.
	 */
	private boolean interpolationDecision(ArrayList<Double> data) {
		return data.stream().anyMatch(value -> Double.isNaN(value));
	}

	/**
	 * Replaces null values with Double.NaN in the given ArrayList.
	 *
	 * @param data The ArrayList to be processed.
	 * @return A new ArrayList with null values replaced by Double.NaN.
	 */
	public static ArrayList<Double> replaceNullWithNaN(ArrayList<Double> data) {
		return data.stream()//
				.map(value -> (value == null) ? Double.NaN : value)//
				.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Calculates the mean (average) of a list of numeric values, excluding NaN
	 * values.
	 *
	 * @param data The list of numeric values from which to calculate the mean.
	 * @return The mean of the non-NaN numeric values in the input list.
	 */
	public static double calculateMean(ArrayList<Double> data) {
		if (data.isEmpty()) {
			return Double.NaN;
		}

		OptionalDouble meanOptional = data.stream()//
				.filter(value -> !Double.isNaN(value))//
				.mapToDouble(Double::doubleValue)//
				.average();

		return meanOptional.orElse(Double.NaN);
	}

	/**
	 * Ungroups a list of sublists into a single list.
	 *
	 * @param data The list of sublists to be ungrouped.
	 * @return A single list containing all elements from the sublists.
	 */
	public static ArrayList<Double> unGroup(List<ArrayList<Double>> data) {
		return data.stream()//
				.flatMap(List::stream)//
				.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Groups a list of data into sublists of a specified size. This method takes a
	 * list of data and groups it into sublists of a specified size. Each sublist
	 * will contain up to {@code groupSize} elements, except for the last sublist,
	 * which may contain fewer elements if the total number of elements is not a
	 * multiple of {@code groupSize}.
	 *
	 * @param data      The list of data to be grouped.
	 * @param groupSize The maximum number of elements in each sublist.
	 * @return A list of sublists, each containing up to {@code groupSize} elements.
	 */
	public static ArrayList<ArrayList<Double>> group(ArrayList<Double> data, int groupSize) {
		ArrayList<ArrayList<Double>> groupedData = new ArrayList<>();

		for (int i = 0; i < data.size(); i += groupSize) {
			ArrayList<Double> sublist = new ArrayList<>(data.subList(i, Math.min(i + groupSize, data.size())));
			groupedData.add(sublist);
		}
		return groupedData;
	}
}
