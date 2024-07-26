package io.openems.edge.predictor.lstmmodel.train;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import org.junit.Test;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.common.ReadAndSaveModels;
import io.openems.edge.predictor.lstmmodel.common.ReadCsv;
import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;

public class BatchImplementationTest {
	/**
	 * Batch testing.
	 */
	// @Test
	public void trainInBatchtest() {

		HyperParameters hyperParameters;
		String modelName = "ConsumptionActivePower";

		hyperParameters = ReadAndSaveModels.read(modelName);

		int check = hyperParameters.getOuterLoopCount();

		for (int i = check; i <= 25; i++) {

			hyperParameters.setOuterLoopCount(i);

			final String pathTrain = Integer.toString(4) + ".csv";
			final String pathValidate = Integer.toString(4) + ".csv";
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

			// ReadAndSaveModels.adapt(hyperParameters, validateBatchData,
			// validateBatchDate);

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

	@Test
	public void trainInBatchtestMultivarient() {

		HyperParameters hyperParameters;
		String modelName = "ConsumptionActivePower";

		hyperParameters = ReadAndSaveModels.read(modelName);

		int check = hyperParameters.getOuterLoopCount();

		for (int i = check; i <= 25; i++) {

			hyperParameters.setOuterLoopCount(i);

			final String pathTrain = Integer.toString(i + 4) + ".csv";
			final String pathValidate = Integer.toString(i + 4) + ".csv";
			System.out.println("");

			hyperParameters.printHyperParameters();
			hyperParameters.setLearningRateLowerLimit(0.00001);
			hyperParameters.setLearningRateUpperLimit(0.001);

			System.out.println("");

			System.out.println(pathTrain);
			System.out.println(pathValidate);

			ReadCsv obj1 = new ReadCsv(pathTrain);
			final ReadCsv obj2 = new ReadCsv(pathValidate);

			var trainingref = this.generateRefrence(obj1.getDates());
			var validationref = this.generateRefrence(obj2.getDates());

			var trainingData = DataModification.elementWiseMultiplication(trainingref, obj1.getData());
			var validationData = DataModification.elementWiseMultiplication(validationref, obj2.getData());

			var validateBatchData = DataModification.getDataInBatch(validationData, 6).get(1);
			var validateBatchDate = DataModification.getDateInBatch(obj2.getDates(), 6).get(1);

			// ReadAndSaveModels.adapt(hyperParameters, validateBatchData,
			// validateBatchDate);

			new TrainAndValidateBatch(trainingData, obj1.getDates(), validateBatchData, validateBatchDate,
					hyperParameters);

			hyperParameters.setEpochTrack(0);
			hyperParameters.setBatchTrack(0);
			hyperParameters.setOuterLoopCount(hyperParameters.getOuterLoopCount() + 1);
			ReadAndSaveModels.save(hyperParameters);

		}

	}

	/**
	 * Generates a list of reference values based on the provided list of
	 * OffsetDateTime objects. Each reference value is calculated using the cosine
	 * of the angle corresponding to the time of day represented by each
	 * OffsetDateTime. The formula used is: - One hour corresponds to 360/24
	 * degrees. - One minute corresponds to 360/(24*60) degrees.
	 *
	 * @param date an ArrayList of OffsetDateTime objects representing the date and
	 *             time.
	 * @return an ArrayList of Double values representing the generated reference
	 *         values.
	 */
	public ArrayList<Double> generateRefrence(ArrayList<OffsetDateTime> date) {
		ArrayList<Double> data = new ArrayList<Double>();

		for (int i = 0; i < date.size(); i++) {
			// Extract the hour and minute from the current OffsetDateTime.
			int hour = date.get(i).getHour();
			int minute = date.get(i).getMinute();

			// Calculate the degree values for the hour and minute.
			double deg = 360.0 * hour / 24.0;
			double degDec = 360.0 * minute / (24.0 * 60.0);
			double angle = deg + degDec;

			// Calculate the cosine of the angle in radians and add 1.5 to the result.
			double addVal = Math.cos(Math.toRadians(angle));
			data.add(1.5 + addVal);
		}
		return data;
	}

}