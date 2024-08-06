package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.openems.edge.predictor.lstmmodel.LstmPredictor;
import io.openems.edge.predictor.lstmmodel.common.DataStatistics;
import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.performance.PerformanceMatrix;
import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;

public class ValidateSeasonailtyTest {

	public static final String SEASONALITY = "seasonality";

	/**
	 * Pipeline Validate Seasonality Test.
	 * 
	 * @param values                    List of double values
	 * @param dates                     List of OffsetDateTime values
	 * @param untestedSeasonalityWeight List of un-tested weights
	 * @param hyperParameters           {@link HyperParameters}
	 */
	public static void validateSeasonality(ArrayList<Double> values, ArrayList<OffsetDateTime> dates,
			ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> untestedSeasonalityWeight,
			HyperParameters hyperParameters) {

		ArrayList<ArrayList<Double>> rmsTemp2 = new ArrayList<ArrayList<Double>>();

		var dataGroupedByMinute = Pipeline.of(values, dates, hyperParameters)// ArrayList<Double> -> double[]

				// intepolation double[] -> double[]
				.interpolate()//

				// movingaverage double[] -> double[]
				.movingAverage()//

				// scaling double[] -> double[]
				.scale()//

				// double[] -> double[]
				.filterOutliers()//

				// groupby hours n minutes doouble[] -> double [][][]
				.groupByHoursAndMinutes().get();

		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allModels = DataModification
				.reshape((DataModification.flattern4dto3d(untestedSeasonalityWeight)), hyperParameters);

		for (int h = 0; h < allModels.size(); h++) {
			ArrayList<Double> rmsTemp1 = new ArrayList<Double>();
			int k = 0;
			for (int i = 0; i < dataGroupedByMinute.length; i++) {
				for (int j = 0; j < dataGroupedByMinute[i].length; j++) {

					double[][][] intermediate = Pipeline.of(dataGroupedByMinute[i][j], hyperParameters)
							// .differencing
							.groupToWIndowSeasonality().get();

					var mean = DataStatistics.getMean(intermediate[0]);
					var sd = DataStatistics.getStandardDeviation(intermediate[0]);

					double[][][] preProcessed = Pipeline.of(intermediate, hyperParameters)//
							.normalize()//
							.get();

					var val = allModels.get(h).get(k);

					var tempPredict = LstmPredictor.predictPre(preProcessed[0], val, hyperParameters);

					var predicted = Pipeline.of(tempPredict, hyperParameters)//
							.reverseNormalize(mean, sd)//
							.reverseScale()//
							.get();

					var target = Pipeline.of(intermediate[1][0], hyperParameters)//
							.reverseScale()//
							.get();

					var rms = PerformanceMatrix.rmsError(target, predicted);
					rmsTemp1.add(rms);

					k = k + 1;
				}

			}
			rmsTemp2.add(rmsTemp1);
		}
		var optInd = findOptimumIndex(rmsTemp2, SEASONALITY, hyperParameters);

		DataModification.updateModel(//
				allModels, //
				optInd, Integer.toString(hyperParameters.getCount()) + hyperParameters.getModelName() + SEASONALITY,
				SEASONALITY, //
				hyperParameters);

	}

	/**
	 * find the Optimum index.
	 * 
	 * @param matrix          the matrix
	 * @param variable        the variable
	 * @param hyperParameters the {@link HyperParameters}
	 * @return matrix
	 */
	public static List<List<Integer>> findOptimumIndex(ArrayList<ArrayList<Double>> matrix, String variable,
			HyperParameters hyperParameters) {
		List<List<Integer>> minimumIndices = new ArrayList<>();

		if (matrix.isEmpty() || matrix.get(0).isEmpty()) {
			return minimumIndices; // Empty matrix, return empty list
		}

		int numColumns = matrix.get(0).size();

		for (int col = 0; col < numColumns; col++) {
			double min = matrix.get(0).get(col);
			List<Integer> minIndices = new ArrayList<>(Arrays.asList(0, col));

			for (int row = 0; row < matrix.size(); row++) {
				double value = matrix.get(row).get(col);

				if (value < min) {
					min = value;
					minIndices.set(0, row);
				}
			}

			minimumIndices.add(minIndices);
		}

		ArrayList<Double> err = new ArrayList<Double>();
		for (int i = 0; i < minimumIndices.size(); i++) {

			err.add(matrix.get(minimumIndices.get(i).get(0)).get(minimumIndices.get(i).get(1)));
		}
		hyperParameters.setAllModelErrorSeason(err);
		// TODO Error calculation could be division of sections, or average currently
		// its standard deviation
		double errVal = DataStatistics.getStandardDeviation(err, hyperParameters.getTargetError());
		hyperParameters.setRmsErrorSeasonality(errVal);
		System.out.println("=====> Average RMS error for  " + variable + " = " + errVal);
		return minimumIndices;
	}

	@FunctionalInterface
	interface FilePathGenerator {
		String generatePath(File file, String fileName);
	}

}
