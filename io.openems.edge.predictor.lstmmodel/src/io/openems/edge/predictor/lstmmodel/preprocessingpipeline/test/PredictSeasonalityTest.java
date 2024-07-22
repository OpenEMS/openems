package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;

import io.openems.edge.predictor.lstmmodel.LstmPredictor;
import io.openems.edge.predictor.lstmmodel.common.DataStatistics;
import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.common.ReadAndSaveModels;

public class PredictSeasonalityTest {

	public static ArrayList<Double> predictSeasonalityTest(//
			ArrayList<Double> data, //
			ArrayList<OffsetDateTime> date, //
			HyperParameters hyperParameters //
	) {

		/*
		 * ArrayList<Double> -> double[] intepolation double[] -> double[] movingaverage
		 * double[] -> double[] scaling double[] -> double[] filteroutlier double[] ->
		 * double[][][] groupby hours n minutes dpouble[][][] -> double [][] tranfrm (to
		 * 2d)
		 */

		// caluclate the mean double [][] -> double[]

		/**
		 * mean on 2d array stddevaition on 2d array double [][] -> double[]
		 */

		/**
		 * normalization on 2d array double [][] -? double [][]
		 */

		// now predict with double [][] -? double []

		/**
		 * use predict for revernormalize
		 * 
		 * double [] -> double [] revernormalize(mean , std) but use the mean and std
		 * deviation form the previously calculated double [] -> double[] reverscale
		 */

		final var resized = Pipeline.of(data, date, hyperParameters) //
				.interpolate() //
				// .movingAverage
				.scale() //
				.filterOutliers() //
				.groupByHoursAndMinutes()//
				.to2DList();

		var mean = DataStatistics.getMean(resized.get());
		var sd = DataStatistics.getStandardDeviation(resized.get());

		var normal = Pipeline.of(resized.get(), hyperParameters).normalize().to2DList();

		var allModel = hyperParameters.getBestModelSeasonality();

		final var predicted = LstmPredictor.predictPre(normal, allModel, hyperParameters);

		return Pipeline.of(predicted, date, hyperParameters) //
				.reverseNormalize(mean, sd) //
				.reverseScale() //
				.to1DList();
	}

	public static void main(String[] args) {

		PredictSeasonalityTest obj = new PredictSeasonalityTest();
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

		var res = obj.predictSeasonalityTest(data, date, hp);
		System.out.println(res);

	}

}
