package io.openems.edge.predictor.lstm.preprocessing;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

public class GroupBy {

	private final ArrayList<Double> data;
	private final ArrayList<OffsetDateTime> date;

	private final ArrayList<ArrayList<OffsetDateTime>> groupedDateByMin = new ArrayList<>();
	private final ArrayList<ArrayList<Double>> groupedDataByMin = new ArrayList<>();
	private final ArrayList<ArrayList<OffsetDateTime>> groupedDateByHour = new ArrayList<>();
	private final ArrayList<ArrayList<Double>> groupedDataByHour = new ArrayList<>();

	/**
	 * Group by Temporal filed.
	 * 
	 * @param chronoField     {@link ChronoField}
	 * @param groupedDateList The list of groupedDateList.
	 * @param groupedDataList The list of groupedDataList.
	 */
	public void groupByTemporalField(ChronoField chronoField, List<ArrayList<OffsetDateTime>> groupedDateList,
			List<ArrayList<Double>> groupedDataList) {
		var uniqueList = this.extractUniqueAndSortedValues(chronoField);

		for (var uniqueValue : uniqueList) {
			var groupedDateTemp = this.groupDatesByUniqueValue(uniqueValue, chronoField);
			var groupedDataTemp = this.groupDataByUniqueValue(uniqueValue, chronoField);

			groupedDateList.add(new ArrayList<>(groupedDateTemp));
			groupedDataList.add(new ArrayList<>(groupedDataTemp));
		}
	}

	private List<Integer> extractUniqueAndSortedValues(ChronoField chronoField) {
		return this.date.stream()//
				.map(date -> date.get(chronoField)) //
				.distinct() //
				.sorted() //
				.collect(toList());
	}

	private List<OffsetDateTime> groupDatesByUniqueValue(Integer uniqueValue, ChronoField chronoField) {
		return this.date.stream()//
				.filter(date -> uniqueValue.equals(date.get(chronoField)))//
				.collect(toList());
	}

	private List<Double> groupDataByUniqueValue(Integer uniqueValue, ChronoField chronoField) {
		return range(0, this.data.size())//
				.filter(i -> {
					double dateValue = this.date.get(i).get(chronoField);
					return Double.compare(dateValue, uniqueValue.doubleValue()) == 0;
				}) //
				.mapToObj(i -> this.data.get(i)) //
				.collect(toList());
	}

	/**
	 * grouping by hour.
	 */
	public void hour() {
		this.groupByTemporalField(ChronoField.HOUR_OF_DAY, this.groupedDateByHour, this.groupedDataByHour);
	}

	/**
	 * grouping by minute.
	 */
	public void minute() {
		this.groupByTemporalField(ChronoField.MINUTE_OF_HOUR, this.groupedDateByMin, this.groupedDataByMin);
	}

	public ArrayList<ArrayList<Double>> getGroupedDataByHour() {
		return this.groupedDataByHour;
	}

	public ArrayList<ArrayList<OffsetDateTime>> getGroupedDateByHour() {
		return this.groupedDateByHour;
	}

	public ArrayList<ArrayList<Double>> getGroupedDataByMinute() {
		return this.groupedDataByMin;
	}

	public ArrayList<ArrayList<OffsetDateTime>> getGroupedDateByMinute() {
		return this.groupedDateByMin;
	}

	public GroupBy(ArrayList<Double> data, List<OffsetDateTime> date) {
		this.data = new ArrayList<>(data);
		this.date = new ArrayList<>(date);
	}
}