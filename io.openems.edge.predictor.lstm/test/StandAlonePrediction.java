import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import org.junit.Test;

import io.openems.edge.predictor.lstm.common.DataModification;
import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.common.ReadCsv;
import io.openems.edge.predictor.lstm.common.ReadModels;
import io.openems.edge.predictor.lstm.interpolation.InterpolationManager;
import io.openems.edge.predictor.lstm.performance.PerformanceMatrix;
import io.openems.edge.predictor.lstm.predictor.Prediction;
import io.openems.edge.predictor.lstm.predictor.Predictor;

public class StandAlonePrediction {
	/**
	 * This class contains a method for prediction testing.
	 */

	public void predictionTest() {
		ArrayList<Double> predicted = new ArrayList<Double>();
		ArrayList<Double> predictionFromSeasonality = new ArrayList<Double>();
		ArrayList<Double> allTarget = new ArrayList<Double>();
		ArrayList<Double> forTrendPrediction = new ArrayList<Double>();
		ArrayList<OffsetDateTime> dateForTrend = new ArrayList<OffsetDateTime>();
		ArrayList<Double> target = new ArrayList<Double>();
		ArrayList<String> fileName = new ArrayList<String>();
		ArrayList<Double> rmsTrend = new ArrayList<Double>();
		ArrayList<Double> rmsSeasonality = new ArrayList<Double>();
		ArrayList<Double> accuracyTrend = new ArrayList<Double>();
		ArrayList<Double> accuracySeasonality = new ArrayList<Double>();
		HyperParameters hyperParameters = new HyperParameters();
		for (int m = 8; m <= 8; m++) {
			ArrayList<Double> predictionFromTrend = new ArrayList<Double>();
			String pathSeasonality = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\SavedModel.txt";
			
			String csvFileNAme = Integer.toString(m) + ".csv";
			fileName.add(csvFileNAme);
			for (int i = 0; i < 288; i++) {
				int windowSize = hyperParameters.getWindowSizeSeasonality();
				ZonedDateTime nowDate = ZonedDateTime.of(2023, 8, 8, 0, 0, 0, 0, ZonedDateTime.now().getZone());
				nowDate = nowDate.plusMinutes(i * hyperParameters.getInterval());
				ZonedDateTime until = ZonedDateTime.of(nowDate.getYear(), nowDate.getMonthValue(),
						nowDate.getDayOfMonth(), nowDate.getHour(), getMinute(nowDate, hyperParameters), 0, 0,
						nowDate.getZone());
				ZonedDateTime temp = until.minusDays(windowSize - 1);
				ZonedDateTime fromDate = ZonedDateTime.of(temp.getYear(), temp.getMonthValue(), temp.getDayOfMonth(),
						nowDate.getHour(), getMinute(nowDate, hyperParameters), 0, 0, temp.getZone());
				ZonedDateTime targetFrom = until.plusMinutes(hyperParameters.getInterval());
				ZonedDateTime targetTo = targetFrom.plusHours(24);
				final ArrayList<Double> data = this.quarryData(fromDate, until, csvFileNAme);
				target = this.quarryData(targetFrom, targetTo, csvFileNAme);
				allTarget.addAll(target);
				forTrendPrediction = this.quarryData(
						until.minusMinutes(hyperParameters.getInterval() * hyperParameters.getWindowSizeTrend()), until,
						csvFileNAme);
				dateForTrend = this.quarryDate(
						until.minusMinutes(hyperParameters.getInterval() * hyperParameters.getWindowSizeTrend()), until,
						csvFileNAme);
				ArrayList<OffsetDateTime> date = this.quarryDate(fromDate, until, csvFileNAme);
				if (i == 0) {
					Prediction var = new Prediction(data, date, pathSeasonality, hyperParameters);
					predictionFromSeasonality = getArranged(
							getIndex(targetFrom.getHour(), targetFrom.getMinute(), hyperParameters),
							var.getPredictedValues());
				}
				Prediction prediction = new Prediction(data, date, pathSeasonality, hyperParameters);
				predicted = getArranged(getIndex(targetFrom.getHour(), targetFrom.getMinute(), hyperParameters),
						prediction.getPredictedValues());
				final ArrayList<Double> trendPrediction = this.predictTrend(forTrendPrediction, dateForTrend, until,
						hyperParameters);
				predictionFromTrend.add(trendPrediction.get(0));
				for (int l = 0; l < trendPrediction.size(); l++) {
					predicted.set(l, trendPrediction.get(l));
				}
			}
			System.out.println(csvFileNAme);
			System.out.println("Target =" + target);
			System.out.println("Predictedtrend =" + predictionFromTrend);
			System.out.println("Predictedseasonality =" + predictionFromSeasonality); 
			System.out.println(predictionFromSeasonality.size());
			System.out.println(target.size());
			rmsSeasonality.add(PerformanceMatrix.rmsError(target, predictionFromSeasonality));
			accuracySeasonality.add(PerformanceMatrix.accuracy(target, predictionFromTrend, 0.2));
			System.out.println("File Name= " + fileName);
			System.out.println("Rms Trend = " + rmsTrend);
			System.out.println("Rms seasonality = " + rmsSeasonality);
			System.out.println("Accuracy Trend = " + accuracyTrend);
			System.out.println("Rms seasonality = " + accuracySeasonality);
		}
		System.out.println("File Name= " + fileName);
		System.out.println("Rms Trend = " + rmsTrend);
		System.out.println("Rms seasonality = " + rmsSeasonality);
		System.out.println("Accuracy Trend = " + accuracyTrend);
		System.out.println("Rms seasonality = " + accuracySeasonality);
	}

