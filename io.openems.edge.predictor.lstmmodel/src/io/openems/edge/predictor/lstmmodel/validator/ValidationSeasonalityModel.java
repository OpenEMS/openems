package io.openems.edge.predictor.lstmmodel.validator;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.openems.edge.predictor.lstmmodel.LstmPredictor;
import io.openems.edge.predictor.lstmmodel.common.DataStatistics;
import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import static io.openems.edge.predictor.lstmmodel.performance.PerformanceMatrix.rmsError;
import static io.openems.edge.predictor.lstmmodel.performance.PerformanceMatrix.accuracy;
import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.PreprocessingPipeImpl;
import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;

public class ValidationSeasonalityModel {

	public static final String SEASONALITY = "seasonality";

	/**
	 * Validate the Seasonality.
	 * 
	 * @param values                    the values
	 * @param dates                     the dates
	 * @param untestedSeasonalityWeight Models to validate.
	 * @param hyperParameters           the hyperParameters
	 */

	public void validateSeasonality(ArrayList<Double> values, ArrayList<OffsetDateTime> dates,
			ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> untestedSeasonalityWeight,
			HyperParameters hyperParameters) {

		ArrayList<ArrayList<Double>> rmsTemp2 = new ArrayList<ArrayList<Double>>();

		PreprocessingPipeImpl preProcessing = new PreprocessingPipeImpl(hyperParameters);
		double[][][] dataGroupedByMinute = (double[][][]) preProcessing.setData(UtilityConversion.to1DArray(values))//
				.setDates(dates)//
				.interpolate()//
				.movingAverage()//
				.scale()//
				.filterOutliers()//
				.groupByHoursAndMinutes()//
				.execute();

		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allModels = DataModification
				.reshape((DataModification.flattern4dto3d(untestedSeasonalityWeight)), hyperParameters);

		for (int h = 0; h < allModels.size(); h++) {
			ArrayList<Double> rmsTemp1 = new ArrayList<Double>();
			int k = 0;
			for (int i = 0; i < dataGroupedByMinute.length; i++) {
				for (int j = 0; j < dataGroupedByMinute[i].length; j++) {

					double[][][] intermediate = (double[][][]) preProcessing.setData(dataGroupedByMinute[i][j])//
							// .differencing()//
							.groupToWIndowSeasonality()//
							.execute();

					double[][][] preProcessed = (double[][][]) preProcessing.setData(intermediate)//
							.normalize()//
							.execute();

					ArrayList<ArrayList<Double>> val = allModels.get(h).get(k);

					double[] result = (double[]) preProcessing//
							.setData(UtilityConversion
									.to1DArray(LstmPredictor.predictPre(preProcessed[0], val, hyperParameters)))//
							.setMean(DataStatistics.getMean(intermediate[0]))//
							.setStandardDeviation(DataStatistics.getStandardDeviation(intermediate[0]))//
							.reverseNormalize()//
							.reverseScale()//
							.execute();

					double rms = rmsError(//
							(double[]) preProcessing//
									.setData(intermediate[1][0])//
									.reverseScale()//
									.execute(),
							result) //
							* //
							(1 - accuracy((double[]) preProcessing.setData(intermediate[1][0])//
									.reverseScale()//
									.execute(), result, 0.01));

					rmsTemp1.add(rms);
					k = k + 1;

				}

			}
			rmsTemp2.add(rmsTemp1);
		}
		List<List<Integer>> optInd = findOptimumIndex(rmsTemp2, SEASONALITY, hyperParameters);

		DataModification.updateModel(allModels, optInd,
				Integer.toString(hyperParameters.getCount()) + hyperParameters.getModelName() + SEASONALITY,
				SEASONALITY, hyperParameters);
	}

	/**
	 * Find the indices of the minimum values in each column of a 2D matrix. This
	 * method takes a 2D matrix represented as a List of Lists and finds the row
	 * indices of the minimum values in each column. The result is returned as a
	 * List of Lists, where each inner list contains two integers: the row index and
	 * column index of the minimum value.
	 *
	 * @param matrix          A 2D matrix represented as a List of Lists of doubles.
	 * @param variable        the variable
	 * @param hyperParameters the hyperParameters
	 * @return A List of Lists containing the row and column indices of the minimum
	 *         values in each column. If the input matrix is empty, an empty list is
	 *         returned.
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
