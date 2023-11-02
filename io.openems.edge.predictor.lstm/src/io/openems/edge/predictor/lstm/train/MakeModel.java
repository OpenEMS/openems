package io.openems.edge.predictor.lstm.train;

import java.time.OffsetDateTime;
import java.util.ArrayList;
//import io.openems.edge.predictor.lstmmodel.interpolation.InterpolationManager;
//import io.openems.edge.predictor.lstmmodel.model.SaveModel;
//import io.openems.edge.predictor.lstmmodel.preprocessing.PreProcessingImpl;
//import io.openems.edge.predictor.lstmmodel.preprocessing.GroupBy;
//import io.openems.edge.predictor.lstmmodel.preprocessing.Normalize;
//import io.openems.edge.predictor.lstmmodel.preprocessing.Suffle;
//import io.openems.edge.predictor.lstmmodel.util.Engine;
//import io.openems.edge.predictor.lstmmodel.util.Engine.EngineBuilder;
import java.util.Collections;

import io.openems.edge.predictor.lstm.common.DataModification;
import io.openems.edge.predictor.lstm.common.ReadModels;
import io.openems.edge.predictor.lstm.common.SaveModel;
import io.openems.edge.predictor.lstm.interpolation.InterpolationManager;
import io.openems.edge.predictor.lstm.preprocessing.GroupBy;
import io.openems.edge.predictor.lstm.preprocessing.PreProcessingImpl;
import io.openems.edge.predictor.lstm.preprocessing.Suffle;
import io.openems.edge.predictor.lstm.util.Engine;
import io.openems.edge.predictor.lstm.util.Engine.EngineBuilder;

public class MakeModel {
	private String pathSeasonality = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\";
	private String pathTrend = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\";

	public MakeModel(ArrayList<Double> data, ArrayList<OffsetDateTime> date, Integer itterNumb) {
		this.trainSeasonality(data, date, itterNumb);
		//this.trinTrend(data, date, itterNumb);

	}

	/**
	 * Trains a seasonality model using the provided data and parameters.
	 * 
	 * @param data      The time series data to train the model on.
	 * @param date      The corresponding timestamps for the data.
	 * @param itterNumb The number of iterations for training the model.
	 */

	public void trainSeasonality(ArrayList<Double> data, ArrayList<OffsetDateTime> date, int itterNumb) {
		ArrayList<Double> values;
		ArrayList<OffsetDateTime> dates;
		ArrayList<ArrayList<ArrayList<OffsetDateTime>>> dateGroupedByMinute = new ArrayList<ArrayList<ArrayList<OffsetDateTime>>>();
		ArrayList<ArrayList<ArrayList<Double>>> dataGroupedByMinute = new ArrayList<ArrayList<ArrayList<Double>>>();
		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> weightMatrix = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
		ArrayList<ArrayList<Double>> weight1 = new ArrayList<ArrayList<Double>>();
		double minOfTrainingData;
		final double maxOfTrainingData;

		values = data;
		dates = date;
		int windowsSize = 7;

		// compute interpolation
		InterpolationManager inter = new InterpolationManager(values);
		minOfTrainingData = Collections.min(inter.getInterpolatedData());
		maxOfTrainingData = Collections.max(inter.getInterpolatedData());

		// Grouping the interpolated data by hour
		GroupBy groupAsHour = new GroupBy(inter.getInterpolatedData(), dates);
		groupAsHour.hour();

		// Grouping data by minute

		for (int i = 0; i < groupAsHour.getDataGroupedByHour().size(); i++) {

			GroupBy groupAsMinute = new GroupBy(groupAsHour.getDataGroupedByHour().get(i),
					groupAsHour.getDateGroupedByHour().get(i));
			groupAsMinute.minute();
			dataGroupedByMinute.add(groupAsMinute.getDataGroupedByMinute());
			dateGroupedByMinute.add(groupAsMinute.getDateGroupedByMinute());
		}
		/**
		 * compute model
		 */

		int k = 0;

		for (int i = 0; i < dataGroupedByMinute.size(); i++) {
			if (itterNumb == 0) {
				weight1 = this.generateInitialWeightMatrix(windowsSize);
			} else {
				String path = this.pathSeasonality + Integer.toString(itterNumb - 1) + "seasonality.txt";
				ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allModel = ReadModels.getModelForSeasonality(path);
				weight1 = allModel.get(allModel.size() - 1).get(k);
			}

			System.out.println("");
			System.out.println(i + 1 + "/" + dataGroupedByMinute.size());
			for (int j = 0; j < dataGroupedByMinute.get(i).size(); j++) {

				PreProcessingImpl preprocessing = new PreProcessingImpl(
						DataModification.scale(dataGroupedByMinute.get(i).get(j), minOfTrainingData, maxOfTrainingData),
						windowsSize);

				try {

					double[][] trainData = preprocessing.getFeatureData(
							preprocessing.getTrainTestSplit().getTrainLowerIndex(),
							preprocessing.getTrainTestSplit().getTrainUpperIndex());

					double[][] validateData = preprocessing.getFeatureData(
							preprocessing.getTrainTestSplit().getValidateLowerIndex(),
							preprocessing.getTrainTestSplit().getValidateUpperIndex());

					double[] trainTarget = preprocessing.getTargetData(
							preprocessing.getTrainTestSplit().getTrainLowerIndex(),
							preprocessing.getTrainTestSplit().getTrainUpperIndex());

					double[] validateTarget = preprocessing.getTargetData(
							preprocessing.getTrainTestSplit().getValidateLowerIndex(),
							preprocessing.getTrainTestSplit().getValidateUpperIndex());

					Suffle obj1 = new Suffle(trainData, trainTarget);
					Suffle obj2 = new Suffle(validateData, validateTarget);

					Engine model = new EngineBuilder() //
							.setInputMatrix(DataModification.normalizeData(obj1.getData())) //
							.setTargetVector(obj1.getTarget()) //
							.setValidateData(DataModification.normalizeData(obj2.getData())) //
							.setValidateTarget(obj2.getTarget()) //
							.setValidatorCounter(25000 * 10000)//
							.build();

					int epochs = 1000;
					model.fit(epochs, weight1);
					// weight1 = model.getWeights().get(model.getWeights().size() - 1);

					weightMatrix.add(model.getWeights());
				} catch (Exception e) {
					e.printStackTrace();

				}

			}

			/**
			 * saving Model as .txt file
			 */

		}
		SaveModel.saveModels(weightMatrix, Integer.toString(itterNumb) + "seasonality.txt");

		System.out.println("Model Saved");

	}

