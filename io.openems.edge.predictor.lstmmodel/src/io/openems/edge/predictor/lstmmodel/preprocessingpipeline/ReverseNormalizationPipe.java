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
		try {
			double[] inputData = (double[]) input;
			double[] mean = (double[]) this.mean;
			double[] std = (double[]) this.standerDeviation;
			return DataModification.reverseStandrize(inputData, mean, std, this.hyperParameters);
		} catch (ClassCastException e) {
			try {
				double inputData = (double) input;
				double mean = (double) this.mean;
				double std = (double) this.standerDeviation;
				return DataModification.reverseStandrize(inputData, mean, std, this.hyperParameters);

			} catch (ClassCastException f) {
				try {
					double[] inputData = (double[]) input;
					double mean = (double) this.mean;
					double std = (double) this.standerDeviation;
					return DataModification.reverseStandrize(inputData, mean, std, this.hyperParameters);

				} catch (ClassCastException g) {
					f.printStackTrace();
					return null;
				}
			}
		}

	}

}
