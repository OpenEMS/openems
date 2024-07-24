package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

import static io.openems.edge.predictor.lstmmodel.preprocessing.DataModification.groupDataByHourAndMinute;
import static io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion.to1DArrayList;
import static io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion.to3DArray;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;

public class GroupbyPipe implements Stage<Object, Object> {
	private ArrayList<OffsetDateTime> dates;

	public GroupbyPipe(HyperParameters hype, ArrayList<OffsetDateTime> date) {
		this.dates = date;
	}

	@Override
	public Object execute(Object input) {
		if (input instanceof double[] in) {
			var inList = to1DArrayList(in);
			var groupedByHourAndMinuteList = groupDataByHourAndMinute(inList, this.dates);
			return to3DArray(groupedByHourAndMinuteList);
		} else {
			throw new IllegalArgumentException("Input must be an instance of double[]");
		}
	}
}