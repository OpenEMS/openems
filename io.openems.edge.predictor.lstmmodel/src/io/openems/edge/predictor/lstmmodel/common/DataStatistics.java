package io.openems.edge.predictor.lstmmodel.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;

public class DataStatistics {

	/**
	 * Get the mean of the array.
	 * 
	 * @param data the data
	 * @return mean value
	 */
	public static double getMean(Collection<? extends Number> data) {
		return data.stream().mapToDouble(Number::doubleValue).average().orElse(0.0);
	}

	/**
	 * Calculates the mean (average) of each row in a 2D array of doubles and
	 * returns an ArrayList containing the means of each row.
	 *
	 * @param data a 2D array of doubles containing the data from which to calculate
	 *             means
	 * @return an ArrayList of Double containing the means of each row
	 */
	public static double[] getMean(double[][] data) {
		return Arrays.stream(data).mapToDouble(row -> Arrays.stream(row).average().orElse(0.0)).toArray();
	}

	/**
	 * Computes the mean (average) of an array of double values.
	 *
	 * <p>
	 * This method calculates the mean by summing all the elements in the input
	 * array and dividing by the number of elements. If the array is empty, it
	 * throws a NoSuchElementException.
	 *
	 * @param data the array of double values for which the mean is to be computed
	 * @return the mean of the input array
	 * @throws java.util.NoSuchElementException if the array is empty
	 */
	public static double getMean(double[] data) {
		return Arrays.stream(data).parallel().average().getAsDouble();
	}

	/**
	 * Calculates the standard deviation of a list of double values. This method
	 * computes the standard deviation of the provided list of double values.
	 * Standard deviation measures the amount of variation or dispersion in the
	 * data. It is calculated as the square root of the variance, which is the
	 * average of the squared differences between each data point and the mean. When
	 * stander deviation is 0, the method returns a value close to zero to avoid
	 * divisible by 0 error
	 *
	 * @param data An ArrayList of double values for which to calculate the standard
	 *             deviation.
	 * @return The standard deviation of the provided data as a double value.
	 * @throws IllegalArgumentException if the input list is empty.
	 */
	public static double getStandardDeviation(Collection<? extends Number> data) {
		double mean = getMean(data);
		double sumSquaredDeviations = data.stream().mapToDouble(x -> Math.pow(x.doubleValue() - mean, 2)).sum();
		double variance = sumSquaredDeviations / data.size();
		double stdDeviation = Math.sqrt(variance);
		return (stdDeviation == 0) ? 0.000000000000001 : stdDeviation;
	}

	/**
	 * * calculates the deviation of the data from the expected error. THis method
	 * computes the average deviation from the expected error.
	 * 
	 * @param data          the data of type numbers
	 * @param expectedError the expected error
	 * @return stdDeviation the standard deviation
	 */
	public static double getStandardDeviation(Collection<? extends Number> data, double expectedError) {
		double mean = expectedError;
		double sumSquaredDeviations = data.stream()//
				.mapToDouble(x -> Math.pow(x.doubleValue() - mean, 2))//
				.sum();
		double variance = sumSquaredDeviations / data.size();
		double stdDeviation = Math.sqrt(variance);
		return (stdDeviation == 0) ? 0.000000000000001 : stdDeviation;
	}

	/**
	 * Computes the standard deviation of an array of double values.
	 *
	 * <p>
	 * This method calculates the mean of the input array, then computes the
	 * variance by finding the average of the squared differences from the mean.
	 * Finally, it returns the square root of the variance as the standard
	 * deviation. If the standard deviation is zero, a very small positive number
	 * (1e-15) is returned to avoid returning zero.
	 *
	 * @param data the array of double values for which the standard deviation is to
	 *             be computed
	 * @return the standard deviation of the input array
	 */

	public static double getStandardDeviation(double[] data) {
		double mean = Arrays.stream(data).average().getAsDouble();
		double sumSquaredDeviations = Arrays.stream(data).map(x -> Math.pow(x - mean, 2)).sum();
		double variance = sumSquaredDeviations / data.length;
		double stdDeviation = Math.sqrt(variance);
		return (stdDeviation == 0) ? 0.000000000000001 : stdDeviation;
	}

	/**
	 * Calculates the standard deviation of each row in a 2D array of doubles and
	 * returns an ArrayList containing the standard deviations of each row.
	 *
	 * @param data a 2D array of doubles containing the data from which to calculate
	 *             standard deviations
	 * @return an ArrayList of Double containing the standard deviations of each row
	 */
	public static double[] getStandardDeviation(double[][] data) {
		return Arrays.stream(data)//
				.mapToDouble(row -> getStandardDeviation(row))//
				.toArray();
	}

	/**
	 * Computes the root mean square (RMS) error between two arrays of double
	 * values.
	 *
	 * @param original the original array of double values
	 * @param computed the computed array of double values
	 * @return the RMS error between the original and computed arrays
	 * @throws IllegalArgumentException if the arrays have different lengths
	 */
	public static double computeRms(double[] original, double[] computed) {
		if (original.length != computed.length) {
			throw new IllegalArgumentException("Arrays must have the same length");
		}

		var sumOfSquaredDifferences = IntStream.range(0, original.length)
				.mapToDouble(i -> Math.pow(original[i] - computed[i], 2))//
				.average();

		return Math.sqrt(sumOfSquaredDifferences.getAsDouble());
	}
}
