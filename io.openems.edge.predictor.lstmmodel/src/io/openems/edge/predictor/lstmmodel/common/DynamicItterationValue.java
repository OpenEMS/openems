package io.openems.edge.predictor.lstmmodel.common;

import java.util.ArrayList;
import java.util.Collections;

public class DynamicItterationValue {

	public static int setItter(ArrayList<Double> error, int errorIndex, HyperParameters hyperParameters) {

		if (error.isEmpty()) {
			return 10;
		}
		double minErrorVal = Collections.min(error);
		double maxErrorVal = Collections.max(error);
		int minItterVal = 1;
		int maxItteeVal = 10 * hyperParameters.getEpochTrack() + 1;
		var val = minItterVal
				+ (((error.get(errorIndex) - minErrorVal) / (maxErrorVal - minErrorVal)) * (maxItteeVal - minItterVal));

		return (int) val;

	}

}
