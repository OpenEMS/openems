package io.openems.edge.predictor.lstm.preprocessing;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
	 * group by hour.
	 */
	public void hour() {

		ArrayList<Integer> hourData = new ArrayList<Integer>();
		ArrayList<OffsetDateTime> groupedDateByHourTemp = new ArrayList<OffsetDateTime>();
		ArrayList<Double> groupedDataByHourTemp = new ArrayList<Double>();

		for (int i = 0; i < this.date.size(); i++) {
			// OffsetDateTime odt = date.get(i).atOffset(java.time.ZoneOffset.UTC);
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
	 * group by minute.
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
		 * Check the unique data in the date Group data and date
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
	}

	public ArrayList<Double> getData() {
		return this.data;
	}

	public void setData(ArrayList<Double> data) {
		this.data = data;
	}

	public ArrayList<OffsetDateTime> getDate() {
		return this.date;
	}

	public void setDate(ArrayList<OffsetDateTime> date) {
		this.date = date;
	}

	public ArrayList<ArrayList<OffsetDateTime>> getGroupedDateByMin() {
		return this.groupedDateByMin;
	}

	public void setGroupedDateByMin(ArrayList<ArrayList<OffsetDateTime>> groupedDateByMin) {
		this.groupedDateByMin = groupedDateByMin;
	}

	public ArrayList<ArrayList<Double>> getGroupedDataByMin() {
		return this.groupedDataByMin;
	}

	public void setGroupedDataByMin(ArrayList<ArrayList<Double>> groupedDataByMin) {
		this.groupedDataByMin = groupedDataByMin;
	}

	public ArrayList<ArrayList<OffsetDateTime>> getGroupedDateByHour() {
		return this.groupedDateByHour;
	}

	public void setGroupedDateByHour(ArrayList<ArrayList<OffsetDateTime>> groupedDateByHour) {
		this.groupedDateByHour = groupedDateByHour;
	}

	public ArrayList<ArrayList<Double>> getGroupedDataByHour() {
		return this.groupedDataByHour;
	}

	public void setGroupedDataByHour(ArrayList<ArrayList<Double>> groupedDataByHour) {
		this.groupedDataByHour = groupedDataByHour;
	}
}
