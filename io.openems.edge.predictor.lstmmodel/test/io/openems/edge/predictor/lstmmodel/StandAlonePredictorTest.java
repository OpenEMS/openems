package io.openems.edge.predictor.lstmmodel;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

//import org.junit.Test;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.common.ReadAndSaveModels;
import io.openems.edge.predictor.lstmmodel.common.ReadCsv;
import io.openems.edge.predictor.lstmmodel.interpolation.InterpolationManager;
import io.openems.edge.predictor.lstmmodel.performance.PerformanceMatrix;
import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;
import static io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion.to1DArrayList;

/**
 * This test class is intended for local testing and is not executed during the
 * build process. To run the JUnit test cases, please uncomment the relevant
 * annotations. Ensure that the necessary data and model files are accessible in
 * the specified path before executing the tests.
 */
public class StandAlonePredictorTest {

	public static final String CSV = "1.csv";
	public static final ZonedDateTime GLOBAL_DATE = ZonedDateTime.of(2022, 6, 16, 0, 0, 0, 0,
			ZonedDateTime.now().getZone());
	public static final HyperParameters HYPER_PARAMETERS = ReadAndSaveModels.read("ConsumptionActivePower");

	/**
	 * Prediction testing.
	 */
	// @Test
	public void predictionTest() {

		ArrayList<ArrayList<Double>> predictedSeasonality = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> predictedTrend = new ArrayList<Double>();
		HYPER_PARAMETERS.printHyperParameters();

		int predictionCount = 1;
		for (int i = 0; i < predictionCount; i++) {
			ZonedDateTime nowDate = GLOBAL_DATE.plusHours(24 * i);
			var tempPredicted = this.predictSeasonality(HYPER_PARAMETERS, nowDate, CSV);
			predictedSeasonality.add(tempPredicted);
		}

		for (int i = 0; i < predictionCount; i++) {
			ZonedDateTime nowDate = GLOBAL_DATE.plusHours(24 * i);
			predictedTrend = this.predictTrendOneDay(HYPER_PARAMETERS, nowDate, CSV);
		}

		var pre = to1DArrayList(predictedSeasonality);

		var until = GLOBAL_DATE.withMinute(getMinute(GLOBAL_DATE, HYPER_PARAMETERS)).withSecond(0).withNano(0);
		var targetFrom = until.plusMinutes(HYPER_PARAMETERS.getInterval());
		var targetTo = targetFrom.plusHours(24 * predictionCount);

		var target = this.getTargetData(targetFrom, targetTo, CSV, HYPER_PARAMETERS);
		var rmsSeasonality = PerformanceMatrix.rmsError(target, pre);
		var rmsTrend = PerformanceMatrix.rmsError(target, predictedTrend);

		StringBuilder sb = new StringBuilder();
		String format = "%-25s %s%n";

		sb.append(String.format(format, "Target:", DataModification.constantScaling(target, 1)))
				.append(String.format(format, "PredictedSeasonality:", DataModification.constantScaling(pre, 1)))
				.append(String.format(format, "Target (raw):", target))
				.append(String.format(format, "Predicted trend:", DataModification.constantScaling(predictedTrend, 1)))
				.append(String.format(format, "RMS Trend:", rmsTrend))
				.append(String.format(format, "RMS Seasonality:", rmsSeasonality))
				.append(String.format(format, "Accuracy Trend:",
						PerformanceMatrix.accuracy(target, predictedTrend, 0.15)))
				.append(String.format(format, "Accuracy Seasonality:", PerformanceMatrix.accuracy(target, pre, 0.15)));

		System.out.println(sb.toString());
	}

