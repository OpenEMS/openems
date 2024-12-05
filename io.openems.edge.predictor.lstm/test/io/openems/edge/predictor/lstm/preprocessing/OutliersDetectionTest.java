package io.openems.edge.predictor.lstm.preprocessing;

import org.junit.Test;

import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.preprocessing.DataModification;
import io.openems.edge.predictor.lstm.preprocessing.FilterOutliers;
import io.openems.edge.predictor.lstm.utilities.UtilityConversion;

public class OutliersDetectionTest {

	@Test
	public void test() {
		HyperParameters hyperParameters = new HyperParameters();
		double[] data = { 1, 2, 4, 8, 1000, 4000, 9, 8, 7, 2, 3, 8, 7, 7, 9, 7 };
		var dataScaled = DataModification.scale(data, hyperParameters.getScalingMin(), hyperParameters.getScalingMax());
		var dataWithoutOutliers = FilterOutliers.filterOutlier(dataScaled);
		System.out.println(UtilityConversion.to1DArrayList(DataModification.scaleBack(dataWithoutOutliers,
				hyperParameters.getScalingMin(), hyperParameters.getScalingMax())));
	}

}
