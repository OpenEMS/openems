package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import static io.openems.edge.predictor.lstmmodel.preprocessing.DataModification.modifyFortrendPrediction;
import static io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion.to1DArrayList;
import static io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion.to2DArray;

public class ModifyDataForTrend implements Stage<Object, Object> {

	private HyperParameters hyperparameters;

	private ArrayList<OffsetDateTime> dates;

	public ModifyDataForTrend(ArrayList<OffsetDateTime> date, HyperParameters hype) {
		this.dates = date;
		this.hyperparameters = hype;
	}

	@Override
	public Object execute(Object input) {

		if (input instanceof double[] inputData) {
			var inList = to1DArrayList(inputData);
			var modified = modifyFortrendPrediction(inList, this.dates, this.hyperparameters);
			return to2DArray(modified);
		}
		return null;
	}

	public void setDates(ArrayList<OffsetDateTime> date) {
		this.dates = date;
	}
}
