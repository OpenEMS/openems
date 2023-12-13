package io.openems.edge.predictor.lstm.preprocessing;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.openems.edge.predictor.lstm.common.HyperParameters;

public class GroupBy {

	private ArrayList<Double> data = new ArrayList<Double>();
	private ArrayList<OffsetDateTime> date = new ArrayList<OffsetDateTime>();
	private ArrayList<ArrayList<OffsetDateTime>> groupedDateByMin = new ArrayList<ArrayList<OffsetDateTime>>();
	private ArrayList<ArrayList<Double>> groupedDataByMin = new ArrayList<ArrayList<Double>>();
	private ArrayList<ArrayList<OffsetDateTime>> groupedDateByHour = new ArrayList<ArrayList<OffsetDateTime>>();
	private ArrayList<ArrayList<Double>> groupedDataByHour = new ArrayList<ArrayList<Double>>();

	public GroupBy(List<Double> data2, List<OffsetDateTime> date2) {
		this.data = (ArrayList<Double>) data2;
		this.date = (ArrayList<OffsetDateTime>) date2;
	}

	/**
	 * Extracts 24-hour intervals of data from the given list and transposes the
	 * result. Each interval is determined by the specified interval in minutes and
	 * the window size.
	 *
	 * @param data            The input list of numerical data.
	 * @param hyperParameters The hyperparameters used for processing the data.
	 * @return An ArrayList of ArrayLists representing the transposed 24-hour
	 *         intervals of data.
	 * @throws Exception if an error occurs during processing.
	 */

	public static ArrayList<ArrayList<Double>> twentyFourHour(ArrayList<Double> data, HyperParameters hyperParameters)
			throws Exception {

		int startingValue = 0;
		int endValue = (60 / hyperParameters.getInterval() * 24);
		ArrayList<ArrayList<Double>> firstGroup = new ArrayList<ArrayList<Double>>();
		while (data.size() >= endValue) {
			ArrayList<Double> tempList = new ArrayList<Double>();

			for (int i = startingValue; i < endValue; i++) {
				tempList.add(data.get(i));

			}

			firstGroup.add(tempList);

			startingValue = endValue;

			endValue = startingValue + (60 / hyperParameters.getInterval() * 24);

		}
	
		return firstGroup;

	}

	/**
	 * Groups data by days corresponding to the provided dates.
	 *
	 * @param data  The list of numerical data.
	 * @param dates The list of OffsetDateTime dates corresponding to the data.
	 * @return An ArrayList of ArrayLists representing data grouped by days.
	 */
	public static ArrayList<ArrayList<Double>> groupByDays(ArrayList<Double> data, ArrayList<OffsetDateTime> dates) {
		// Create a map to store data grouped by days
		Map<OffsetDateTime, ArrayList<Double>> groupedDataMap = new HashMap<>();

		// Iterate through data and dates to group by days
		for (int i = 0; i < data.size(); i++) {
			OffsetDateTime date = dates.get(i);
			ArrayList<Double> group = new ArrayList<>();
			group.add(data.get(i));
			groupedDataMap.merge(date, group, (existingGroup, newData) -> {
				existingGroup.addAll(newData);
				return existingGroup;
			});
		}

		// Convert the map values to ArrayList and return
		return new ArrayList<>(groupedDataMap.values());
	}

	/**
	 * This method processes a list of date-time values and extracts the hour
	 * component from each date-time, storing it in an ArrayList of integers.
	 */

	public void hour() {

		ArrayList<Integer> hourData = new ArrayList<Integer>();
		ArrayList<OffsetDateTime> groupedDateByHourTemp = new ArrayList<OffsetDateTime>();
		ArrayList<Double> groupedDataByHourTemp = new ArrayList<Double>();

		for (int i = 0; i < this.date.size(); i++) {

			int temp = this.date.get(i).getHour();
			hourData.add(temp);
		}

		Set<Integer> uniqueSet = new HashSet<>(hourData);
		ArrayList<Integer> uniqueList = new ArrayList<>(uniqueSet);
		Collections.sort(uniqueList);

		/**
		 * Chek the unique data in the date Group data and date
		 */
		for (int i = 0; i < uniqueList.size(); i++) {
			for (int j = 0; j < this.date.size(); j++) {

				if (uniqueList.get(i) == this.date.get(j).getHour()) {
					groupedDateByHourTemp.add(this.date.get(j));
					groupedDataByHourTemp.add(this.data.get(j));

				}

			}
			this.groupedDateByHour.add(groupedDateByHourTemp);
			this.groupedDataByHour.add(groupedDataByHourTemp);
			groupedDateByHourTemp = new ArrayList<OffsetDateTime>();
			groupedDataByHourTemp = new ArrayList<Double>();
		}

	}

	/**
	 * This method processes a list of date-time values and their associated data,
	 * extracting the minute component from each date-time and grouping them into
	 * separate lists based on unique minute values. The resulting grouped data is
	 * stored in class member ArrayLists.
	 */

	public void minute() {

		ArrayList<Integer> minData = new ArrayList<Integer>();
		ArrayList<OffsetDateTime> groupedDateByMinTemp = new ArrayList<OffsetDateTime>();
		ArrayList<Double> groupedDataByMinTemp = new ArrayList<Double>();

		for (int i = 0; i < this.date.size(); i++) {
			// OffsetDateTime odt = date.get(i).atOffset(java.time.ZoneOffset.UTC);
			int temp = this.date.get(i).getMinute();
			minData.add(temp);
		}

		Set<Integer> uniqueSet = new HashSet<>(minData);
		ArrayList<Integer> uniqueList = new ArrayList<>(uniqueSet);
		Collections.sort(uniqueList);

		/**
		 * Chek the unique data in the date Group data and date
		 */
		for (int i = 0; i < uniqueList.size(); i++) {
			for (int j = 0; j < this.date.size(); j++) {

				if (uniqueList.get(i) == this.date.get(j).getMinute()) {
					groupedDateByMinTemp.add(this.date.get(j));
					groupedDataByMinTemp.add(this.data.get(j));

				}

			}
			this.groupedDateByMin.add(groupedDateByMinTemp);
			this.groupedDataByMin.add(groupedDataByMinTemp);
			groupedDateByMinTemp = new ArrayList<OffsetDateTime>();
			groupedDataByMinTemp = new ArrayList<Double>();
		}
		// System.out.print(groupedDataByMin.size());

	}

	public ArrayList<ArrayList<Double>> getDataGroupedByHour() {
		return this.groupedDataByHour;

	}

	public ArrayList<ArrayList<OffsetDateTime>> getDateGroupedByHour() {
		return this.groupedDateByHour;
	}

	public ArrayList<ArrayList<Double>> getDataGroupedByMinute() {
		return this.groupedDataByMin;
	}

	public ArrayList<ArrayList<OffsetDateTime>> getDateGroupedByMinute() {
		return this.groupedDateByMin;
	}
}
