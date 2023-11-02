import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;

import io.openems.edge.predictor.lstm.common.ReadCsv;
import io.openems.edge.predictor.lstm.performance.PerformanceMatrix;
import io.openems.edge.predictor.lstm.predictor.Prediction;

public class StandAlonePrediction {
	/**
	 * This class contains a method for prediction testing.
	 */

	public void predictionTest() {
		ArrayList<Double> predicted = new ArrayList<Double>();
		ArrayList<Double> allPredicted = new ArrayList<Double>();
		ArrayList<Double> allTarget = new ArrayList<Double>();
		ArrayList<Double> forTrendPrediction = new ArrayList<Double>();
		ArrayList<Double> target = new ArrayList<Double>();

		String path = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\SavedModel.txt";
		String csvFileNAme = "17.csv";
		for (int i = 0; i < 1; i++) {

			ZonedDateTime nowDate = ZonedDateTime.of(2022, 6, 10, 4, 0, 0, 0, ZonedDateTime.now().getZone());
			nowDate = nowDate.plusDays(i);
			ZonedDateTime until = ZonedDateTime.of(nowDate.getYear(), nowDate.getMonthValue(), nowDate.getDayOfMonth(),
					nowDate.getHour(), getMinute(nowDate), 0, 0, nowDate.getZone());

			ZonedDateTime temp = until.minusDays(6);
			ZonedDateTime fromDate = ZonedDateTime.of(temp.getYear(), temp.getMonthValue(), temp.getDayOfMonth(),
					nowDate.getHour(), getMinute(nowDate), 0, 0, temp.getZone());
			ZonedDateTime targetFrom = until.plusMinutes(15);

			ZonedDateTime targetTo = targetFrom.plusHours(24);

			System.out.println("From : " + fromDate);
			System.out.println("Till : " + until);

			// quarry data

			final ArrayList<Double> data = this.quarryData(fromDate, until, csvFileNAme);
			target = this.quarryData(targetFrom, targetTo, csvFileNAme);
			allTarget.addAll(target);

			// quarry dates

			ArrayList<OffsetDateTime> date = this.quarryDate(fromDate, until, csvFileNAme);
			System.out.println(date);

			Prediction prediction = new Prediction(data, date, Collections.min(data), Collections.max(data), path);

			predicted = getArranged(getIndex(targetFrom.getHour(), targetFrom.getMinute()),
					prediction.getPredictedValues());
			allPredicted.addAll(predicted);

			// System.out.println(prediction.predicted);
			System.out.println("data size" + data.size());
			System.out.println("Target From = " + targetFrom);
			System.out.println("Target to= " + targetTo);
			System.out.println("Target = " + target);
			System.out.println("");
			// System.out.println("Last week same day: " + sameDayLastWeek);
			System.out.println("");
			System.out.println("Predicted = " + prediction.getPredictedValues());
			System.out.println("");
			System.out.println("forTrendfrom =" + until.minusMinutes(105));
			System.out.println("forTrendto =" + until);
			System.out.println("forTrendPrediction =" + forTrendPrediction);
			// System.out.println("forTrendPredictionr result =" + onePointPrediction);
			System.out.println("");
			System.out.println("Predicted Arranged= " + predicted);

			System.out.println("Predicted size= " + prediction.getPredictedValues().size());
			System.out.println("Target size = " + target.size());
			System.out.println("");
			PerformanceMatrix pm = new PerformanceMatrix(allTarget, allPredicted, 0.2);
			pm.statusReport();

		}

		Prediction.makePlot(allTarget, allPredicted, 0);

	}

	/**
	 * Gets the rounded minute value for the provided ZonedDateTime.
	 *
	 * @param nowDate The ZonedDateTime for which to determine the rounded minute
	 *                value.
	 * @return The rounded minute value (0, 15, 30, or 45) based on the minute
	 *         component of the input time.
	 */

	public static Integer getMinute(ZonedDateTime nowDate) {

		int nowMinute = nowDate.getMinute();
		if (nowMinute >= 0 && nowMinute < 15) {

			return 0;
		} else if (nowMinute >= 15 && nowMinute < 30) {
			return 15;
		} else if (nowMinute >= 30 && nowMinute < 45) {
			return 30;
		}
		return 45;
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
		// System.out.println(dates);

		// checking date in date arraylist
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
		ArrayList<Double> data = csv.getData();
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
	 * @param hour   The hour component (0-23) to be used for the calculation.
	 * @param minute The minute component (0, 15, 30, or 45) to be used for the
	 *               calculation.
	 * @return The index representing the specified hour and minute combination.
	 */

	public static Integer getIndex(Integer hour, Integer minute) {

		int k = 0;
		for (int i = 0; i < 24; i++) {
			for (int j = 0; j < 4; j++) {
				int h = i;
				int m = j * 15;
				if (hour == h && minute == m) {
					System.out.println(k);

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

}
