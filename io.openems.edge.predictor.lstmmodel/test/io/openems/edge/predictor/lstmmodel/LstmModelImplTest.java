package io.openems.edge.predictor.lstmmodel;

import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.utilities.DataUtility;

public class LstmModelImplTest {

	private static HyperParameters hyperParameters = new HyperParameters();

	@Test
	public void test() {

		var dateTime1 = ZonedDateTime.of(2022, 1, 1, 12, 4, 0, 0, ZoneId.systemDefault());
		var res = DataUtility.getMinute(dateTime1, hyperParameters).intValue();
		assertEquals(0, res);

		var dateTime2 = ZonedDateTime.of(2022, 1, 1, 12, 8, 0, 0, ZoneId.systemDefault());
		var res1 = DataUtility.getMinute(dateTime2, hyperParameters).intValue();
		assertEquals(5, res1);

		var dateTime3 = ZonedDateTime.of(2022, 1, 1, 12, 36, 0, 0, ZoneId.systemDefault());
		var res2 = DataUtility.getMinute(dateTime3, hyperParameters).intValue();

		assertEquals(35, res2);

		var dateTime4 = ZonedDateTime.of(2022, 1, 1, 12, 50, 0, 0, ZoneId.systemDefault());
		var res3 = DataUtility.getMinute(dateTime4, hyperParameters).intValue();
		assertEquals(50, res3);
	}

	@Test
	public void testCombine() {
		var testData = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0));
		var testData1 = new ArrayList<>(Arrays.asList(100.0, 200.0, 300.0, 400.0));
		var res = DataUtility.combine(testData1, testData);
		var expectedResult = new ArrayList<>(Arrays.asList(100.0, 200.0, 300.0, 400.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0));
		assertEquals(expectedResult, res);
	}
}
