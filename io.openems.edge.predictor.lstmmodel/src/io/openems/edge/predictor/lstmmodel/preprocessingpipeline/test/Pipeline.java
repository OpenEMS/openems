package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.Pipeline1D.Pipeline1DImpl;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.Pipeline2D.Pipeline2DImpl;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.Pipeline3D.Pipeline3DImpl;
import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;

public interface Pipeline<O> {

	static Pipeline1D of(ArrayList<Double> data, ArrayList<OffsetDateTime> date, HyperParameters hyperParameters) {
		var dataAr = UtilityConversion.to1DArray(data);
		var dateAr = UtilityConversion.to1DArray(date);
		return new Pipeline1DImpl(hyperParameters, dataAr, dateAr);
	}

	static Pipeline1D of(ArrayList<Double> data, HyperParameters hyperParameters) {
		var dataAr = UtilityConversion.to1DArray(data);

		return new Pipeline1DImpl(hyperParameters, dataAr);
	}

	static Pipeline1D of(double[] data, OffsetDateTime[] date, HyperParameters hyperParameters) {
		return new Pipeline1DImpl(hyperParameters, data, date);
	}

	static Pipeline1D of(double[] data, HyperParameters hyperParameters) {
		return new Pipeline1DImpl(hyperParameters, data);
	}

	static Pipeline2D of(double[][] data, HyperParameters hyperParameters) {
		return new Pipeline2DImpl(hyperParameters, data);
	}

	static Pipeline3D of(double[][][] data, HyperParameters hyperParameters) {
		return new Pipeline3DImpl(hyperParameters, data);
	}

	HyperParameters getHyperParameters();

	O get();

}