	/**
	 * Gets the rounded minute value for the provided ZonedDateTime.
	 *
	 * @param nowDate         The ZonedDateTime for which to determine the rounded
	 *                        minute
	 * @param hyperParameters is the object of class HyperParameters value.
	 * @return The rounded minute value (0, 15, 30, or 45) based on the minute
	 *         component of the input time.
	 */

	public static Integer getMinute(ZonedDateTime nowDate, HyperParameters hyperParameters) {

		int totalGroups = 60 / hyperParameters.getInterval();
		int startVal = 0;
		int endVal = 0;
		for (int i = 0; i < totalGroups; i++) {
			endVal = startVal + hyperParameters.getInterval();
			boolean check = startVal <= nowDate.getMinute() && nowDate.getMinute() < endVal;
			if (check == false) {

				startVal = endVal;

			} else {

				break;
			}
		}
		return startVal;
	}

	/**
	 * Queries data from a CSV file for a specified time range and returns the
	 * relevant data points.
	 *
	 * @param fromDate  The start date and time for data retrieval.
	 * @param untilDate The end date and time for data retrieval.
	 * @param path      The file path to the CSV data file.
	 * @return An ArrayList of data points that fall within the specified time
	 *         range.
	 */

	public ArrayList<Double> quarryData(ZonedDateTime fromDate, ZonedDateTime untilDate, String path) {
		String dataPath = path;
		ReadCsv csv = new ReadCsv(dataPath);
		ArrayList<Double> data = csv.getData();
		ArrayList<OffsetDateTime> dates = csv.getDates();
		ArrayList<Double> toReturn = new ArrayList<Double>();
		int from = this.getindexOfDate(this.convertZoned2Ooned(fromDate), dates);
		int till = this.getindexOfDate(this.convertZoned2Ooned(untilDate), dates);
		toReturn = this.getData(from, till, data);
		return toReturn;
	}

	/**
	 * Queries and retrieves a list of OffsetDateTime values from a CSV file that
	 * fall within a specified time range.
	 *
	 * @param fromDate  The start date and time for data retrieval.
	 * @param untilDate The end date and time for data retrieval.
	 * @param path      The file path to the CSV data file.
	 * @return An ArrayList of OffsetDateTime values that correspond to the
	 *         specified time range.
	 */

	public ArrayList<OffsetDateTime> quarryDate(ZonedDateTime fromDate, ZonedDateTime untilDate, String path) {
		String dataPath = path;
		ReadCsv csv = new ReadCsv(dataPath);
		ArrayList<OffsetDateTime> dates = csv.getDates();
		ArrayList<OffsetDateTime> toReturn = new ArrayList<OffsetDateTime>();
		int from = this.getindexOfDate(this.convertZoned2Ooned(fromDate), dates);
		int till = this.getindexOfDate(this.convertZoned2Ooned(untilDate), dates);
		toReturn = this.getDate(from, till, dates);
		return toReturn;
	}

	/**
	 * Converts an OffsetDateTime to a ZonedDateTime, retaining the date and time
	 * components.
	 *
	 * @param time The OffsetDateTime to convert to ZonedDateTime.
	 * @return The converted ZonedDateTime with the same date and time components.
	 */

