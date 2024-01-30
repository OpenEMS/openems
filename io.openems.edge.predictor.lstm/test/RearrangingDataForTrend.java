import static org.junit.Assert.fail;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;

import io.openems.edge.predictor.lstm.common.DataModification;
import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.common.ReadCsv;
import io.openems.edge.predictor.lstm.common.ReadModels;
import io.openems.edge.predictor.lstm.common.SaveModel;
import io.openems.edge.predictor.lstm.interpolation.InterpolationManager;
import io.openems.edge.predictor.lstm.preprocessing.GroupBy;
import io.openems.edge.predictor.lstm.preprocessing.PreProcessingImpl;
import io.openems.edge.predictor.lstm.preprocessing.Suffle;
import io.openems.edge.predictor.lstm.util.Engine;
import io.openems.edge.predictor.lstm.util.Engine.EngineBuilder;

public class RearrangingDataForTrend {
	/**
	 * Rearranges and processes time-series data for trend analysis using a
	 * specified set of hyperparameters and a pre-trained model path.
	 *
	 * @param hyperParameters The hyperparameters configuration for the trend
	 *                        analysis.
	 * @param modlePath       The path to the pre-trained model for seasonality
	 *                        analysis.
	 * @throws Exception Throws an exception if there is an error during the
	 *                   process.
	 */
	public void main(HyperParameters hyperParameters, String modlePath) throws Exception {
		String csvFileName = "1.csv";
		ArrayList<ArrayList<ArrayList<Double>>> dataGroupedByMinute = new ArrayList<ArrayList<ArrayList<Double>>>();
		ArrayList<ArrayList<ArrayList<OffsetDateTime>>> dateGroupedByMinute = new ArrayList<ArrayList<ArrayList<OffsetDateTime>>>();
		ArrayList<ArrayList<Double>> reShapeFirst = new ArrayList<ArrayList<Double>>();
		ReadCsv csv = new ReadCsv(csvFileName);
		ArrayList<Double> data = csv.getData();
		ArrayList<OffsetDateTime> date = csv.getDates();
		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> weightMatrix = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
		InterpolationManager inter = new InterpolationManager(data, date, hyperParameters);
		ArrayList<ArrayList<Double>> weight1 = new ArrayList<ArrayList<Double>>();
		ArrayList<ArrayList<Double>> finalGroupedMatrix = new ArrayList<ArrayList<Double>>();
		// Grouping the data by hour and minute
		GroupBy groupAsHour = new GroupBy(inter.getInterpolatedData(), date);
		groupAsHour.hour();

		for (int i = 0; i < groupAsHour.getDataGroupedByHour().size(); i++) {

			GroupBy groupAsMinute = new GroupBy(groupAsHour.getDataGroupedByHour().get(i),
					groupAsHour.getDateGroupedByHour().get(i));
			groupAsMinute.minute();
			dataGroupedByMinute.add(groupAsMinute.getDataGroupedByMinute());
			dateGroupedByMinute.add(groupAsMinute.getDateGroupedByMinute());
		}

		// rehsaping the grouped data step1 : Reshaping the dimension of the grouped
		// matrix :
		for (int i = 0; i < dataGroupedByMinute.size(); i++) {
			for (int j = 0; j < dataGroupedByMinute.get(i).size(); j++) {

				reShapeFirst.add(dataGroupedByMinute.get(i).get(j));
			}

		}
		// reGrouping data from reshaped matrix

		int offset = 0;

		for (int i = 0; i < reShapeFirst.size(); i++) {
			ArrayList<ArrayList<Double>> toCombine = new ArrayList<ArrayList<Double>>();

			for (int j = 0; j <= hyperParameters.getWindowSizeTrend(); j++) {
				if (j + offset < reShapeFirst.size()) {
					toCombine.add(reShapeFirst.get(j + offset));

				} else {

					toCombine.add(reShapeFirst.get(j + offset - reShapeFirst.size()));

				}

			}

			finalGroupedMatrix.add(this.combinedArray(toCombine));
			offset++;

		}

		// final grouping the incorporate the prediction for time points included in th
		// efirst window

		// System.out.println(finalGroupedMatrix.get(0).size());

  //		hyperParameters.setModleSuffix("trend.txt");

		for (int i = 0; i < finalGroupedMatrix.size(); i++) {
			// System.out.println(finalGroupedMatrix.get(i));

			if (hyperParameters.getCount() == 0) {
				weight1 = this.generateInitialWeightMatrix(hyperParameters.getWindowSizeTrend(), hyperParameters);
			} else {
				String path = modlePath + Integer.toString(hyperParameters.getCount() - 1) + "trend.txt";
				ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allModel = ReadModels.getModelForSeasonality(path,
						hyperParameters);

				weight1 = allModel.get(allModel.size() - 1).get(i);
			}
			double[][] trainData = PreProcessingImpl.groupToStiffedWindow(DataModification
					.scale(finalGroupedMatrix.get(i), hyperParameters.getScalingMin(), hyperParameters.getScalingMax()),
					hyperParameters.getWindowSizeTrend());

			// System.out.println("Total Size of the matrix " +
			// finalGroupedMatrix.get(i).size());
			// System.out.println("Train data len " + trainData.length);
			double[] trainTarget = PreProcessingImpl.groupToStiffedTarget(DataModification
					.scale(finalGroupedMatrix.get(i), hyperParameters.getScalingMin(), hyperParameters.getScalingMax()),
					hyperParameters.getWindowSizeTrend());

			// System.out.println("Train Target len " + trainTarget.length);
			Suffle obj1 = new Suffle(trainData, trainTarget);

			Engine model = new EngineBuilder() //
					.setInputMatrix(DataModification.normalizeData(obj1.getData())) // removing normalization
					.setTargetVector(obj1.getTarget()) //
					.build();
			// System.out.println("Passed normalized data"
			// +
			// UtilityConversion.convert2DArrayTo2DArrayList(DataModification.normalizeData(trainData)).get(56));
			model.fit(hyperParameters.getGdIterration(), weight1, hyperParameters);
			// weight1 = model.getWeights().get(model.getWeights().size() - 1);

			weightMatrix.add(model.getWeights());
			// System.out.println(model.getWeights());

		}

		SaveModel.saveModels(weightMatrix, Integer.toString(hyperParameters.getCount()) + "trend.txt");

		System.out.println("Modle saved as : " + "trend.txt");

	}

