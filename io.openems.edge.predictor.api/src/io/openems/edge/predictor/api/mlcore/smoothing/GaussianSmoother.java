package io.openems.edge.predictor.api.mlcore.smoothing;

import java.util.Arrays;

public class GaussianSmoother implements Smoother {

	private final double[] kernel;

	public GaussianSmoother(double[] kernel) {
		if (kernel == null || kernel.length == 0) {
			throw new IllegalArgumentException("Kernel must not be null or empty");
		}
		this.kernel = Arrays.copyOf(kernel, kernel.length);
		final double sum = Arrays.stream(this.kernel).sum();
		if (Math.abs(sum) < 1e-10) {
			throw new IllegalArgumentException("Kernel sum must not be zero");
		}
		Arrays.setAll(this.kernel, i -> this.kernel[i] / sum);
	}

	/**
	 * Applies Gaussian smoothing with sum preservation to the given values using
	 * convolution.
	 * 
	 * <p>
	 * Uses edge extension: boundary values are repeated when the kernel extends
	 * beyond the array bounds.
	 * 
	 * @param values the input values to smooth
	 * @return smoothed and sum-preserved values of the same length as input
	 * @throws IllegalArgumentException if values is null or empty
	 */
	@Override
	public double[] smooth(double[] values) {
		if (values == null || values.length == 0) {
			throw new IllegalArgumentException("Input values must not be null or empty");
		}

		final double[] smoothed = new double[values.length];
		final int radius = this.kernel.length / 2;

		for (int i = 0; i < values.length; i++) {
			double sum = 0.0;
			for (int k = 0; k < this.kernel.length; k++) {
				final int idx = Math.clamp(i + k - radius, 0, values.length - 1);
				sum += this.kernel[k] * values[idx];
			}
			smoothed[i] = sum;
		}

		final double originalSum = Arrays.stream(values).sum();
		final double smoothedSum = Arrays.stream(smoothed).sum();

		if (Math.abs(smoothedSum) < 1e-10) {
			return smoothed;
		}

		final double factor = originalSum / smoothedSum;
		return Arrays.stream(smoothed).map(v -> v * factor).toArray();
	}
}