	public ZonedDateTime convertOff2Zoned(OffsetDateTime time) {
		ZonedDateTime toReturn = ZonedDateTime.of(time.getYear(), time.getMonthValue(), time.getDayOfMonth(),
				time.getHour(), time.getMinute(), 0, 0, ZonedDateTime.now().getZone());
		return toReturn;
	}

	/**
	 * Converts a ZonedDateTime to an OffsetDateTime, retaining the date, time, and
	 * offset components.
	 *
	 * @param time The ZonedDateTime to convert to OffsetDateTime.
	 * @return The converted OffsetDateTime with the same date, time, and offset
	 *         components.
	 */

	public OffsetDateTime convertZoned2Ooned(ZonedDateTime time) {
		OffsetDateTime toReturn = OffsetDateTime.of(time.toLocalDateTime(), time.getOffset());
		return toReturn;

	}

	/**
	 * Find the index of a specific OffsetDateTime within an ArrayList of
	 * OffsetDateTime values.
	 *
	 * @param date  The OffsetDateTime to search for within the ArrayList.
	 * @param dates An ArrayList of OffsetDateTime values to search in.
	 * @return The index of the specified date in the ArrayList if found; otherwise,
	 *         null.
	 */

	public Integer getindexOfDate(OffsetDateTime date, ArrayList<OffsetDateTime> dates) {
		for (int i = 0; i < dates.size(); i++) {
			if (dates.get(i).isEqual(date)) {
				return i;
			}
		}
		System.out.println(date + " is not in the list");
		return null;
	}

	/**
	 * Retrieves a subset of data from an ArrayList of Double values based on
	 * specified indices.
	 *
	 * @param fromIndex The starting index (inclusive) for data retrieval.
	 * @param toIndex   The ending index (exclusive) for data retrieval.
	 * @param data      An ArrayList of Double values containing the data.
	 * @return A new ArrayList containing the subset of data from the specified
	 *         range of indices.
	 */

	public ArrayList<Double> getData(Integer fromIndex, Integer toIndex, ArrayList<Double> data) {
		ArrayList<Double> newList = new ArrayList<Double>();
		for (int i = 0; i < data.size(); i++) {
			if (i >= fromIndex && i < toIndex) {
				newList.add(data.get(i));
			}
		}
		return newList;
	}

	/**
	 * Retrieves a subset of OffsetDateTime values from an ArrayList based on
	 * specified indices.
	 *
	 * @param fromIndex The starting index (inclusive) for date retrieval.
	 * @param toIndex   The ending index (exclusive) for date retrieval.
	 * @param date      An ArrayList of OffsetDateTime values containing the dates.
	 * @return A new ArrayList containing the subset of OffsetDateTime values from
	 *         the specified range of indices.
	 */

	public ArrayList<OffsetDateTime> getDate(Integer fromIndex, Integer toIndex, ArrayList<OffsetDateTime> date) {
		ArrayList<OffsetDateTime> newList = new ArrayList<OffsetDateTime>();
		for (int i = 0; i < date.size(); i++) {
			if (i >= fromIndex && i < toIndex) {
				newList.add(date.get(i));
			}
		}

		return newList;
	}

	/**
	 * Re-arranges an ArrayList of Double values by splitting it at the specified
	 * index and moving the second part to the front.
	 *
	 * @param splitIndex  The index at which the ArrayList will be split.
	 * @param singleArray An ArrayList of Double values to be re-arranged.
	 * @return A new ArrayList containing the Double values after re-arrangement.
	 */

	public static ArrayList<Double> getArranged(int splitIndex, ArrayList<Double> singleArray) {
		ArrayList<Double> arranged = new ArrayList<Double>();
		ArrayList<Double> firstGroup = new ArrayList<Double>();
		ArrayList<Double> secondGroup = new ArrayList<Double>();

		for (int i = 0; i < singleArray.size(); i++) {
			if (i < splitIndex) {
				firstGroup.add(singleArray.get(i));
			} else {
				secondGroup.add(singleArray.get(i));
			}
		}

		arranged.addAll(secondGroup);
		arranged.addAll(firstGroup);

		return arranged;
	}

