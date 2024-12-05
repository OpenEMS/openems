package io.openems.edge.predictor.lstm.validator;

import static io.openems.edge.predictor.lstm.preprocessing.DataModification.flattern4dto3d;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.openems.edge.predictor.lstm.common.DataStatistics;
import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.common.LstmPredictor;
import io.openems.edge.predictor.lstm.performance.PerformanceMatrix;
import io.openems.edge.predictor.lstm.preprocessing.DataModification;
import io.openems.edge.predictor.lstm.preprocessingpipeline.PreprocessingPipeImpl;
import io.openems.edge.predictor.lstm.utilities.UtilityConversion;

public class ValidationTrendModel {
	public static final String TREND = "trend";

	/**
	 * Validate Trend.
	 * 
	 * @param values               the value
	 * @param dates                the date
	 * @param untestedTrendWeights Untested Models.
	 * @param hyperParameters      the hyperParam
	 */
	public void validateTrend(ArrayList<Double> values, ArrayList<OffsetDateTime> dates,
			ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> untestedTrendWeights, HyperParameters hyperParameters) {
		var allModels = DataModification.reshape((flattern4dto3d(untestedTrendWeights)), hyperParameters);

		var rmsErrors = this.validateModels(//
				values, //
				dates, //
				allModels, //
				hyperParameters);

		var optInd = findOptimumIndex(rmsErrors, TREND, hyperParameters);

		this.updateModels(//
				allModels, //
				optInd, Integer.toString(hyperParameters.getCount()) + hyperParameters.getModelName() + TREND, //
				TREND, hyperParameters);
	}

	/**
	 * Find the indices of the maximum values in each column of a 2D matrix. This
	 * method takes a 2D matrix represented as a List of Lists and finds the row
	 * indices of the maximum values in each column. The result is returned as a
	 * List of Lists, where each inner list contains two integers: the row index and
	 * column index of the maximum value.
	 *
	 * @param matrix          A 2D matrix represented as a List of Lists of doubles.
	 * @param var             the var
	 * @param hyperParameters the hyperParam
	 * @return A List of Lists containing the row and column indices of the maximum
	 *         values in each column. If the input matrix is empty, an empty list is
	 *         returned.
	 */

	public static List<List<Integer>> findOptimumIndex(ArrayList<ArrayList<Double>> matrix, String var,
			HyperParameters hyperParameters) {
		var minimumIndices = new ArrayList<List<Integer>>();

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

		var err = new ArrayList<Double>();
		for (int i = 0; i < minimumIndices.size(); i++) {
			err.add(matrix.get(minimumIndices.get(i).get(0)).get(minimumIndices.get(i).get(1)));
		}
		hyperParameters.setAllModelErrorTrend(err);
		double errVal = DataStatistics.getStandardDeviation(err, hyperParameters.getTargetError());
		hyperParameters.setRmsErrorTrend(errVal);
		System.out.println("=====> Average RMS error for  " + var + " = " + errVal);
		return minimumIndices;
	}

	private ArrayList<ArrayList<Double>> validateModels(ArrayList<Double> value, ArrayList<OffsetDateTime> dates,
			ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allModels, HyperParameters hyperParameters) {
		var validateTrendPreProcess = new PreprocessingPipeImpl(hyperParameters);
		double[][] modifiedData = (double[][]) validateTrendPreProcess//
				.setData(UtilityConversion.to1DArray(value))//
				.setDates(dates)//
				.interpolate()//
				.movingAverage()//
				.scale()//
				.filterOutliers()//
				.modifyForTrendPrediction()//
				.execute();

		var rmsTemp2 = new ArrayList<ArrayList<Double>>();
		for (var modelsForData : allModels) {
			var rmsTemp1 = new ArrayList<Double>();

			for (int j = 0; j < modifiedData.length; j++) {
				double[][][] intermediate = (double[][][]) validateTrendPreProcess.setData(modifiedData[j])//
						// .differencing()//
						.groupToStiffedWindow()//
						.execute();

				double[][][] preprocessed = (double[][][]) validateTrendPreProcess.setData(intermediate)//
						.normalize()//
						.shuffle()//
						.execute();

				double[] result = (double[]) validateTrendPreProcess//
						.setData(UtilityConversion.to1DArray(
								LstmPredictor.predictPre(preprocessed[0], modelsForData.get(j), hyperParameters)))//
						.setMean(DataStatistics.getMean(intermediate[0]))//
						.setStandardDeviation(DataStatistics.getStandardDeviation(intermediate[0]))//
						.reverseNormalize()//
						.reverseScale()//
						.execute();

				double rms = PerformanceMatrix.rmsError(
						(double[]) validateTrendPreProcess.setData(intermediate[1][0]).reverseScale().execute(), result)
						* (1 - PerformanceMatrix.accuracy(
								(double[]) validateTrendPreProcess.setData(intermediate[1][0]).reverseScale().execute(),
								result, 0.01));
				rmsTemp1.add(rms);
			}
			rmsTemp2.add(rmsTemp1);
		}
		return rmsTemp2;
	}

	private void updateModels(ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allModels, List<List<Integer>> optInd,
			String modelFileName, String modelType, HyperParameters hyperParameters) {
		DataModification.updateModel(allModels, optInd, modelFileName, modelType, hyperParameters);
	}

}
