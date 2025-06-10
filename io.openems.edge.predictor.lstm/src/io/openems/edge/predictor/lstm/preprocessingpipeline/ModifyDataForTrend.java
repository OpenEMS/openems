package io.openems.edge.predictor.lstm.preprocessingpipeline;

import static io.openems.edge.predictor.lstm.preprocessing.DataModification.modifyFortrendPrediction;
import static io.openems.edge.predictor.lstm.utilities.UtilityConversion.to1DArrayList;
import static io.openems.edge.predictor.lstm.utilities.UtilityConversion.to2DArray;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.edge.predictor.lstm.common.HyperParameters;

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
			try {
				var inList = to1DArrayList(inputData);
				var modified = modifyFortrendPrediction(inList, this.dates, this.hyperparameters);
				return to2DArray(modified);
			} catch (Exception e) {
				throw new RuntimeException("Error processing input data", e);
			}
		} else {
			throw new IllegalArgumentException("Input must be an instance of double[]");
		}
	}

	public void setDates(ArrayList<OffsetDateTime> date) {
		this.dates = date;
	}
}
