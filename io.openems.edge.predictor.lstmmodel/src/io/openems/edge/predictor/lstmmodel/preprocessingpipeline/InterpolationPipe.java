package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.interpolation.InterpolationManager;
import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;

public class InterpolationPipe implements Stage<Object, Object> {
	private HyperParameters hyperParameters;

	public InterpolationPipe(HyperParameters hype, ArrayList<OffsetDateTime> dates) {
		this.hyperParameters = hype;
	}

	@Override
	public Object execute(Object input) {
		double[] inputData = (double[]) input;
		InterpolationManager inter = new InterpolationManager(UtilityConversion.to1DArrayList(inputData),
				this.hyperParameters);
		return UtilityConversion.to1DArray(inter.getInterpolatedData());
	}
}
