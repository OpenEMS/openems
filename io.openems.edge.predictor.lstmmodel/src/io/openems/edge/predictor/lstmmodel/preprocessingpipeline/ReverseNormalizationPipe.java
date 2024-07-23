package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;

public class ReverseNormalizationPipe implements Stage<Object, Object> {
	private Object mean;
	private Object standerDeviation;
	private HyperParameters hyperParameters;

	public ReverseNormalizationPipe(Object average, Object std, HyperParameters hyp) {
		this.mean = average;
		this.standerDeviation = std;
		this.hyperParameters = hyp;
	}

	@Override
	public Object execute(Object input) {
		if (input instanceof double[] inputArray) {
			if (this.mean instanceof double[] meanArray //
					&& this.standerDeviation instanceof double[] sdArray) {
				return DataModification.reverseStandrize(inputArray, meanArray, sdArray, this.hyperParameters);
			} else if (this.mean instanceof Double meanValue //
					&& this.standerDeviation instanceof Double sdValue) {
				return DataModification.reverseStandrize(inputArray, meanValue, sdValue, this.hyperParameters);
			} else {
				return null;
			}
		} else if (input instanceof Double inputArray) {
			double mean = (double) this.mean;
			double std = (double) this.standerDeviation;
			return DataModification.reverseStandrize(inputArray, mean, std, this.hyperParameters);
		} else {
			return null;
		}
	}
}
