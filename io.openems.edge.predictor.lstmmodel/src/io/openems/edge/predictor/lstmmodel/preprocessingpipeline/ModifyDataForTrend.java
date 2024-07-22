package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;
import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;

public class ModifyDataForTrend implements Stage<Object, Object> {

	private HyperParameters hyperparameters;

	private ArrayList<OffsetDateTime> dates;

	public ModifyDataForTrend(ArrayList<OffsetDateTime> date, HyperParameters hype) {
		this.dates = date;
		this.hyperparameters = hype;

	}

	@Override
	public Object execute(Object input) {
		double[] inputData = (double[]) input;
		Object modifiedData = UtilityConversion.to2DArray(DataModification.modifyFortrendPrediction(
				UtilityConversion.to1DArrayList(inputData), this.dates, this.hyperparameters));
		// TODO Auto-generated method stub
		return modifiedData;
	}

	public void setDates(ArrayList<OffsetDateTime> date) {
		this.dates = date;
	}

}
