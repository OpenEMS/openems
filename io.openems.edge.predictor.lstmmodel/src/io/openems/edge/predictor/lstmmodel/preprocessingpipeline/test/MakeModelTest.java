package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.edge.predictor.lstmmodel.common.DynamicItterationValue;
import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.util.Engine.EngineBuilder;

public class MakeModelTest {

	public static final String SEASONALITY = "seasonality.txt";
	public static final String TREND = "trend.txt";

	public synchronized ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> trainTrend(ArrayList<Double> data,
			ArrayList<OffsetDateTime> date, HyperParameters hyperParameters) {

		var weightMatrix = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
		var weightTrend = new ArrayList<ArrayList<Double>>();

		var modifiedData = Pipeline.of(data, date, hyperParameters)//

				.interpolate()//
				.movingAverage()//
				.movingAverage()//
				.filterOutliers()//
				.modifyForTrendPrediction()//
				.get();

		for (int i = 0; i < modifiedData.length; i++) {

			weightTrend = (hyperParameters.getCount() == 0) //
					? generateInitialWeightMatrix(hyperParameters.getWindowSizeTrend(), hyperParameters)//
					: hyperParameters.getlastModelTrend().get(i);

			double[][][] preProcessed = Pipeline.of(modifiedData[i], hyperParameters)//
					.groupedToStiffedWindow()//
					.shuffle().get();

			var model = new EngineBuilder() //
					.setInputMatrix(preProcessed[0])//
					.setTargetVector(preProcessed[1][0]) //
					.build();
			model.fit(hyperParameters.getGdIterration(), weightTrend, hyperParameters);
			weightMatrix.add(model.getWeights());

		}

		return weightMatrix;
	}

	public synchronized ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> trainSeasonality(ArrayList<Double> data,
			ArrayList<OffsetDateTime> date, HyperParameters hyperParameters) {

		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> weightMatrix = new ArrayList<>();
		ArrayList<ArrayList<Double>> weightSeasonality = new ArrayList<>();
		int windowsSize = hyperParameters.getWindowSizeSeasonality();

		var dataGroupedByMinute = Pipeline.of(data, date, hyperParameters)//
				.interpolate()//
				.movingAverage()//
				.movingAverage()//
				.filterOutliers()//
				.groupByHoursAndMinutes()//
				.get();

		int k = 0;

		for (int i = 0; i < dataGroupedByMinute.length; i++) {
			for (int j = 0; j < dataGroupedByMinute[i].length; j++) {

				hyperParameters.setGdIterration(
						DynamicItterationValue.setIteration(hyperParameters.getAllModelErrorSeason(), k, hyperParameters));

				if (hyperParameters.getCount() == 0) {
					weightSeasonality = generateInitialWeightMatrix(windowsSize, hyperParameters);
				} else {
					weightSeasonality = hyperParameters.getlastModelSeasonality().get(k);
				}

				var preProcessedSeason = Pipeline.of(dataGroupedByMinute[i][j], hyperParameters)//
						.groupToWIndowSeasonality()//
						.normalize()//
						.shuffle()//
						.get();

				var model = new EngineBuilder()//
						.setInputMatrix(preProcessedSeason[0])//
						.setTargetVector(preProcessedSeason[1][0])//
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

		ArrayList<ArrayList<Double>> initialWeight = new ArrayList<>();
		String[] parameterTypes = { "wi", "wo", "wz", "ri", "ro", "rz", "yt", "ct" };

		for (String type : parameterTypes) {
			ArrayList<Double> temp = new ArrayList<>();
			for (int i = 1; i <= windowSize; i++) {
				double value = switch (type) {
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
