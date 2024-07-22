package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage;

import java.time.OffsetDateTime;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.interpolation.InterpolationManager;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.Stage;
import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;

public class InterpolationStage implements Stage<double[], double[]> {
	private HyperParameters hyperParameters;

	public InterpolationStage(HyperParameters hype, OffsetDateTime[] dates) {
		this.hyperParameters = hype;
	}

	@Override
	public double[] execute(double[] inputData) {
		InterpolationManager inter = new InterpolationManager(inputData, this.hyperParameters);
		return UtilityConversion.to1DArray(inter.getInterpolatedData());
	}
}