	/**
	 * Trains a trend model using the provided data and parameters.
	 *
	 * @param data      The time series data to train the trend model on.
	 * @param date      The corresponding timestamps for the data.
	 * @param itterNumb The number of iterations for training the model.
	 */

	public void trinTrend(ArrayList<Double> data, ArrayList<OffsetDateTime> date, int itterNumb) {

		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> weightMatrix = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
		ArrayList<ArrayList<Double>> weight1 = new ArrayList<ArrayList<Double>>();
		int windowsSize = 7;

		if (itterNumb == 0) {
			weight1 = this.generateInitialWeightMatrix(windowsSize);

		} else {
			String path = this.pathTrend + Integer.toString(itterNumb - 1) + "Trend.txt";
			ArrayList<ArrayList<ArrayList<Double>>> dataList = ReadModels.getModelForTrend(path);
			weight1 = dataList.get(0);

		}

		// compute interpolation
		InterpolationManager inter = new InterpolationManager(data);
		double minOfTrainingData = Collections.min(inter.getInterpolatedData());
		double maxOfTrainingData = Collections.max(inter.getInterpolatedData());

		// weight1 = mod.dataList.get(0);
		// System.out.println(mod);

		PreProcessingImpl preprocessing = new PreProcessingImpl(
				DataModification.scale(data, minOfTrainingData, maxOfTrainingData), windowsSize);

		try {

			double[][] trainData = preprocessing.getFeatureData(preprocessing.getTrainTestSplit().getTrainLowerIndex(),
					preprocessing.getTrainTestSplit().getTrainUpperIndex());

			double[][] validateData = preprocessing.getFeatureData(
					preprocessing.getTrainTestSplit().getValidateLowerIndex(),
					preprocessing.getTrainTestSplit().getValidateUpperIndex());

			double[] trainTarget = preprocessing.getTargetData(preprocessing.getTrainTestSplit().getTrainLowerIndex(),
					preprocessing.getTrainTestSplit().getTrainUpperIndex());

			double[] validateTarget = preprocessing.getTargetData(
					preprocessing.getTrainTestSplit().getValidateLowerIndex(),
					preprocessing.getTrainTestSplit().getValidateUpperIndex());

			Suffle obj1 = new Suffle(trainData, trainTarget);
			Suffle obj2 = new Suffle(validateData, validateTarget);

			Engine model = new EngineBuilder() //
					.setInputMatrix(DataModification.normalizeData(obj1.getData())) //
					.setTargetVector(obj1.getTarget()) //
					.setValidateData(DataModification.normalizeData(obj2.getData())) //
					.setValidateTarget(obj2.getTarget()) //
					.build();

			int epochs = 1000;
			model.fit(epochs, weight1);
			// weight1 = model.getWeights().get(model.getWeights().size() - 1);
			weightMatrix.add(model.getWeights());
		} catch (Exception e) {
			e.printStackTrace();

		}

		SaveModel.saveModels(weightMatrix, Integer.toString(itterNumb) + "trend.txt");

	}

	/**
	 * Generates an initial weight matrix for a neural network with specified
	 * parameters.
	 *
	 * @param windowSize The size of the window or context for the neural network.
	 * @return An ArrayList of ArrayLists containing the initial weight matrix.
	 */

	public ArrayList<ArrayList<Double>> generateInitialWeightMatrix(int windowSize) {
		ArrayList<ArrayList<Double>> initialWeight = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> temp1 = new ArrayList<Double>();
		ArrayList<Double> temp2 = new ArrayList<Double>();
		ArrayList<Double> temp3 = new ArrayList<Double>();
		ArrayList<Double> temp4 = new ArrayList<Double>();
		ArrayList<Double> temp5 = new ArrayList<Double>();
		ArrayList<Double> temp6 = new ArrayList<Double>();
		ArrayList<Double> temp7 = new ArrayList<Double>();
		ArrayList<Double> temp8 = new ArrayList<Double>();

		for (int i = 1; i <= windowSize; i++) {
			double wi = 1;
			double wo = 1;
			double wz = 1;
			final double ri = -10;
			final double ro = -10;
			final double rz = -10;
			final double ct = 0;
			final double yt = 0;

			temp1.add(wi);
			temp2.add(wo);
			temp3.add(wz);
			temp4.add(ri);
			temp5.add(ro);
			temp6.add(rz);
			temp7.add(yt);
			temp8.add(ct);

		}
		initialWeight.add(temp1);
		initialWeight.add(temp2);
		initialWeight.add(temp3);
		initialWeight.add(temp4);
		initialWeight.add(temp5);
		initialWeight.add(temp6);
		initialWeight.add(temp7);
		initialWeight.add(temp8);

		return initialWeight;

	}

}
