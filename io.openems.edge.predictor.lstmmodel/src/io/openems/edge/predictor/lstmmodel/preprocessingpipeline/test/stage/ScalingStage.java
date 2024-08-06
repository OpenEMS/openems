package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage;

import java.util.Arrays;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.Stage;

public class ScalingStage implements Stage<double[], double[]> {

	private static final double MIN_SCALED = 0.2;
	private static final double MAX_SCALED = 0.8;

	private HyperParameters hyperParameter;

	public ScalingStage(HyperParameters hyperParameters) {
		this.hyperParameter = hyperParameters;
	}

	@Override
	public double[] execute(double[] value) {
		double min = this.hyperParameter.getScalingMin();
		double max = this.hyperParameter.getScalingMax();

		return Arrays.stream(value)//
				.map(v -> MIN_SCALED + ((v - min) / (max - min)) * (MAX_SCALED - MIN_SCALED))//
				.toArray();
	}

}
