package io.openems.edge.predictor.lstmmodel.train;

import org.junit.Test;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.common.ReadAndSaveModels;
import io.openems.edge.predictor.lstmmodel.common.ReadCsv;
import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;

public class BatchImplementationTest {
	/**
	 * Batch testing.
	 */
	@Test
	public void trainInBatchtest() {

		HyperParameters hyperParameters;
		String modelName = "ConsumptionActivePower";

		hyperParameters = ReadAndSaveModels.read(modelName);

		int check = hyperParameters.getOuterLoopCount();

		for (int i = check; i <= 25; i++) {
			
			hyperParameters.setOuterLoopCount(i);

			final String pathTrain = Integer.toString(i + 4) + ".csv";
			final String pathValidate = Integer.toString(i+4) + ".csv";
			System.out.println("");

			hyperParameters.printHyperParameters();
			hyperParameters.setLearningRateLowerLimit(0.00001);
			hyperParameters.setLearningRateUpperLimit(0.001);

			System.out.println("");

			System.out.println(pathTrain);
			System.out.println(pathValidate);

			ReadCsv obj1 = new ReadCsv(pathTrain);
			final ReadCsv obj2 = new ReadCsv(pathValidate);

			var validateBatchData = DataModification.getDataInBatch(obj2.getData(), 6).get(1);
			var validateBatchDate = DataModification.getDateInBatch(obj2.getDates(), 6).get(1);

//		 ReadAndSaveModels.adapt(hyperParameters, validateBatchData,
//			 validateBatchDate);

			new TrainAndValidateBatch(
					DataModification.constantScaling(DataModification.removeNegatives(obj1.getData()), 1),
					obj1.getDates(),
					DataModification.constantScaling(DataModification.removeNegatives(validateBatchData), 1),
					validateBatchDate, hyperParameters);

			hyperParameters.setEpochTrack(0);
			hyperParameters.setBatchTrack(0);
			hyperParameters.setOuterLoopCount(hyperParameters.getOuterLoopCount() + 1);
			ReadAndSaveModels.save(hyperParameters);

		}

	}

}
