package io.openems.edge.predictor.lstm.train;

import static io.openems.edge.predictor.lstm.utilities.UtilityConversion.to1DArray;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.edge.predictor.lstm.common.DynamicItterationValue;
import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.preprocessingpipeline.PreprocessingPipeImpl;
import io.openems.edge.predictor.lstm.util.Engine.EngineBuilder;

public class MakeModel {

	public static final String SEASONALITY = "seasonality";
	public static final String TREND = "trend";

	/**
	 * Trains the trend model using the specified data, timestamps, and
	 * hyperparameters. The training process involves preprocessing the data for
	 * short-term prediction, generating initial weights, and fitting the model for
	 * each modified data segment. The trained weights are saved to the model file.
	 *
	 * @param data            The ArrayList of Double values representing the
	 *                        time-series data.
	 * @param date            The ArrayList of OffsetDateTime objects corresponding
	 *                        to the timestamps of the data.
	 * @param hyperParameters The hyperparameters configuration for training the
	 *                        trend model.
	 * @return weightMatrix Trained models.
	 */
	public synchronized ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> trainTrend(ArrayList<Double> data,
			ArrayList<OffsetDateTime> date, HyperParameters hyperParameters) {
		var weightMatrix = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
		var weightTrend = new ArrayList<ArrayList<Double>>();
		PreprocessingPipeImpl preProcessing = new PreprocessingPipeImpl(hyperParameters);
		preProcessing.setData(to1DArray(data));
		preProcessing.setDates(date);

		var modifiedData = (double[][]) preProcessing//
				.interpolate()//
				.movingAverage()//
				.scale()//
				.filterOutliers()//
				.modifyForTrendPrediction()//
				.execute();

		for (int i = 0; i < modifiedData.length; i++) {
			weightTrend = (hyperParameters.getCount() == 0) //
					? generateInitialWeightMatrix(hyperParameters.getWindowSizeTrend(), hyperParameters)//
					: hyperParameters.getlastModelTrend().get(i);

			preProcessing.setData(modifiedData[i]);

			var preProcessed = (double[][][]) preProcessing//
					.groupToStiffedWindow()//
					.normalize()//
					.shuffle()//
					.execute();

			var model = new EngineBuilder() //
					.setInputMatrix(preProcessed[0])//
					.setTargetVector(preProcessed[1][0]) //
					.build();
			model.fit(hyperParameters.getGdIterration(), weightTrend, hyperParameters);
			weightMatrix.add(model.getWeights());

		}

		return weightMatrix;
	}

	/**
	 * Trains the seasonality model using the specified data, timestamps, and
	 * hyperparameters. The training process involves preprocessing the data,
	 * grouping it by hour and minute, and fitting the model for each group. The
	 * trained weights are saved to the model file.
	 *
	 * @param data            The ArrayList of Double values representing the
	 *                        time-series data.
	 * @param date            The ArrayList of OffsetDateTime objects corresponding
	 *                        to the timestamps of the data.
	 * @param hyperParameters The hyperparameters configuration for training the
	 *                        seasonality model.
	 * @return weightMatrix Trained seasonality models.
	 */

	public synchronized ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> trainSeasonality(ArrayList<Double> data,
			ArrayList<OffsetDateTime> date, HyperParameters hyperParameters) {
		var weightMatrix = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
		var weightSeasonality = new ArrayList<ArrayList<Double>>();
		int windowsSize = hyperParameters.getWindowSizeSeasonality();

		var preprocessing = new PreprocessingPipeImpl(hyperParameters);
		preprocessing.setData(to1DArray(data));//
		preprocessing.setDates(date);//

		var dataGroupedByMinute = (double[][][]) preprocessing//
				.interpolate()//
				.movingAverage()//
				.scale()//
				.filterOutliers()//
				.groupByHoursAndMinutes()//
				.execute();
		int k = 0;

		for (int i = 0; i < dataGroupedByMinute.length; i++) {
			for (int j = 0; j < dataGroupedByMinute[i].length; j++) {

				hyperParameters.setGdIterration(DynamicItterationValue
						.setIteration(hyperParameters.getAllModelErrorSeason(), k, hyperParameters));

				if (hyperParameters.getCount() == 0) {
					weightSeasonality = generateInitialWeightMatrix(windowsSize, hyperParameters);

				} else {
					weightSeasonality = hyperParameters.getlastModelSeasonality().get(k);
				}

				preprocessing.setData(dataGroupedByMinute[i][j]);

				var preProcessedSeason = (double[][][]) preprocessing//
						// .differencing()//
						.groupToWIndowSeasonality() //
						.normalize() //
						.shuffle() //
						.execute();

				var model = new EngineBuilder()//
						.setInputMatrix(preProcessedSeason[0]) //
						.setTargetVector(preProcessedSeason[1][0]) //
						.build();

				model.fit(hyperParameters.getGdIterration(), weightSeasonality, hyperParameters);
				weightMatrix.add(model.getWeights());

				k = k + 1;
			}
		}

		return weightMatrix;

	}

	/**
	 * Generates the initial weight matrix for the LSTM model based on the specified
	 * window size and hyperparameters.
	 *
	 * @param windowSize      The size of the window for the initial weight matrix.
	 * @param hyperParameters The hyperparameters used for generating the initial
	 *                        weight matrix.
	 * @return The initial weight matrix as an ArrayList of ArrayList of Double
	 *         values.
	 */
	public static ArrayList<ArrayList<Double>> generateInitialWeightMatrix(int windowSize,
			HyperParameters hyperParameters) {
		var initialWeight = new ArrayList<ArrayList<Double>>();
		var parameterTypes = new String[] { "wi", "wo", "wz", "ri", "ro", "rz", "yt", "ct" };

		for (var type : parameterTypes) {
			var temp = new ArrayList<Double>();
			for (int i = 1; i <= windowSize; i++) {
				var value = switch (type) {
				case "wi" -> hyperParameters.getWiInit();
				case "wo" -> hyperParameters.getWoInit();
				case "wz" -> hyperParameters.getWzInit();
				case "ri" -> hyperParameters.getRiInit();
				case "ro" -> hyperParameters.getRoInit();
				case "rz" -> hyperParameters.getRzInit();
				case "yt" -> hyperParameters.getYtInit();
				case "ct" -> hyperParameters.getCtInit();
				default -> throw new IllegalArgumentException("Invalid parameter type");
				};
				temp.add(value);
			}
			initialWeight.add(temp);
		}
		return initialWeight;
	}

}
