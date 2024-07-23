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

	/**
	 * Pipeline Seasonality Test.
	 * 
	 * @param data            List of double values
	 * @param date            List of OffsetDateTime values
	 * @param hyperParameters {@link HyperParameters}
	 * @return List of the predicted data
	 */
	public static ArrayList<Double> predictSeasonalityTest(//
			ArrayList<Double> data, //
			ArrayList<OffsetDateTime> date, //
			HyperParameters hyperParameters //
	) {

		final var resized = Pipeline.of(data, date, hyperParameters) //

				// ArrayList<Double> -> double[] intepolation
				.interpolate() //

				// double[] -> double[] movingaverage
				.movingAverage()//

				// double[] -> double[] scaling
				.scale() //

				// double[] -> double[] filteroutlier
				.filterOutliers() //

				// double[] -> double[][][] groupby hours n minutes
				.groupByHoursAndMinutes()//

				// (to2d)
				.to2DList();

		// calculate the mean double [][] -> double[]
		var mean = DataStatistics.getMean(resized.get());

		// calculate the Standard deviation double [][] -> double[]
		var sd = DataStatistics.getStandardDeviation(resized.get());

		var normal = Pipeline.of(resized.get(), hyperParameters)//

				// double [][] -> double [][]
				.normalize()//

				// (to2d)
				.to2DList();

		var allModel = hyperParameters.getBestModelSeasonality();

		final var predicted = LstmPredictor.predictPre(normal, allModel, hyperParameters);

		return Pipeline.of(predicted, date, hyperParameters) //

				// double [] -> double [] revernormalize(mean , std)
				.reverseNormalize(mean, sd) //

				// double [] -> double[] reverscale
				.reverseScale() //

				// (to1d)
				.to1DList();
	}

	/**
	 * main method for testing.
	 * 
	 * @param args the args
	 */
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

		var res = PredictSeasonalityTest.predictSeasonalityTest(data, date, hp);
		System.out.println(res);

	}

}
