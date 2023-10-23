package io.openems.edge.goodwe.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.edge.goodwe.common.enums.GoodWeHardwareType;

public class TestStatic {

	@Test
	public void testGetHardwareTypeFromSerialNr() {
		assertEquals(GoodWeHardwareType.GOODWE_10, AbstractGoodWe.getHardwareTypeFromSerialNr("7010KETU22AW0901"));
		assertNotEquals(GoodWeHardwareType.GOODWE_10, AbstractGoodWe.getHardwareTypeFromSerialNr("70000KETU22AW090"));

		assertEquals(GoodWeHardwareType.GOODWE_20, AbstractGoodWe.getHardwareTypeFromSerialNr("9020KETT22AW0004"));
		assertNotEquals(GoodWeHardwareType.GOODWE_20, AbstractGoodWe.getHardwareTypeFromSerialNr("9010KETT22AW0004"));

		assertEquals(GoodWeHardwareType.GOODWE_29_9, AbstractGoodWe.getHardwareTypeFromSerialNr("9030KETT228W0004"));
		assertNotEquals(GoodWeHardwareType.GOODWE_29_9, AbstractGoodWe.getHardwareTypeFromSerialNr("9020KETT228W0004"));
		assertEquals(GoodWeHardwareType.GOODWE_29_9, AbstractGoodWe.getHardwareTypeFromSerialNr("929K9ETT231W0159"));
		assertNotEquals(GoodWeHardwareType.GOODWE_29_9, AbstractGoodWe.getHardwareTypeFromSerialNr("929KETT231W0159"));
		assertNotEquals(GoodWeHardwareType.GOODWE_29_9, AbstractGoodWe.getHardwareTypeFromSerialNr("928K9ETT231W0159"));

		assertEquals(GoodWeHardwareType.OTHER, AbstractGoodWe.getHardwareTypeFromSerialNr("9040KETT228W0004"));
		assertEquals(GoodWeHardwareType.OTHER, AbstractGoodWe.getHardwareTypeFromSerialNr("9000KETT228W0004"));
		assertEquals(GoodWeHardwareType.OTHER, AbstractGoodWe.getHardwareTypeFromSerialNr("ET2"));
		assertEquals(GoodWeHardwareType.UNDEFINED, AbstractGoodWe.getHardwareTypeFromSerialNr(""));
	}

	@Test
	public void testDetectActiveDiagStatesH() {
		// 0x00000001 DIAG_STATUS_H_BATTERY_PRECHARGE_RELAY_OFF
		// 0x00000002 DIAG_STATUS_H_BYPASS_RELAY_STICK
		// 0x10000000 DIAG_STATUS_H_METER_VOLTAGE_SAMPLE_FAULT
		// 0x20000000 DIAG_STATUS_H_EXTERNAL_STOP_MODE_ENABLE
		// 0x40000000 DIAG_STATUS_H_BATTERY_OFFGRID_DOD
		// 0x80000000 DIAG_STATUS_H_BATTERY_SOC_ADJUST_ENABLE

		Long value = 0xC0000001L;
		assertTrue(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_BATTERY_SOC_ADJUST_ENABLE));
		assertTrue(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_BATTERY_OFFGRID_DOD));
		assertTrue(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_BATTERY_PRECHARGE_RELAY_OFF));
		assertFalse(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_BYPASS_RELAY_STICK));
		assertFalse(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_METER_VOLTAGE_SAMPLE_FAULT));
		assertFalse(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_EXTERNAL_STOP_MODE_ENABLE));

		value = 0xC0005701L;
		assertTrue(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_BATTERY_SOC_ADJUST_ENABLE));
		assertTrue(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_BATTERY_OFFGRID_DOD));
		assertTrue(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_BATTERY_PRECHARGE_RELAY_OFF));
		assertFalse(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_BYPASS_RELAY_STICK));
		assertFalse(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_METER_VOLTAGE_SAMPLE_FAULT));
		assertFalse(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_EXTERNAL_STOP_MODE_ENABLE));

		value = 0x90000003L;
		assertTrue(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_BATTERY_SOC_ADJUST_ENABLE));
		assertFalse(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_BATTERY_OFFGRID_DOD));
		assertTrue(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_BATTERY_PRECHARGE_RELAY_OFF));
		assertTrue(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_BYPASS_RELAY_STICK));
		assertTrue(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_METER_VOLTAGE_SAMPLE_FAULT));
		assertFalse(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_EXTERNAL_STOP_MODE_ENABLE));

		value = 3221225473L;
		assertTrue(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_BATTERY_SOC_ADJUST_ENABLE));
		assertTrue(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_BATTERY_OFFGRID_DOD));
		assertTrue(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_BATTERY_PRECHARGE_RELAY_OFF));
		assertFalse(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_BYPASS_RELAY_STICK));
		assertFalse(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_METER_VOLTAGE_SAMPLE_FAULT));
		assertFalse(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_EXTERNAL_STOP_MODE_ENABLE));

		value = null;
		assertFalse(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_BATTERY_SOC_ADJUST_ENABLE));
		assertFalse(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_BATTERY_OFFGRID_DOD));
		assertFalse(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_BATTERY_PRECHARGE_RELAY_OFF));
		assertFalse(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_BYPASS_RELAY_STICK));
		assertFalse(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_METER_VOLTAGE_SAMPLE_FAULT));
		assertFalse(AbstractGoodWe.detectDiagStatesH(value) //
				.get(GoodWe.ChannelId.DIAG_STATUS_EXTERNAL_STOP_MODE_ENABLE));
	}
}