	// @Test
	protected void predictionTestMultivarient() {
		ArrayList<ArrayList<Double>> predictedSeasonality = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> predictedTrend = new ArrayList<Double>();
		HYPER_PARAMETERS.printHyperParameters();

		int predictionFor = 1;
		for (int i = 0; i < predictionFor; i++) {
			ZonedDateTime nowDate = GLOBAL_DATE.plusHours(24 * i);
			var tempPredicted = this.predictSeasonalityMultivarent(HYPER_PARAMETERS, nowDate, CSV);
			predictedSeasonality.add(tempPredicted);
		}

		for (int i = 0; i < predictionFor; i++) {
			ZonedDateTime nowDate = GLOBAL_DATE.plusHours(24 * i);
			predictedTrend = this.predictTrendOneDayMultivarent(HYPER_PARAMETERS, nowDate, CSV);

		}
		var pre = to1DArrayList(predictedSeasonality);
		var until = GLOBAL_DATE.withMinute(getMinute(GLOBAL_DATE, HYPER_PARAMETERS)).withSecond(0).withNano(0);
		var targetFrom = until.plusMinutes(HYPER_PARAMETERS.getInterval());
		var targetTo = targetFrom.plusHours(24 * predictionFor);

		// changing target data for reference
		var target = this.getTargetData(targetFrom, targetTo, CSV, HYPER_PARAMETERS);
		var ref = this.getTargetRefrence(targetFrom, targetTo);

		var trend = DataModification.elementWiseDiv(predictedTrend, ref);
		var rmsSeasonality = PerformanceMatrix.rmsError(target, pre);
		var rmsTrend = PerformanceMatrix.rmsError(target, trend);

		var sb = new StringBuilder();
		String format = "%-25s %s%n";

		sb.append(String.format(format, "Target:", DataModification.constantScaling(target, 1)))
				.append(String.format(format, "PredictedSeasonality:", DataModification.constantScaling(pre, 1)))
				.append(String.format(format, "Target (raw):", target))
				.append(String.format(format, "Predicted trend:", DataModification.constantScaling(trend, 1)))
				.append(String.format(format, "RMS Trend:", rmsTrend))
				.append(String.format(format, "RMS Seasonality:", rmsSeasonality))
				.append(String.format(format, "Accuracy trend:", PerformanceMatrix.accuracy(target, trend, 0.15)))
				.append(String.format(format, "Accuracy seasonality:", PerformanceMatrix.accuracy(target, pre, 0.15)));

		System.out.println(sb.toString());
	}

	/**
	 * Doing what it suppose to do.
	 * 
	 * @param hyperParameters the Hyperparam
	 * @param nowDate         nowDate
	 * @param csvFileName     csvFileName
	 * @return predicted the predicted
	 */
	public ArrayList<Double> predictSeasonality(HyperParameters hyperParameters, ZonedDateTime nowDate,
			String csvFileName) {

		var until = GLOBAL_DATE.withMinute(getMinute(GLOBAL_DATE, HYPER_PARAMETERS)).withSecond(0).withNano(0);
		var windowSize = hyperParameters.getWindowSizeSeasonality();

		nowDate = nowDate.plusMinutes(hyperParameters.getInterval());

		var temp = until.minusDays(windowSize);
		var fromDate = temp.withMinute(getMinute(nowDate, hyperParameters)).withSecond(0).withNano(0);

		final var data = this.queryData(fromDate, until, csvFileName);
		final var date = this.queryDate(fromDate, until, csvFileName);

		var targetFrom = until.plusMinutes(hyperParameters.getInterval());

		ArrayList<Double> predicted = LstmPredictor.getArranged(
				LstmPredictor.getIndex(targetFrom.getHour(), targetFrom.getMinute(), hyperParameters),
				LstmPredictor.predictSeasonality(data, date, hyperParameters));
		return predicted;
	}

	/**
	 * Doing what it suppose to do.
	 * 
	 * @param hyperParameters the Hyperparam
	 * @param nowDate         nowDate
	 * @param csvFileName     csvFileName
	 * @return predicted the predicted
	 */
	public ArrayList<Double> predictSeasonalityMultivarent(HyperParameters hyperParameters, ZonedDateTime nowDate,
			String csvFileName) {

		var until = nowDate.withMinute(getMinute(nowDate, hyperParameters)).withSecond(0).withNano(0);

		var windowSize = hyperParameters.getWindowSizeSeasonality();

		nowDate = nowDate.plusMinutes(hyperParameters.getInterval());

		var temp = until.minusDays(windowSize);
		var fromDate = temp.withMinute(getMinute(nowDate, hyperParameters)).withSecond(0).withNano(0);

		final var data = this.queryData(fromDate, until, csvFileName);
		final var date = this.queryDate(fromDate, until, csvFileName);

		var refdata = this.generateRefrence(date);
		var toPredictData = DataModification.elementWiseMultiplication(refdata, data);

		var targetFrom = until.plusMinutes(hyperParameters.getInterval());

		var predicted = LstmPredictor.getArranged(
				LstmPredictor.getIndex(targetFrom.getHour(), targetFrom.getMinute(), hyperParameters),
				LstmPredictor.predictSeasonality(toPredictData, date, hyperParameters));

		// postprocess
		var targetRef = this.getTargetRefrence(fromDate, until);
		return DataModification.elementWiseDiv(predicted, targetRef);
	}

