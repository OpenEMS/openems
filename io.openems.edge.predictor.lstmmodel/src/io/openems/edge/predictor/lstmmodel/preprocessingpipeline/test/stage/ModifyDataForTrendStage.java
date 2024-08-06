package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.Stage;
import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;

public class ModifyDataForTrendStage implements Stage<double[], double[][]> {

	private HyperParameters hyperparameters;

	private ArrayList<OffsetDateTime> dates;

	public ModifyDataForTrendStage(ArrayList<OffsetDateTime> date, HyperParameters hype) {
		this.dates = date;
		this.hyperparameters = hype;
	}

	public ModifyDataForTrendStage(OffsetDateTime[] date, HyperParameters hype) {
		this.dates = UtilityConversion.to1DArrayList(date);
		this.hyperparameters = hype;
	}

	@Override
	public double[][] execute(double[] input) {
		double[] inputData = (double[]) input;
		var modifiedData = UtilityConversion.to2DArray(DataModification.modifyFortrendPrediction(
				UtilityConversion.to1DArrayList(inputData), this.dates, this.hyperparameters));
		return modifiedData;
	}
}
