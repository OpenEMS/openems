package io.openems.edge.predictor.lstm.utillities;

import static io.openems.edge.predictor.lstm.utilities.DataUtility.combine;
import static io.openems.edge.predictor.lstm.utilities.DataUtility.getMinute;
import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import io.openems.edge.predictor.lstm.common.HyperParameters;

public class DataUtilityTest {

	private static final HyperParameters HYPER_PARAMETERS = new HyperParameters();

	@Test
	public void test() {
		var dateTime1 = ZonedDateTime.of(2022, 1, 1, 12, 4, 0, 0, ZoneId.systemDefault());
		var res = getMinute(dateTime1, HYPER_PARAMETERS).intValue();
		assertEquals(0, res);

		var dateTime2 = ZonedDateTime.of(2022, 1, 1, 12, 8, 0, 0, ZoneId.systemDefault());
		var res1 = getMinute(dateTime2, HYPER_PARAMETERS).intValue();
		assertEquals(5, res1);

		var dateTime3 = ZonedDateTime.of(2022, 1, 1, 12, 36, 0, 0, ZoneId.systemDefault());
		var res2 = getMinute(dateTime3, HYPER_PARAMETERS).intValue();

		assertEquals(35, res2);

		var dateTime4 = ZonedDateTime.of(2022, 1, 1, 12, 50, 0, 0, ZoneId.systemDefault());
		var res3 = getMinute(dateTime4, HYPER_PARAMETERS).intValue();
		assertEquals(50, res3);
	}

	@Test
	public void testCombine() {
		var testData = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0));
		var testData1 = new ArrayList<>(Arrays.asList(100.0, 200.0, 300.0, 400.0));
		var res = combine(testData1, testData);
		var expectedResult = new ArrayList<>(Arrays.asList(100.0, 200.0, 300.0, 400.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0));
		assertEquals(expectedResult, res);
	}

}
