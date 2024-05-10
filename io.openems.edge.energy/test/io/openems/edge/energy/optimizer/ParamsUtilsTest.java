package io.openems.edge.energy.optimizer;

import static io.openems.edge.energy.optimizer.ParamsUtils.calculateChargeEnergyInChargeGrid;
import static io.openems.edge.energy.optimizer.ParamsUtils.calculatePeriodLengthHourFromIndex;
import static org.junit.Assert.assertEquals;

import java.time.ZonedDateTime;

import org.junit.Test;

public class ParamsUtilsTest {

	@Test
	public void testCalculateParamsMaxChargeEnergyInChargeGrid() {
		assertEquals(1250, calculateChargeEnergyInChargeGrid(1000, 11000, new int[0], new int[0], new double[0]));

		assertEquals(583, calculateChargeEnergyInChargeGrid(1000, 11000, //
				new int[] { 0, 100, 200 }, //
				new int[] { 1000, 1100 }, //
				new double[0]));

		assertEquals(496, calculateChargeEnergyInChargeGrid(1000, 11000, //
				new int[] { 0, 100, 200, 300, 400, 500, 600, 700 }, //
				new int[] { 700, 600, 500, 400, 300, 200, 100, 0 }, //
				new double[] { 123, 124, 125, 126, 123, 122, 121, 120 }));

		assertEquals(468, calculateChargeEnergyInChargeGrid(1000, 11000, //
				new int[] { 0, 100, 200, 300, 400, 500, 600, 700 }, //
				new int[] { 700, 600, 500, 1140, 1150, 200, 100, 0 }, //
				new double[] { 120, 121, 122, 126, 125, 122, 121, 120 }));
	}

	@Test
	public void testCalculatePeriodLengthHourFromIndex() {
		assertEquals(24, calculatePeriodLengthHourFromIndex(ZonedDateTime.parse("2020-03-04T14:00:00.00Z")));
		assertEquals(24 + 3, calculatePeriodLengthHourFromIndex(ZonedDateTime.parse("2020-03-04T14:15:00.00Z")));
		assertEquals(24 + 2, calculatePeriodLengthHourFromIndex(ZonedDateTime.parse("2020-03-04T14:30:00.00Z")));
		assertEquals(24 + 1, calculatePeriodLengthHourFromIndex(ZonedDateTime.parse("2020-03-04T14:45:00.00Z")));
		assertEquals(24, calculatePeriodLengthHourFromIndex(ZonedDateTime.parse("2020-03-04T15:00:00.00Z")));
	}
}
