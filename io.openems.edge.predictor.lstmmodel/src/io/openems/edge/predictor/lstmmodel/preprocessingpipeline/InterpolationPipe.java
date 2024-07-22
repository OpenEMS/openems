package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.interpolation.InterpolationManager;
import static io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion.to1DArrayList;
import static io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion.to1DArray;

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
		}
		return null;
	}
}
