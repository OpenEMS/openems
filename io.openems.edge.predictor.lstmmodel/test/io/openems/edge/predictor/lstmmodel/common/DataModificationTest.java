package io.openems.edge.predictor.lstmmodel.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;

public class DataModificationTest {

	@Test
	public void testGroupDataByHourAndMinute() {
		ArrayList<Double> testData = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0));
		ArrayList<OffsetDateTime> testDate = new ArrayList<>(Arrays.asList(OffsetDateTime.parse("2022-01-01T10:15:30Z"),
				OffsetDateTime.parse("2022-01-01T11:30:45Z"), OffsetDateTime.parse("2022-01-01T10:45:00Z"),
				OffsetDateTime.parse("2022-01-01T11:15:00Z"), OffsetDateTime.parse("2022-01-01T10:30:00Z"),
				OffsetDateTime.parse("2022-01-01T10:15:30Z"), OffsetDateTime.parse("2022-01-01T11:30:45Z"),
				OffsetDateTime.parse("2022-01-01T10:45:00Z"), OffsetDateTime.parse("2022-01-01T11:15:00Z"),
				OffsetDateTime.parse("2022-01-01T10:30:00Z")));

		ArrayList<ArrayList<ArrayList<Double>>> result = DataModification.groupDataByHourAndMinute(testData, testDate);

		assertEquals(2, result.size());
	}

	@Test
	public void testCombinedArray() {
		ArrayList<ArrayList<Double>> testData1 = new ArrayList<>(
				Arrays.asList(new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0)),
						new ArrayList<>(Arrays.asList(4.0, 5.0, 6.0)), new ArrayList<>(Arrays.asList(7.0, 8.0, 9.0))));
		ArrayList<Double> expectedResult1 = new ArrayList<>(Arrays.asList(1.0, 4.0, 7.0, 2.0, 5.0, 8.0, 3.0, 6.0, 9.0));
		assertEquals(expectedResult1, DataModification.combinedArray(testData1));

		ArrayList<ArrayList<Double>> testData2 = new ArrayList<>();
		ArrayList<Double> expectedResult2 = new ArrayList<>();
		assertEquals(expectedResult2, DataModification.combinedArray(testData2));
	}

	@Test
	public void testModifyFortrendPrediction() {
		ArrayList<Double> testData = new ArrayList<>(List.of(1.0, 2.0, 3.0, 4.0, 5.0));
		ArrayList<OffsetDateTime> testDates = new ArrayList<>(List.of(OffsetDateTime.parse("2022-01-01T12:30:00Z"),
				OffsetDateTime.parse("2022-01-01T12:45:00Z"), OffsetDateTime.parse("2022-01-01T13:00:00Z"),
				OffsetDateTime.parse("2022-01-01T13:15:00Z"), OffsetDateTime.parse("2022-01-01T13:30:00Z")));
		HyperParameters testHyperParameters = new HyperParameters();

		ArrayList<ArrayList<Double>> result = DataModification.modifyFortrendPrediction(testData, testDates,
				testHyperParameters);

		assertNotNull(result);
		assertEquals(5, result.size());

	}

	@Test
	public void testScale() {
		ArrayList<Double> testData = new ArrayList<>();
		testData.add(10.0);
		testData.add(20.0);
		testData.add(30.0);

		ArrayList<Double> scaledData = DataModification.scale(testData, 10.0, 30.0);

		assertEquals(0.2, scaledData.get(0), 0.0001);
		assertEquals(0.5, scaledData.get(1), 0.0001);
		assertEquals(0.8, scaledData.get(2), 0.0001);
	}

	@Test
	public void testScaleBack() {
		double scaledValue = 0.5;
		double minOriginal = 10.0;
		double maxOriginal = 30.0;

		double originalValue = DataModification.scaleBack(scaledValue, minOriginal, maxOriginal);
		assertEquals(20.0, originalValue, 0.0001);
	}

	@Test
	public void groupByTest() {
		HyperParameters hyperParameters = new HyperParameters();
		ArrayList<Double> data = new ArrayList<Double>();
		ArrayList<OffsetDateTime> date = new ArrayList<OffsetDateTime>();
		int interval = hyperParameters.getInterval();
		int forDays = 2;
		int itter = forDays * 24 * 60 / interval;
		// generating data
		OffsetDateTime startingDate = OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(1));
		for (int i = 0; i < itter; i++) {
			date.add(startingDate.plusMinutes(i * interval));
			data.add(i + 0.00);

		}

		ArrayList<ArrayList<ArrayList<Double>>> groupedData = DataModification.groupDataByHourAndMinute(data, date);
		for (int i = 0; i < groupedData.size(); i++) {
			for (int j = 0; j < groupedData.get(i).get(j).size(); j++) {
				assertEquals(groupedData.get(i).get(j).size(), forDays);
			}
		}

	}

	@Test
	public void getDataInBatchTest() {
		ArrayList<Double> data = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0));
		int numberOfGroups = 2;
		ArrayList<ArrayList<Double>> result = DataModification.getDataInBatch(data, numberOfGroups);
		assertEquals(result.size(), numberOfGroups);
		int i = 0;
		for (ArrayList<Double> outerVal : result) {
			for (double val : outerVal) {
				assertEquals(val, data.get(i), 0.00001);
				i++;

			}

		}

	}

	@Test

	public void getDateInBatchTest() {

		ArrayList<OffsetDateTime> dateList = new ArrayList<OffsetDateTime>();
		HyperParameters hyperParameters = new HyperParameters();
		OffsetDateTime startingDate = OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(1));
		int numberOfGroups = 2;
		int j = 0;

		// populating Date list

		for (int i = 0; i < 10; i++) {
			dateList.add(startingDate.plusMinutes(i * hyperParameters.getInterval()));
		}

		ArrayList<ArrayList<OffsetDateTime>> result = DataModification.getDateInBatch(dateList, numberOfGroups);
		assertEquals(result.size(), numberOfGroups);

		for (ArrayList<OffsetDateTime> outerVal : result) {
			for (OffsetDateTime val : outerVal) {
				assertEquals(val, dateList.get(j));
				j++;

			}

		}

	}

	@Test

	public void removeNegatives() {

		ArrayList<Double> inputList = new ArrayList<>(Arrays.asList(5.0, -3.0, 2.0, -7.5));
		ArrayList<Double> expectedList = new ArrayList<>(Arrays.asList(5.0, 0.0, 2.0, 0.0));

		ArrayList<Double> resultList = DataModification.removeNegatives(inputList);

		assertEquals(expectedList, resultList);

	}

	@Test
	public void constantScalingTest() {

		ArrayList<Double> inputData = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0));
		double scalingFactor = 2.0;
		ArrayList<Double> expectedOutput = new ArrayList<>(Arrays.asList(2.0, 4.0, 6.0));

		ArrayList<Double> actualOutput = DataModification.constantScaling(inputData, scalingFactor);

		assertEquals(expectedOutput, actualOutput);

	}

}
