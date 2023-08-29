package io.openems.edge.predictor.lstm.preprocessing;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

public class GroupBy {

	ArrayList<Double> data = new ArrayList<Double>();
	ArrayList<OffsetDateTime> date = new ArrayList<OffsetDateTime>();
	ArrayList<ArrayList<OffsetDateTime>> groupedDateByYear = new ArrayList<ArrayList<OffsetDateTime>>();
	ArrayList<ArrayList<Double>> groupedDataByYear = new ArrayList<ArrayList<Double>>();

	public ArrayList<ArrayList<OffsetDateTime>> groupedDateByMin = new ArrayList<ArrayList<OffsetDateTime>>();
	public ArrayList<ArrayList<Double>> groupedDataByMin = new ArrayList<ArrayList<Double>>();

	public ArrayList<ArrayList<OffsetDateTime>> groupedDateByHour = new ArrayList<ArrayList<OffsetDateTime>>();
	public ArrayList<ArrayList<Double>> groupedDataByHour = new ArrayList<ArrayList<Double>>();

	public GroupBy(List<Double> data2, List<OffsetDateTime> date2) {
		data = (ArrayList<Double>) data2;
		date = (ArrayList<OffsetDateTime>) date2;
	}

//	private void year() {
//		/**
//		 * Should contain arraylist of arraylist of utc time; Inner list should contain
//		 * the date grouped by year; Should contain arraylist of arraylist of double;
//		 * inner list should contain corresponding data grouped by year
//		 */
//		/**
//		 * Find out how many unique year are there in the
//		 */
//		ArrayList<Integer> yearData = new ArrayList<Integer>();
//		ArrayList<OffsetDateTime> groupedDateByYearTemp = new ArrayList<OffsetDateTime>();
//		ArrayList<Double> groupedDataByYearTemp = new ArrayList<Double>();
//
//		for (int i = 0; i < date.size(); i++) {
//			// OffsetDateTime odt = date.get(i).atOffset(java.time.ZoneOffset.UTC);
//			int temp = date.get(i).getYear();
//			yearData.add(temp);
//		}
//		/**
//		 * get the uniqe list of years and sort it
//		 */
//		Set<Integer> uniqueSet = new HashSet<>(yearData);
//		ArrayList<Integer> uniqueList = new ArrayList<>(uniqueSet);
//		Collections.sort(uniqueList);
//		// System.out.println(uniqueList);
//
//		/**
//		 * Chek the unique data in the date Group data and date
//		 */
//		for (int i = 0; i < uniqueList.size(); i++) {
//			for (int j = 0; j < date.size(); j++) {
////		System.out.println(j);
////		System.out.println(date.size());
//
//				if (uniqueList.get(i) == date.get(j).getYear()) {
//					groupedDateByYearTemp.add(date.get(j));
//					groupedDataByYearTemp.add(data.get(j));
////			 System.out.println(groupedDateByYearTemp);
//
//				}
//
//			}
//			groupedDateByYear.add(groupedDateByYearTemp);
//			groupedDataByYear.add(groupedDataByYearTemp);
//			groupedDateByYearTemp = new ArrayList<OffsetDateTime>();
//			groupedDataByYearTemp = new ArrayList<Double>();
//		}
////System.out.print(groupedDataByYear.size());
//	}

	static void day() {

	}

	static void dayOfWeek() {

	}

	public void hour() {

		ArrayList<Integer> hourData = new ArrayList<Integer>();
		ArrayList<OffsetDateTime> groupedDateByHourTemp = new ArrayList<OffsetDateTime>();
		ArrayList<Double> groupedDataByHourTemp = new ArrayList<Double>();

		for (int i = 0; i < date.size(); i++) {
			// OffsetDateTime odt = date.get(i).atOffset(java.time.ZoneOffset.UTC);
			int temp = date.get(i).getHour();
			hourData.add(temp);
		}

		Set<Integer> uniqueSet = new HashSet<>(hourData);
		ArrayList<Integer> uniqueList = new ArrayList<>(uniqueSet);
		Collections.sort(uniqueList);
//		System.out.println(uniqueList);

		/**
		 * Chek the unique data in the date Group data and date
		 */
		for (int i = 0; i < uniqueList.size(); i++) {
			for (int j = 0; j < date.size(); j++) {
//		System.out.println(j);
//		System.out.println(date.size());

				if (uniqueList.get(i) == date.get(j).getHour()) {
					groupedDateByHourTemp.add(date.get(j));
					groupedDataByHourTemp.add(data.get(j));
//			 System.out.println(groupedDateByYearTemp);

				}

			}
			groupedDateByHour.add(groupedDateByHourTemp);
			groupedDataByHour.add(groupedDataByHourTemp);
			groupedDateByHourTemp = new ArrayList<OffsetDateTime>();
			groupedDataByHourTemp = new ArrayList<Double>();
		}
		// System.out.println(groupedDataByHour.size());
		// System.out.println(groupedDateByHour.get(0).size());

	}

	public void minute() {

		ArrayList<Integer> minData = new ArrayList<Integer>();
		ArrayList<OffsetDateTime> groupedDateByMinTemp = new ArrayList<OffsetDateTime>();
		ArrayList<Double> groupedDataByMinTemp = new ArrayList<Double>();

		for (int i = 0; i < date.size(); i++) {
			// OffsetDateTime odt = date.get(i).atOffset(java.time.ZoneOffset.UTC);
			int temp = date.get(i).getMinute();
			minData.add(temp);
		}

		Set<Integer> uniqueSet = new HashSet<>(minData);
		ArrayList<Integer> uniqueList = new ArrayList<>(uniqueSet);
		Collections.sort(uniqueList);
//		System.out.println(uniqueList);

		/**
		 * Chek the unique data in the date Group data and date
		 */
		for (int i = 0; i < uniqueList.size(); i++) {
			for (int j = 0; j < date.size(); j++) {
//		System.out.println(j);
//		System.out.println(date.size());

				if (uniqueList.get(i) == date.get(j).getMinute()) {
					groupedDateByMinTemp.add(date.get(j));
					groupedDataByMinTemp.add(data.get(j));
//			 System.out.println(groupedDateByYearTemp);

				}

			}
			groupedDateByMin.add(groupedDateByMinTemp);
			groupedDataByMin.add(groupedDataByMinTemp);
			groupedDateByMinTemp = new ArrayList<OffsetDateTime>();
			groupedDataByMinTemp = new ArrayList<Double>();
		}
		// System.out.print(groupedDataByMin.size());

	}

}
