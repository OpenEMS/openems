package io.openems.edge.goodwe.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TestStatic {

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
