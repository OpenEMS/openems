package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage;

import static io.openems.edge.predictor.lstmmodel.preprocessing.DataModification.groupDataByHourAndMinute;
import static io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion.to1DArrayList;
import static io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion.to3DArray;

import java.time.OffsetDateTime;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.Stage;
import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;

public class GroupByStage implements Stage<double[], double[][][]> {
	private OffsetDateTime[] dates;

	public GroupByStage(HyperParameters hype, OffsetDateTime[] date) {
		this.dates = date;
	}

	@Override
	public double[][][] execute(double[] input) {
		var inList = to1DArrayList(input);
		var groupedByHourAndMinuteList = groupDataByHourAndMinute(inList, UtilityConversion.to1DArrayList(this.dates));
		return to3DArray(groupedByHourAndMinuteList);
	}
}