	/**
	 * Calculates the index of a specific hour and minute combination within a
	 * 24-hour period, divided into 15-minute intervals.
	 *
	 * @param hour            The hour component (0-23) to be used for the
	 *                        calculation.
	 * @param minute          The minute component (0, 5, 10, ..., 55) to be used
	 *                        for the
	 * @param hyperParameters is the object of class HyperParameters, calculation.
	 * @return The index representing the specified hour and minute combination.
	 */

	public static Integer getIndex(Integer hour, Integer minute, HyperParameters hyperParameters) {

		int k = 0;
		for (int i = 0; i < 24; i++) {
			for (int j = 0; j < (int) 60 / hyperParameters.getInterval(); j++) {
				int h = i;
				int m = j * hyperParameters.getInterval();
				if (hour == h && minute == m) {
					// System.out.println(k);

					return k;
				} else {
					k = k + 1;
				}
			}
		}

		return k;
	}

	@Test
	public void test() {
		StandAlonePrediction obj = new StandAlonePrediction();
		obj.predictionTest();

	}

	/**
	 * Predict a trend value for a given dataset using a trained model. This method
	 * predicts a trend value for a given dataset using a trained model. The method
	 * reads the model data, performs data interpolation, scales the data, and uses
	 * the model to make the prediction. It then scales back the predicted value to
	 * the original range.
	 *
	 * @param data            An ArrayList of Double representing the dataset for
	 *                        prediction.
	 * @param until           A ZonedDateTime representing the time until which the
	 *                        prediction is made.
	 * @param date            reference date for time series data
	 * @param hyperParameters is the object of class HyperParameter
	 * @return A double representing the predicted trend value.
	 */

	public ArrayList<Double> predictTrend(ArrayList<Double> data, ArrayList<OffsetDateTime> date, ZonedDateTime until,
			HyperParameters hyperParameters) {
		// read Model
		String pathTrend = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\trend.txt";
		double pred = 0;
		ArrayList<Double> trendPrediction = new ArrayList<Double>();
		ZonedDateTime predictionFor = until.plusMinutes(hyperParameters.getInterval());
		ArrayList<ArrayList<ArrayList<Double>>> val = ReadModels.getModelForSeasonality(pathTrend, hyperParameters)
				.get(0);
		InterpolationManager interpolationManager = new InterpolationManager(data, date, hyperParameters);
		data = interpolationManager.getInterpolatedData();
		ArrayList<Double> scaledData = DataModification.scale(data, hyperParameters.getScalingMin(),
				hyperParameters.getScalingMax());
		for (int i = 0; i < hyperParameters.getTrendPoint(); i++) {
			predictionFor = predictionFor.plusMinutes(i * hyperParameters.getInterval());
			int modlelindex = (int) this.decodeDateToColumnIndex(predictionFor, hyperParameters);
			System.out.println(modlelindex);
			double predTemp = Predictor.predict(scaledData, val.get(modlelindex).get(0), val.get(modlelindex).get(1),
					val.get(modlelindex).get(2), val.get(modlelindex).get(3), val.get(modlelindex).get(4),
					val.get(modlelindex).get(5), val.get(modlelindex).get(7), val.get(modlelindex).get(6));
			scaledData.add(predTemp);
			scaledData.remove(0);
			pred = DataModification.scaleBack(predTemp, hyperParameters.getScalingMin(),
					hyperParameters.getScalingMax());
			trendPrediction.add(pred);
		}
		return trendPrediction;
	}

	/**
	 * Decodes a given ZonedDateTime into a column index based on the hour and
	 * minute. The method calculates the column index of the model to be used by
	 * converting the given hour and minute into a standardized time representation
	 * where each 15-minute interval is assigned a unique index.
	 *
	 * @param predictionFor   The ZonedDateTime for which the column index is to be
	 * @param hyperParameters is the object of class HyperParameters. decoded.
	 * @return The column index corresponding to the specified ZonedDateTime.
	 * 
	 */

	public double decodeDateToColumnIndex(ZonedDateTime predictionFor, HyperParameters hyperParameters) {

		int hour = predictionFor.getHour();
		int minute = predictionFor.getMinute();
		int index = (Integer) hour * (60 / hyperParameters.getInterval()) + minute / hyperParameters.getInterval();
		int modifiedIndex = index - hyperParameters.getWindowSizeTrend();
		if (modifiedIndex >= 0) {
			return modifiedIndex;
		} else {
			return modifiedIndex + 60 / hyperParameters.getInterval() * 24;
		}
	}

}
