package io.openems.edge.predictor.api.mlcore.interpolation;

import java.util.List;

public interface Interpolator {

	/**
	 * Interpolates a value at the specified index from the provided list of values.
	 * 
	 * @param index  the position at which to interpolate the value
	 * @param values the list of Double values used for interpolation
	 * @return the interpolated Double value, or null if interpolation is not
	 *         possible
	 */
	public Double interpolate(int index, List<Double> values);
}