	/**
	 * Gives prediction for seasoality.
	 * 
	 * @param hyperParameters the Hyperparam
	 * @param nowDate         nowDate
	 * @param csvFileName     csvFileName
	 * @return predicted the predicted
	 */
	public ArrayList<Double> predictTrendOneDay(HyperParameters hyperParameters, ZonedDateTime nowDate,
			String csvFileName) {
		ArrayList<Double> predicted = new ArrayList<Double>();
		for (int i = 0; i < 60 / hyperParameters.getInterval() * 24; i++) {
			var nowDateTemp = nowDate.plusMinutes(i * hyperParameters.getInterval());
			var until = nowDateTemp.withMinute(getMinute(nowDateTemp, hyperParameters)).withSecond(0).withNano(0);
			var forTrendPrediction = this.queryData(
					until.minusMinutes(hyperParameters.getInterval() * hyperParameters.getWindowSizeTrend()), until,
					csvFileName);
			var dateForTrend = this.queryDate(
					until.minusMinutes(hyperParameters.getInterval() * hyperParameters.getWindowSizeTrend()), until,
					csvFileName);
			predicted.add(LstmPredictor.predictTrend(forTrendPrediction, dateForTrend, until, hyperParameters).get(0));
		}
		return predicted;
	}

	/**
	 * Predicts the Trend.
	 * 
	 * @param hyperParameters the {@link HyperParameters}
	 * @param nowDate         the {@link ZonedDateTime} for now
	 * @param csvFileName     the csv file name
	 * @return the trend
	 */
	public ArrayList<Double> predictTrendOneDayMultivarent(HyperParameters hyperParameters, ZonedDateTime nowDate,
			String csvFileName) {

		ArrayList<Double> predicted = new ArrayList<Double>();
		for (int i = 0; i < 60 / hyperParameters.getInterval() * 24; i++) {

			var nowDateTemp = nowDate.plusMinutes(i * hyperParameters.getInterval());
			var until = nowDateTemp.withMinute(getMinute(nowDateTemp, hyperParameters)).withSecond(0).withNano(0);
			var forTrendPrediction = this.queryData(
					until.minusMinutes(hyperParameters.getInterval() * hyperParameters.getWindowSizeTrend()), until,
					csvFileName);
			var dateForTrend = this.queryDate(
					until.minusMinutes(hyperParameters.getInterval() * hyperParameters.getWindowSizeTrend()), until,
					csvFileName);

			// modification for multivariant
			var tempData = this.generateRefrence(dateForTrend);

			tempData = DataModification.elementWiseMultiplication(forTrendPrediction, tempData);
			predicted.add(LstmPredictor.predictTrend(tempData, dateForTrend, until, hyperParameters).get(0));
		}
		return predicted;
	}

	/**
	 * Gives target data to compare.
	 * 
	 * @param from           the From
	 * @param to             the to
	 * @param csvfileName    the csvfileName
	 * @param hyperParameter the hyperParameter
	 * @return the target data
	 */
	public ArrayList<Double> getTargetData(ZonedDateTime from, ZonedDateTime to, String csvfileName,
			HyperParameters hyperParameter) {
		InterpolationManager obj = new InterpolationManager(this.queryData(from, to, csvfileName), hyperParameter);
		return obj.getInterpolatedData();

	}

