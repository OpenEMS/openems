package io.openems.edge.predictor.lstmmodel.common;

import java.util.ArrayList;
import java.util.Collections;

public class DynamicItterationValue {

	public static int setIteration(ArrayList<Double> errors, int errorIndex, HyperParameters hyperParameters) {

		if (errors.isEmpty()) {
			return 10;
		}

		var minError = Collections.min(errors);
		var maxError = Collections.max(errors);
		var minIteration = 1;
		var maxIteration = 10 * hyperParameters.getEpochTrack() + 1;

		var errorValue = errors.get(errorIndex);
		var normalizedError = (errorValue - minError) / (maxError - minError);
		var iterationValue = minIteration + (normalizedError * (maxIteration - minIteration));

		return (int) Math.round(iterationValue);
	}
}
