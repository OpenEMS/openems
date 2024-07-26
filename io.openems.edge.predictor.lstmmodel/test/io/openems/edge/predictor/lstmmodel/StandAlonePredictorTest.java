package io.openems.edge.predictor.lstmmodel;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import org.junit.Test;

//import org.junit.Test;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.common.ReadAndSaveModels;
import io.openems.edge.predictor.lstmmodel.common.ReadCsv;
import io.openems.edge.predictor.lstmmodel.interpolation.InterpolationManager;
import io.openems.edge.predictor.lstmmodel.performance.PerformanceMatrix;
import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.PredictSeasonalityTest;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.PredictTrendTest;
import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;

public class StandAlonePredictorTest {

	/**
	 * Prediction testing.
	 */
	// @Test
	public void predictionTest() {
		ArrayList<ArrayList<Double>> predictedSeasonality = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> predictedTrend = new ArrayList<Double>();

		String csv = "1" + ".csv";

		ZonedDateTime globDate = ZonedDateTime.of(2022, 6, 16, 0, 0, 0, 0, ZonedDateTime.now().getZone());

		HyperParameters hyperParameters = ReadAndSaveModels.read("ConsumptionActivePower");

		hyperParameters.printHyperParameters();

		int predictionFor = 1;
		for (int i = 0; i < predictionFor; i++) {

			ZonedDateTime nowDate = globDate;
			nowDate = nowDate.plusHours(24 * i);

			var tempPredicted = this.predictSeasonality(hyperParameters, nowDate, csv);
			predictedSeasonality.add(tempPredicted);

		}

		for (int i = 0; i < predictionFor; i++) {
			ZonedDateTime nowDate = globDate;
			nowDate = nowDate.plusHours(24 * i);
			predictedTrend = this.predictTrendOneDay(hyperParameters, nowDate, csv);

		}
		ArrayList<Double> pre = UtilityConversion.to1DArrayList(predictedSeasonality);
		ZonedDateTime nowDate = globDate;
		ZonedDateTime until = ZonedDateTime.of(nowDate.getYear(), nowDate.getMonthValue(), nowDate.getDayOfMonth(),
				nowDate.getHour(), getMinute(nowDate, hyperParameters), 0, 0, nowDate.getZone());

		ZonedDateTime targetFrom = until.plusMinutes(hyperParameters.getInterval());
		ZonedDateTime targetTo = targetFrom.plusHours(24 * predictionFor);

		ArrayList<Double> target = this.getTargetData(targetFrom, targetTo, csv, hyperParameters);
		double rmsSeasonality = PerformanceMatrix.rmsError(target, pre);
		double rmsTrend = PerformanceMatrix.rmsError(target, predictedTrend);

		System.out.println("Target = " + DataModification.constantScaling(target, 1));
		System.out.println("PredictedSeasonality = " + DataModification.constantScaling(pre, 1));

		System.out.println("Target = " + target);
		System.out.println("Predicted trend = " + DataModification.constantScaling(predictedTrend, 1));
		System.out.println("rmsTrend = " + rmsTrend);
		System.out.println("rms Seasonality= " + rmsSeasonality);
		System.out.println("accuracy trend = " + PerformanceMatrix.accuracy(target, predictedTrend, .15));
		System.out.println("accuracy seasonlity = " + PerformanceMatrix.accuracy(target, pre, .15));

	}

	@Test

