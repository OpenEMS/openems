package io.openems.edge.goodwe.batteryinverter;

import static io.openems.edge.goodwe.batteryinverter.GoodWeBatteryInverterImpl.MAX_DC_CURRENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.openems.edge.goodwe.batteryinverter.GoodWeBatteryInverterImpl.BatteryData;

public class TestStatic {

	@Test
	public void testCalculateSurplusPower() throws Exception {
		// Battery Current is unknown -> null
		assertNull(GoodWeBatteryInverterImpl.calculateSurplusPower(new BatteryData(null, null), 5000));

		// Battery Current is > Max BatteryInverter DC Current -> null
		assertNull(GoodWeBatteryInverterImpl.calculateSurplusPower(new BatteryData(MAX_DC_CURRENT + 1, null), 5000));

		// Production Power is unknown or negative > null
		assertNull(GoodWeBatteryInverterImpl.calculateSurplusPower(new BatteryData(MAX_DC_CURRENT - 1, null), null));
		assertNull(GoodWeBatteryInverterImpl.calculateSurplusPower(new BatteryData(MAX_DC_CURRENT - 1, null), -1));

		// Max Charge Power exceeds Production Power
		assertNull(GoodWeBatteryInverterImpl.calculateSurplusPower(new BatteryData(20, 466) /* 9320 */, 5000));

		// Surplus Power is Production Power minus Max Charge Power
		assertEquals(5680,
				(int) GoodWeBatteryInverterImpl.calculateSurplusPower(new BatteryData(20, 466) /* 9320 */, 15000));
	}
}
