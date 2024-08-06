package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.Stage;

public class NormalizeStage implements Stage<double[][][], double[][][]> {

	private HyperParameters hyperParameters;

	public NormalizeStage(HyperParameters hyper) {
		this.hyperParameters = hyper;
	}

	@Override
	public double[][][] execute(double[][][] inputData) {
		double[][] train = inputData[0];
		double[] target = inputData[1][0];
		double[][] normTrain = DataModification.normalizeData(train, this.hyperParameters);
		double[] normTarget = DataModification.normalizeData(train, target, this.hyperParameters);
		double[][][] temp1 = new double[2][][];
		double[][] temp2 = new double[1][];
		temp2[0] = normTarget;
		temp1[0] = normTrain;
		temp1[1] = temp2;
		return temp1;
	}

}
