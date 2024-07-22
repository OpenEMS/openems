package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage;

import io.openems.edge.predictor.lstmmodel.common.DataStatistics;
import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.Stage;

public class ReverseNormalizationStage implements Stage<double[], double[]> {

	private double mean = Double.NaN;
	private double standardDeviation = Double.NaN;
	private final HyperParameters hyperParameters;

	public ReverseNormalizationStage(HyperParameters hyp) {
		this.hyperParameters = hyp;
	}

	public ReverseNormalizationStage(HyperParameters hyp, double mean, double standerDeviation) {
		this.mean = mean;
		this.standardDeviation = standerDeviation;
		this.hyperParameters = hyp;
	}

	@Override
	public double[] execute(double[] input) {

		if (Double.isNaN(this.mean)) {
			this.mean = DataStatistics.getMean(input);
		}

		if (Double.isNaN(this.standardDeviation)) {
			this.standardDeviation = DataStatistics.getStandardDeviation(input);
		}
		return DataModification.reverseStandrize(input, this.mean, this.standardDeviation, this.hyperParameters);
	}
}
