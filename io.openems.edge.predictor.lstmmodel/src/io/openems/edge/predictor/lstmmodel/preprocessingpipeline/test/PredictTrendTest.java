package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.openems.edge.predictor.lstmmodel.LstmPredictor;
import io.openems.edge.predictor.lstmmodel.common.DataStatistics;
import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.common.ReadAndSaveModels;

public class PredictTrendTest {

	public static ArrayList<Double> predictTrendtest(ArrayList<Double> data, ArrayList<OffsetDateTime> date,
			ZonedDateTime until, HyperParameters hyperParameters) {

		/*
		 * ArrayList<Double> -> double[] intepolation double[] -> double[] remove
		 * negatives double[] -> double[] scaling
		 */

		// caluclate the mean double [] -> double

		/**
		 * mean on 2d array stddevaition on 1d array double [] -> double
		 */

		/**
		 * normalization on 2d array double [] -? double []
		 */

		// now predict with double [] -> double []

		/**
		 * use predict for revernormalize
		 * 
		 * double [] -> double [] revernormalize(mean , std) but use the mean and std
		 * deviation form the previously calculated double [] -> double[] reverscale
		 */

		var scaled = Pipeline.of(data, date, hyperParameters)//
				.interpolate()//
				.removeNegatives()//
				.scale()//
				.to1DList();

		var mean = DataStatistics.getMean(scaled);
		var sd = DataStatistics.getStandardDeviation(scaled);

		var trendPrediction = new double[hyperParameters.getTrendPoint()];

		var normal = Pipeline.of(scaled, date, hyperParameters)//
				.normalize()//
				.to1DList();

		var predictionFor = until.plusMinutes(hyperParameters.getInterval());
		var val = hyperParameters.getBestModelTrend();

		for (int index = 0; index < hyperParameters.getTrendPoint(); index++) {

			var temp = predictionFor.plusMinutes(index * hyperParameters.getInterval());

			var modlelindex = (int) decodeDateToColumnIndex(temp, hyperParameters);

			double predTemp = LstmPredictor.predict(normal, //
					val.get(modlelindex).get(0), val.get(modlelindex).get(1), //
					val.get(modlelindex).get(2), val.get(modlelindex).get(3), //
					val.get(modlelindex).get(4), val.get(modlelindex).get(5), //
					val.get(modlelindex).get(7), val.get(modlelindex).get(6), //
					hyperParameters);

			normal.add(predTemp);
			normal.remove(0);

			trendPrediction[index] = (predTemp);
		}

		return Pipeline.of(normal, date, hyperParameters)//
				.reverseNormalize(mean, sd)//
				.reverseScale()//
				.to1DList();

	}

	public ArrayList<Double> predictTrend(ArrayList<Double> data, ArrayList<OffsetDateTime> date, ZonedDateTime until,
			HyperParameters hyperParameters) {

		// Pipeline for data preprocessing
		var scaled = Pipeline.of(data, date, hyperParameters).interpolate().scale();

		var trendPoints = hyperParameters.getTrendPoint();
		var trendPrediction = new double[trendPoints];

		var mean = DataStatistics.getMean(scaled.get());
		var standardDeviation = DataStatistics.getStandardDeviation(scaled.get());

		var normData = Pipeline.of(scaled.to1DList(), date, hyperParameters).normalize().to1DList();

		var predictionFor = until.plusMinutes(hyperParameters.getInterval());
		var models = hyperParameters.getBestModelTrend();

		// Create a thread pool for parallel processing
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors
				.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		for (int i = 0; i < trendPoints; i++) {
			final int index = i;
			executor.submit(() -> {
				var temp = predictionFor.plusMinutes(index * hyperParameters.getInterval());
				var modelIndex = (int) decodeDateToColumnIndex(temp, hyperParameters);

				double predTemp = LstmPredictor.predict(normData, models.get(modelIndex).get(0),
						models.get(modelIndex).get(1), models.get(modelIndex).get(2), models.get(modelIndex).get(3),
						models.get(modelIndex).get(4), models.get(modelIndex).get(5), models.get(modelIndex).get(7),
						models.get(modelIndex).get(6), hyperParameters);

				synchronized (normData) {
					normData.add(predTemp);
					normData.remove(0);
				}

				trendPrediction[index] = predTemp;
			});
		}

		// Shutdown the executor and wait for all tasks to complete
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		var result = Pipeline.of(normData, date, hyperParameters).reverseNormalize(mean, standardDeviation)
				.reverseScale().to1DList();

		return result;
	}

	public static double decodeDateToColumnIndex(ZonedDateTime predictionFor, HyperParameters hyperParameters) {

		var hour = predictionFor.getHour();

		var minute = predictionFor.getMinute();
		var index = (Integer) hour * (60 / hyperParameters.getInterval()) + minute / hyperParameters.getInterval();
		var modifiedIndex = index - hyperParameters.getWindowSizeTrend();
		if (modifiedIndex >= 0) {
			return modifiedIndex;
		} else {
			return modifiedIndex + 60 / hyperParameters.getInterval() * 24;
		}
	}

	public static void main(String[] args) {

		String modelName = "ConsumptionActivePower";

		HyperParameters hp = ReadAndSaveModels.read(modelName);

		ArrayList<Double> data = new ArrayList<>(
				Arrays.asList(10.5, 12.3, 11.7, 9.8, 13.6, 15.2, 14.3, 10.1, 11.5, 12.8));
		ArrayList<OffsetDateTime> date = new ArrayList<>(
				Arrays.asList(OffsetDateTime.of(2024, 7, 1, 10, 0, 0, 0, ZoneOffset.UTC),
						OffsetDateTime.of(2024, 7, 2, 11, 0, 0, 0, ZoneOffset.UTC),
						OffsetDateTime.of(2024, 7, 3, 12, 0, 0, 0, ZoneOffset.UTC),
						OffsetDateTime.of(2024, 7, 4, 13, 0, 0, 0, ZoneOffset.UTC),
						OffsetDateTime.of(2024, 7, 5, 14, 0, 0, 0, ZoneOffset.UTC),
						OffsetDateTime.of(2024, 7, 6, 15, 0, 0, 0, ZoneOffset.UTC),
						OffsetDateTime.of(2024, 7, 7, 16, 0, 0, 0, ZoneOffset.UTC),
						OffsetDateTime.of(2024, 7, 8, 17, 0, 0, 0, ZoneOffset.UTC),
						OffsetDateTime.of(2024, 7, 9, 18, 0, 0, 0, ZoneOffset.UTC),
						OffsetDateTime.of(2024, 7, 10, 19, 0, 0, 0, ZoneOffset.UTC)));

		var res = PredictTrendTest.predictTrendtest(data, date,
				ZonedDateTime.of(2024, 7, 9, 18, 0, 0, 0, ZoneOffset.UTC), hp);
		System.out.println(res);

	}

}
