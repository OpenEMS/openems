package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.openems.edge.predictor.lstmmodel.LstmPredictor;
import io.openems.edge.predictor.lstmmodel.common.DataStatistics;
import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.performance.PerformanceMatrix;
import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;

public class ValidateTrendTest {
	public static final String TREND = "trend";

	/**
	 * Pipeline Validate Trend Test.
	 * 
	 * @param values               List of double values
	 * @param dates                List of OffsetDateTime values
	 * @param untestedTrendWeights List of un-tested weights
	 * @param hyperParameters      {@link HyperParameters}
	 */
	public void validateTrendTest(ArrayList<Double> values, ArrayList<OffsetDateTime> dates,
			ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> untestedTrendWeights, HyperParameters hyperParameters) {

		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allModels = DataModification
				.reshape((DataModification.flattern4dto3d(untestedTrendWeights)), hyperParameters);

		ArrayList<ArrayList<Double>> rmsErrors = this.validateModels(//
				values, //
				dates, //
				allModels, //
				hyperParameters);

		List<List<Integer>> optInd = findOptimumIndex(rmsErrors, TREND, hyperParameters);

		this.updateModels(allModels, optInd,
				Integer.toString(hyperParameters.getCount()) + hyperParameters.getModelName() + TREND, TREND,
				hyperParameters);

	}

	private ArrayList<ArrayList<Double>> validateModels(ArrayList<Double> value, ArrayList<OffsetDateTime> dates,
			ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allModels, HyperParameters hyperParameters) {

		double[][] modifiedData = Pipeline.of(value, dates, hyperParameters)// ArrayList<Double> -> double[]

				// intepolation double[] -> double[]
				.interpolate()//

				// movingaverage double[] -> double[]

				.movingAverage()//
				// scaling double[] -> double[]

				.scale()//
				// scaling double[] -> double[]

				.filterOutliers()//
				// double[] -> double[][]

				.modifyForTrendPrediction()//
				.get();

		ArrayList<ArrayList<Double>> rmsTemp2 = new ArrayList<>();

		for (ArrayList<ArrayList<ArrayList<Double>>> modelsForData : allModels) {
			ArrayList<Double> rmsTemp1 = new ArrayList<>();

			for (int j = 0; j < modifiedData.length; j++) {

				double[][][] intermediate = Pipeline.of(modifiedData[j], hyperParameters)//

						// double [] -> double[][][] group to stiffed window
						.groupedToStiffedWindow()//
						.get();

				// caluclate the mean double [][] -> double[]
				var mean = DataStatistics.getMean(intermediate[0]);

				// mean on 2d array stddevaition on 2d array double [][] -> double[]
				var sd = DataStatistics.getStandardDeviation(intermediate[0]);

				double[][][] preprocessed = Pipeline.of(intermediate, hyperParameters)//

						// double [][][] -> double [][][] normalize
						.normalize()//
						.get();

				var temppredicted = LstmPredictor.predictPre(preprocessed[0], modelsForData.get(j), hyperParameters);

				var result = Pipeline.of(temppredicted, hyperParameters)//
						.reverseNormalize(mean, sd)//
						.reverseScale()//
						.get();

				var target = Pipeline.of(intermediate[1][0], hyperParameters)//
						.reverseScale()//
						.get();
				double rms = PerformanceMatrix.rmsError(result, target);

				rmsTemp1.add(rms);
			}
			rmsTemp2.add(rmsTemp1);
		}
		return rmsTemp2;
	}

	/**
	 * find the Optimum index.
	 * 
	 * @param matrix          the matrix
	 * @param var             the variable
	 * @param hyperParameters the {@link HyperParameters}
	 * @return matrix
	 */
	public static List<List<Integer>> findOptimumIndex(ArrayList<ArrayList<Double>> matrix, String var,
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
		hyperParameters.setAllModelErrorTrend(err);
		double errVal = DataStatistics.getStandardDeviation(err, hyperParameters.getTargetError());
		hyperParameters.setRmsErrorTrend(errVal);
		System.out.println("=====> Average RMS error for  " + var + " = " + errVal);
		return minimumIndices;
	}

	private void updateModels(ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allModels, List<List<Integer>> optInd,
			String modelFileName, String modelType, HyperParameters hyperParameters) {
		DataModification.updateModel(allModels, optInd, modelFileName, modelType, hyperParameters);
	}

}
