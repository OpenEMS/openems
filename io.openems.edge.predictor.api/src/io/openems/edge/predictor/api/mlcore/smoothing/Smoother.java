package io.openems.edge.predictor.api.mlcore.smoothing;

public interface Smoother {

	/**
	 * Applies a smoothing algorithm to the given values.
	 *
	 * @param values the input values to smooth
	 * @return the smoothed values
	 */
	double[] smooth(double[] values);
}
