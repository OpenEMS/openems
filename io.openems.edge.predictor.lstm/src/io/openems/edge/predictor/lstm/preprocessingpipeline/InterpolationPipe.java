package io.openems.edge.predictor.lstm.preprocessingpipeline;

import static io.openems.edge.predictor.lstm.utilities.UtilityConversion.to1DArray;
import static io.openems.edge.predictor.lstm.utilities.UtilityConversion.to1DArrayList;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.interpolation.InterpolationManager;

public class InterpolationPipe implements Stage<Object, Object> {
	private HyperParameters hyperParameters;

	public InterpolationPipe(HyperParameters hype, ArrayList<OffsetDateTime> dates) {
		this.hyperParameters = hype;
	}

	@Override
	public Object execute(Object input) {
		if (input instanceof double[] in) {
			var inList = to1DArrayList(in);
			var inter = new InterpolationManager(inList, this.hyperParameters);
			return to1DArray(inter.getInterpolatedData());
		} else {
			throw new IllegalArgumentException("Input must be an instance of double[]");
		}
	}
}