	/**
	 * Generates a Reference from {@link OffsetDateTime}s.
	 * 
	 * @param dates the {@link OffsetDateTime}s
	 * @return the reference
	 */
	public ArrayList<Double> generateRefrence(ArrayList<OffsetDateTime> dates) {
		// one hour = 360/24 degree
		// one minute = 360/(24*60) degree
		Objects.requireNonNull(dates, "Date list must not be null");

		return dates.stream().map(date -> {
			double hourAngle = date.getHour() * 15.0; // 360/24 = 15 degrees per hour
			double minuteAngle = date.getMinute() * 0.25; // 360/(24*60) = 0.25 degrees per minute
			double totalAngle = hourAngle + minuteAngle;

			return 1.5 + Math.cos(Math.toRadians(totalAngle));
		}).collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Generates a Reference from {@link ZonedDateTime}s.
	 * 
	 * @param dates the {@link ZonedDateTime}s
	 * @return the reference
	 */
	public static ArrayList<Double> generateReference(ArrayList<ZonedDateTime> dates) {

		Objects.requireNonNull(dates, "Date list must not be null");

		return dates.stream().map(date -> {
			double hourAngle = date.getHour() * 15.0; // 360/24 = 15 degrees per hour
			double minuteAngle = date.getMinute() * 0.25; // 360/(24*60) = 0.25 degrees per minute
			double totalAngle = hourAngle + minuteAngle;

			return 1.5 + Math.cos(Math.toRadians(totalAngle));
		}).collect(Collectors.toCollection(ArrayList::new));

	}

	ArrayList<Double> getTargetRefrence(ZonedDateTime from, ZonedDateTime to) {

		int interval = 5;
		int hour = 24;
		int dataLen = (60 / interval) * hour;

		List<ZonedDateTime> dates = Stream.iterate(from, date -> date.plusMinutes(interval))//
				.limit(dataLen)//
				.collect(Collectors.toList());

		return generateReference(new ArrayList<>(dates));
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
	public static int getMinute(ZonedDateTime nowDate, final HyperParameters hyperParameters) {
		Objects.requireNonNull(nowDate, "DateTime must not be null");
		Objects.requireNonNull(hyperParameters, "HyperParameters must not be null");

		final int interval = hyperParameters.getInterval();

		if (interval <= 0) {
			throw new IllegalArgumentException("Interval must be positive");
		}
		if (60 % interval != 0) {
			throw new IllegalArgumentException(String.format("Interval %d must be a factor of 60", interval));
		}

		return (nowDate.getMinute() / interval) * interval;
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
	public ArrayList<Double> queryData(ZonedDateTime fromDate, ZonedDateTime untilDate, String path) {
		String dataPath = path;
		ReadCsv csv = new ReadCsv(dataPath);
		ArrayList<Double> data = csv.getData();
		ArrayList<OffsetDateTime> dates = csv.getDates();
		ArrayList<Double> toReturn = new ArrayList<Double>();
		int from = this.getindexOfDate(this.toOffsetDateTime(fromDate), dates);
		int till = this.getindexOfDate(this.toOffsetDateTime(untilDate), dates);
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

	public ArrayList<OffsetDateTime> queryDate(ZonedDateTime fromDate, ZonedDateTime untilDate, String path) {
		String dataPath = path;
		ReadCsv csv = new ReadCsv(dataPath);
		ArrayList<OffsetDateTime> dates = csv.getDates();
		ArrayList<OffsetDateTime> toReturn = new ArrayList<OffsetDateTime>();
		int from = this.getindexOfDate(this.toOffsetDateTime(fromDate), dates);
		int till = this.getindexOfDate(this.toOffsetDateTime(untilDate), dates);
		toReturn = this.getDate(from, till, dates);
		return toReturn;
	}

	/**
	 * Converts an OffsetDateTime to a ZonedDateTime, retaining the date and time
	 * components.
	 *
	 * @param offsetDateTime The OffsetDateTime to convert to ZonedDateTime.
	 * @return The converted ZonedDateTime with the same date and time components.
	 */

	public ZonedDateTime toZonedDateTime(OffsetDateTime offsetDateTime) {

		Objects.requireNonNull(offsetDateTime, "OffsetDateTime must not be null");

		return offsetDateTime.atZoneSameInstant(ZoneId.systemDefault()).withSecond(0).withNano(0);
	}

	/**
	 * Converts a ZonedDateTime to an OffsetDateTime, retaining the date, time, and
	 * offset components.
	 *
	 * @param time The ZonedDateTime to convert to OffsetDateTime.
	 * @return The converted OffsetDateTime with the same date, time, and offset
	 *         components.
	 */

	public OffsetDateTime toOffsetDateTime(ZonedDateTime time) {
		Objects.requireNonNull(time, "ZonedDateTime must not be null");
		return time.toOffsetDateTime().withSecond(0).withNano(0);
	}

	/**
	 * Find the index of a specific OffsetDateTime within an ArrayList of
	 * OffsetDateTime values.
	 *
	 * @param targetDate  The OffsetDateTime to search for within the ArrayList.
	 * @param dates An ArrayList of OffsetDateTime values to search in.
	 * @return The index of the specified date in the ArrayList if found; otherwise,
	 *         null.
	 */

	public Integer getindexOfDate(OffsetDateTime targetDate, ArrayList<OffsetDateTime> dates) {
		Objects.requireNonNull(targetDate, "Target date must not be null");
		Objects.requireNonNull(dates, "Date list must not be null");
		return IntStream.range(0, dates.size()).boxed().filter(i -> targetDate.isEqual(dates.get(i))).findFirst().get();
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
		if (fromIndex < 0 || toIndex > data.size()) {
			throw new IllegalArgumentException("Indices out of bounds. Valid range is 0 to " + data.size());
		}
		if (fromIndex > toIndex) {
			throw new IllegalArgumentException("fromIndex must be less than or equal to toIndex");
		}

		return data.stream().skip(fromIndex).limit(toIndex - fromIndex)
				.collect(Collectors.toCollection(ArrayList::new));
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
		if (fromIndex < 0 || toIndex > date.size()) {
			throw new IllegalArgumentException("Indices out of bounds. Valid range is 0 to " + date.size());
		}
		if (fromIndex > toIndex) {
			throw new IllegalArgumentException("fromIndex must be less than or equal to toIndex");
		}

		return date.stream().skip(fromIndex).limit(toIndex - fromIndex)
				.collect(Collectors.toCollection(ArrayList::new));
	}
}