	/**
	 * Combines values from a list of ArrayLists into a single ArrayList, grouping
	 * values by their respective positions.
	 *
	 * @param val The list of ArrayLists containing values to be combined.
	 * @return An ArrayList of Double representing the combined values.
	 */

	public ArrayList<Double> combinedArray(ArrayList<ArrayList<Double>> val) {
		ArrayList<Integer> sizeMatrix = new ArrayList<Integer>();
		ArrayList<Double> reGroupedsecond = new ArrayList<Double>();
		for (int i = 0; i < val.size(); i++) {
			sizeMatrix.add(val.get(i).size());

		}
		for (int i = 0; i < Collections.min(sizeMatrix); i++) {
			for (int j = 0; j < val.size(); j++) {

				reGroupedsecond.add(val.get(j).get(i));

			}

		}
		return reGroupedsecond;

	}

	/**
	 * Generates an initial weight matrix based on the specified window size and
	 * hyperparameters.
	 *
	 * @param windowSize      The size of the window for which the weights are
	 *                        generated.
	 * @param hyperParameters The hyperparameters used to initialize the weights.
	 * @return An ArrayList of ArrayLists representing the initial weight matrix.
	 */

	public ArrayList<ArrayList<Double>> generateInitialWeightMatrix(int windowSize, HyperParameters hyperParameters) {
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
			double wi = hyperParameters.getWiInit();
			double wo = hyperParameters.getWoInit();
			double wz = hyperParameters.getWzInit();
			final double ri = hyperParameters.getRiInit();
			final double ro = hyperParameters.getRoInit();
			final double rz = hyperParameters.getRzInit();
			final double ct = hyperParameters.getCtInit();
			final double yt = hyperParameters.getYtInit();

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

	/**
	 * Populates and returns a list of target dates based on the provided date
	 * range, window size, and hyperparameters.
	 *
	 * @param date            The list of all dates.
	 * @param startVal        The starting index of the date range.
	 * @param endVal          The ending index (exclusive) of the date range.
	 * @param hyperParameters The hyperparameters used to determine the target
	 *                        dates.
	 * @return An ArrayList of OffsetDateTime representing the target dates.
	 */

	public ArrayList<OffsetDateTime> populateTarget(ArrayList<OffsetDateTime> date, int startVal, int endVal,
			HyperParameters hyperParameters) {
		var dateSpecefic = date.subList(startVal, endVal);
		var targetDates = dateSpecefic.subList(startVal + hyperParameters.getWindowSizeTrend(), dateSpecefic.size());
		return new ArrayList<>(targetDates);

	}

	@Test
	public void test() throws Exception {
		HyperParameters hyperparameters =  HyperParameters.getInstance();
		RearrangingDataForTrend obj = new RearrangingDataForTrend();
		String path = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\";
		obj.main(hyperparameters, path);

		fail("Not yet implemented");
	}

}
