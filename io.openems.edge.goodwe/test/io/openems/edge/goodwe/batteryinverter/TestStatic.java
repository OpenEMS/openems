package io.openems.edge.goodwe.batteryinverter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.openems.edge.common.channel.value.Value;
import io.openems.edge.goodwe.batteryinverter.GoodWeBatteryInverterImpl.BatteryData;

public class TestStatic {

	private static final int MAX_DC_CURRENT = 25;

	@Test
	public void testCalculateSurplusPower() throws Exception {
		// Battery Current is unknown -> null
		assertNull(GoodWeBatteryInverterImpl.calculateSurplusPower(new BatteryData(null, null), 5000, MAX_DC_CURRENT));

		// Battery Current is > Max BatteryInverter DC Current -> null
		assertNull(GoodWeBatteryInverterImpl.calculateSurplusPower(new BatteryData(MAX_DC_CURRENT + 1, null), 5000,
				MAX_DC_CURRENT));

		// Production Power is unknown or negative > null
		assertNull(GoodWeBatteryInverterImpl.calculateSurplusPower(new BatteryData(MAX_DC_CURRENT - 1, null), null,
				MAX_DC_CURRENT));
		assertNull(GoodWeBatteryInverterImpl.calculateSurplusPower(new BatteryData(MAX_DC_CURRENT - 1, null), -1,
				MAX_DC_CURRENT));

		// Max Charge Power exceeds Production Power
		assertNull(GoodWeBatteryInverterImpl.calculateSurplusPower(new BatteryData(20, 466) /* 9320 */, 5000,
				MAX_DC_CURRENT));

		// Surplus Power is Production Power minus Max Charge Power
		assertEquals(5680, (int) GoodWeBatteryInverterImpl.calculateSurplusPower(new BatteryData(20, 466) /* 9320 */,
				15000, MAX_DC_CURRENT));
	}

	@Test
	public void testPreprocessAmpereValue47900() {

		assertEquals(MAX_DC_CURRENT,
				GoodWeBatteryInverterImpl.preprocessAmpereValue47900(new Value<Integer>(null, 1234), MAX_DC_CURRENT));

		assertEquals(0,
				GoodWeBatteryInverterImpl.preprocessAmpereValue47900(new Value<Integer>(null, -25), MAX_DC_CURRENT));

		assertEquals(12,
				GoodWeBatteryInverterImpl.preprocessAmpereValue47900(new Value<Integer>(null, 12), MAX_DC_CURRENT));

		assertEquals(0,
				GoodWeBatteryInverterImpl.preprocessAmpereValue47900(new Value<Integer>(null, null), MAX_DC_CURRENT));
	}
}