	public void predictionTestMultivarient() {
		ArrayList<ArrayList<Double>> predictedSeasonality = new ArrayList<ArrayList<Double>>();
		ArrayList<Double> predictedTrend = new ArrayList<Double>();

		String csv = "1" + ".csv";

		ZonedDateTime globDate = ZonedDateTime.of(2022, 6, 16, 0, 0, 0, 0, ZonedDateTime.now().getZone());

		HyperParameters hyperParameters = ReadAndSaveModels.read("ConsumptionActivePower");

		hyperParameters.printHyperParameters();

		int predictionFor = 1;
		for (int i = 0; i < predictionFor; i++) {

			ZonedDateTime nowDate = globDate;
			nowDate = nowDate.plusHours(24 * i);

			var tempPredicted = this.predictSeasonalityMultivarent(hyperParameters, nowDate, csv);
			predictedSeasonality.add(tempPredicted);

		}

		for (int i = 0; i < predictionFor; i++) {
			ZonedDateTime nowDate = globDate;
			nowDate = nowDate.plusHours(24 * i);
			predictedTrend = this.predictTrendOneDayMultivarent(hyperParameters, nowDate, csv);

		}
		ArrayList<Double> pre = UtilityConversion.to1DArrayList(predictedSeasonality);
		ZonedDateTime nowDate = globDate;
		ZonedDateTime until = ZonedDateTime.of(nowDate.getYear(), nowDate.getMonthValue(), nowDate.getDayOfMonth(),
				nowDate.getHour(), getMinute(nowDate, hyperParameters), 0, 0, nowDate.getZone());

		ZonedDateTime targetFrom = until.plusMinutes(hyperParameters.getInterval());
		ZonedDateTime targetTo = targetFrom.plusHours(24 * predictionFor);

		// changing target data for refrence
		ArrayList<Double> target = this.getTargetData(targetFrom, targetTo, csv, hyperParameters);
		var ref = this.getTargetRefrence(targetFrom, targetTo);

		var trend = DataModification.elementWiseDiv(predictedTrend, ref);

		double rmsSeasonality = PerformanceMatrix.rmsError(target, pre);
		double rmsTrend = PerformanceMatrix.rmsError(target, trend);

		System.out.println("Target = " + DataModification.constantScaling(target, 1));
		System.out.println("PredictedSeasonality = " + DataModification.constantScaling(pre, 1));

		System.out.println("Target = " + target);
		System.out.println("Predicted trend = " + DataModification.constantScaling(trend, 1));
		System.out.println("rmsTrend = " + rmsTrend);
		System.out.println("rms Seasonality= " + rmsSeasonality);
		System.out.println("accuracy trend = " + PerformanceMatrix.accuracy(target, trend, .15));
		System.out.println("accuracy seasonlity = " + PerformanceMatrix.accuracy(target, pre, .15));

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
	public ArrayList<Double> queryData(ZonedDateTime fromDate, ZonedDateTime untilDate, String path) {
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

	public ArrayList<OffsetDateTime> queryDate(ZonedDateTime fromDate, ZonedDateTime untilDate, String path) {
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
	 * Doing what it suppose to do.
	 * 
	 * @param hyperParameters the Hyperparam
	 * @param nowDate         nowDate
	 * @param csvFileName     csvFileName
	 * @return predicted the predicted
	 */
	public ArrayList<Double> predictSeasonality(HyperParameters hyperParameters, ZonedDateTime nowDate,
			String csvFileName) {

		ZonedDateTime until = ZonedDateTime.of(nowDate.getYear(), nowDate.getMonthValue(), nowDate.getDayOfMonth(),
				nowDate.getHour(), getMinute(nowDate, hyperParameters), 0, 0, nowDate.getZone());

		int windowSize = hyperParameters.getWindowSizeSeasonality();

		nowDate = nowDate.plusMinutes(hyperParameters.getInterval());

		ZonedDateTime temp = until.minusDays(windowSize);
		ZonedDateTime fromDate = ZonedDateTime.of(temp.getYear(), temp.getMonthValue(), temp.getDayOfMonth(),
				nowDate.getHour(), getMinute(nowDate, hyperParameters), 0, 0, temp.getZone());

		ArrayList<Double> data = this.queryData(fromDate, until, csvFileName);

		final ArrayList<OffsetDateTime> date = this.queryDate(fromDate, until, csvFileName);

		ZonedDateTime targetFrom = until.plusMinutes(hyperParameters.getInterval());

		ArrayList<Double> predicted = LstmPredictor.getArranged(
				LstmPredictor.getIndex(targetFrom.getHour(), targetFrom.getMinute(), hyperParameters),
				/* LstmPredictor.predictSeasonality(data, date, hyperParameters) */
				PredictSeasonalityTest.predictSeasonalityTest(data, date, hyperParameters));

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

		ZonedDateTime until = ZonedDateTime.of(nowDate.getYear(), nowDate.getMonthValue(), nowDate.getDayOfMonth(),
				nowDate.getHour(), getMinute(nowDate, hyperParameters), 0, 0, nowDate.getZone());

		int windowSize = hyperParameters.getWindowSizeSeasonality();

		nowDate = nowDate.plusMinutes(hyperParameters.getInterval());

		ZonedDateTime temp = until.minusDays(windowSize);
		ZonedDateTime fromDate = ZonedDateTime.of(temp.getYear(), temp.getMonthValue(), temp.getDayOfMonth(),
				nowDate.getHour(), getMinute(nowDate, hyperParameters), 0, 0, temp.getZone());

		ArrayList<Double> data = this.queryData(fromDate, until, csvFileName);

		final ArrayList<OffsetDateTime> date = this.queryDate(fromDate, until, csvFileName);

		// ------------------------->
		var refdata = this.generateRefrence(date);
		var toPredictData = DataModification.elementWiseMultiplication(refdata, data);
		// -------------------------->

		ZonedDateTime targetFrom = until.plusMinutes(hyperParameters.getInterval());

		ArrayList<Double> predicted = LstmPredictor.getArranged(
				LstmPredictor.getIndex(targetFrom.getHour(), targetFrom.getMinute(), hyperParameters),
				LstmPredictor.predictSeasonality(toPredictData, date, hyperParameters));

		// postprocess
		var targetRef = this.getTargetRefrence(fromDate, until);
		var temp1 = DataModification.elementWiseDiv(predicted, targetRef);

		return temp1;

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

		// ArrayList<Double> val = new ArrayList<Double>();
		ArrayList<Double> forTrendPrediction = new ArrayList<Double>();
		ArrayList<OffsetDateTime> dateForTrend = new ArrayList<OffsetDateTime>();
		ArrayList<Double> predicted = new ArrayList<Double>();
		for (int i = 0; i < 60 / hyperParameters.getInterval() * 24; i++) {

			ZonedDateTime nowDateTemp = nowDate.plusMinutes(i * hyperParameters.getInterval());
			System.out.println(nowDateTemp);

			ZonedDateTime until = ZonedDateTime.of(nowDateTemp.getYear(), nowDateTemp.getMonthValue(),
					nowDateTemp.getDayOfMonth(), nowDateTemp.getHour(), getMinute(nowDateTemp, hyperParameters), 0, 0,
					nowDateTemp.getZone());

			forTrendPrediction = this.queryData(
					until.minusMinutes(hyperParameters.getInterval() * hyperParameters.getWindowSizeTrend()), until,
					csvFileName);
			dateForTrend = this.queryDate(
					until.minusMinutes(hyperParameters.getInterval() * hyperParameters.getWindowSizeTrend()), until,
					csvFileName);

			predicted.add(LstmPredictor.predictTrend(forTrendPrediction, dateForTrend, until, hyperParameters)

					/*
					 * TODO for new pipeline PredictTrendTest.predictTrendtest(forTrendPrediction,
					 * dateForTrend, until, hyperParameters)
					 */
					.get(0));

		}
		return predicted;
	}

	public ArrayList<Double> predictTrendOneDayMultivarent(HyperParameters hyperParameters, ZonedDateTime nowDate,
			String csvFileName) {

		// ArrayList<Double> val = new ArrayList<Double>();
		ArrayList<Double> forTrendPrediction = new ArrayList<Double>();
		ArrayList<OffsetDateTime> dateForTrend = new ArrayList<OffsetDateTime>();
		ArrayList<Double> predicted = new ArrayList<Double>();
		for (int i = 0; i < 60 / hyperParameters.getInterval() * 24; i++) {

			ZonedDateTime nowDateTemp = nowDate.plusMinutes(i * hyperParameters.getInterval());
			System.out.println(nowDateTemp);

			ZonedDateTime until = ZonedDateTime.of(nowDateTemp.getYear(), nowDateTemp.getMonthValue(),
					nowDateTemp.getDayOfMonth(), nowDateTemp.getHour(), getMinute(nowDateTemp, hyperParameters), 0, 0,
					nowDateTemp.getZone());

			forTrendPrediction = this.queryData(
					until.minusMinutes(hyperParameters.getInterval() * hyperParameters.getWindowSizeTrend()), until,
					csvFileName);
			dateForTrend = this.queryDate(
					until.minusMinutes(hyperParameters.getInterval() * hyperParameters.getWindowSizeTrend()), until,
					csvFileName);

			// modification for multivariant
			var tempData = this.generateRefrence(dateForTrend);
			tempData = DataModification.elementWiseMultiplication(forTrendPrediction, tempData);

			predicted.add(LstmPredictor.predictTrend(tempData, dateForTrend, until, hyperParameters)
					// TODO use this for new pipeline
					// PredictTrendTest.predictTrendtest(tempData, dateForTrend, until,
					// hyperParameters)
					.get(0));
		}
		return predicted;
	}

	/**
	 * Gives target data to compare.
	 * 
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

	public ArrayList<Double> generateRefrence(ArrayList<OffsetDateTime> date) {

		// one hour = 360/24 degree
		// one minute = 360/(24*60) degree
		ArrayList<Double> data = new ArrayList<Double>();

		for (int i = 0; i < date.size(); i++) {
			var hour = date.get(i).getHour();
			var minute = date.get(i).getMinute();
			double deg = 360 * hour / 24;
			double degDec = 360 * minute / (24 * 60);
			double angle = deg + degDec;
			double addVal = Math.cos(Math.toRadians(angle));
			data.add(1.5 + addVal);
		}
		return data;
	}

	public static ArrayList<Double> generateReference(ArrayList<ZonedDateTime> date) {
		// one hour = 360/24 degrees
		// one minute = 360/(24*60) degrees
		ArrayList<Double> data = new ArrayList<>();

		for (ZonedDateTime zonedDateTime : date) {
			int hour = zonedDateTime.getHour();
			int minute = zonedDateTime.getMinute();
			double deg = 360.0 * hour / 24.0;
			double degDec = 360.0 * minute / (24.0 * 60.0);
			double angle = deg + degDec;
			double addVal = Math.cos(Math.toRadians(angle));

			data.add(1.5 + addVal);

		}

		return data;
	}

	ArrayList<Double> getTargetRefrence(ZonedDateTime from, ZonedDateTime to) {
		System.out.println(from);
		System.out.println(to);
		int interval = 5;
		int hour = 24;
		int dataLen = 60 / interval * hour;
		ArrayList<ZonedDateTime> date = new ArrayList<ZonedDateTime>();
		for (int i = 0; i < dataLen; i++) {
			date.add(from.plusMinutes(i * interval));

		}

		return generateReference(date);

	}
}
