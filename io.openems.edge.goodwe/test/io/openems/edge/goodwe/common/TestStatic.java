package io.openems.edge.goodwe.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.openems.edge.goodwe.common.enums.GoodWeType;

public class TestStatic {

	@Test
	public void testGetHardwareTypeFromSerialNr() {
		assertEquals(GoodWeType.FENECON_FHI_10_DAH, AbstractGoodWe.getGoodWeTypeFromSerialNr("7010KETU22AW0901"));
		assertNotEquals(GoodWeType.FENECON_FHI_10_DAH, AbstractGoodWe.getGoodWeTypeFromSerialNr("70000KETU22AW090"));

		assertEquals(GoodWeType.FENECON_FHI_20_DAH, AbstractGoodWe.getGoodWeTypeFromSerialNr("9020KETT22AW0004"));
		assertNotEquals(GoodWeType.FENECON_FHI_20_DAH, AbstractGoodWe.getGoodWeTypeFromSerialNr("9010KETT22AW0004"));

		assertEquals(GoodWeType.FENECON_FHI_29_9_DAH, AbstractGoodWe.getGoodWeTypeFromSerialNr("9030KETT228W0004"));
		assertNotEquals(GoodWeType.FENECON_FHI_29_9_DAH, AbstractGoodWe.getGoodWeTypeFromSerialNr("9020KETT228W0004"));
		assertEquals(GoodWeType.FENECON_FHI_29_9_DAH, AbstractGoodWe.getGoodWeTypeFromSerialNr("929K9ETT231W0159"));
		assertNotEquals(GoodWeType.FENECON_FHI_29_9_DAH, AbstractGoodWe.getGoodWeTypeFromSerialNr("929KETT231W0159"));
		assertNotEquals(GoodWeType.FENECON_FHI_29_9_DAH, AbstractGoodWe.getGoodWeTypeFromSerialNr("928K9ETT231W0159"));
		assertEquals(GoodWeType.FENECON_FHI_29_9_DAH, AbstractGoodWe.getGoodWeTypeFromSerialNr("929K9ETT231W0160"));

		assertEquals(GoodWeType.UNDEFINED, AbstractGoodWe.getGoodWeTypeFromSerialNr("9040KETT228W0004"));
		assertEquals(GoodWeType.UNDEFINED, AbstractGoodWe.getGoodWeTypeFromSerialNr("9000KETT228W0004"));
		assertEquals(GoodWeType.UNDEFINED, AbstractGoodWe.getGoodWeTypeFromSerialNr("ET2"));
		assertEquals(GoodWeType.UNDEFINED, AbstractGoodWe.getGoodWeTypeFromSerialNr(""));
		assertEquals(GoodWeType.UNDEFINED, AbstractGoodWe.getGoodWeTypeFromSerialNr(null));
	}

	@Test
	public void testGetHardwareTypeFromGoodWeString() {
		assertEquals(GoodWeType.GOODWE_10K_BT, AbstractGoodWe.getGoodWeTypeFromStringValue("GW10K-BT"));
		assertEquals(GoodWeType.GOODWE_10K_ET, AbstractGoodWe.getGoodWeTypeFromStringValue("GW10K-ET"));
		assertEquals(GoodWeType.GOODWE_5K_BT, AbstractGoodWe.getGoodWeTypeFromStringValue("GW5K-BT"));
		assertEquals(GoodWeType.GOODWE_5K_ET, AbstractGoodWe.getGoodWeTypeFromStringValue("GW5K-ET"));
		assertEquals(GoodWeType.GOODWE_8K_BT, AbstractGoodWe.getGoodWeTypeFromStringValue("GW8K-BT"));
		assertEquals(GoodWeType.GOODWE_8K_ET, AbstractGoodWe.getGoodWeTypeFromStringValue("GW8K-ET"));
		assertEquals(GoodWeType.FENECON_FHI_10_DAH, AbstractGoodWe.getGoodWeTypeFromStringValue("FHI-10-DAH"));
		assertEquals(GoodWeType.UNDEFINED, AbstractGoodWe.getGoodWeTypeFromStringValue("ET2"));
		assertEquals(GoodWeType.UNDEFINED, AbstractGoodWe.getGoodWeTypeFromStringValue(""));
		assertEquals(GoodWeType.UNDEFINED, AbstractGoodWe.getGoodWeTypeFromStringValue(null));
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

	@Test
	public void testPostprocessPBattery1() {

		AtomicBoolean stateResult = new AtomicBoolean();
		Optional<Integer> prevPBattery = Optional.of(5000);

		// Max DC Power: 5750W
		var pBattery = 200_000;
		var dcVoltage = 230;
		var dcMaxCurrent = 25;

		assertEquals(prevPBattery.get(), AbstractGoodWe.postprocessPBattery1(pBattery, dcVoltage, dcMaxCurrent,
				state -> stateResult.set(state), prevPBattery)); //
		assertTrue(stateResult.get());

		pBattery = -100_000;
		assertEquals(prevPBattery.get(), AbstractGoodWe.postprocessPBattery1(pBattery, dcVoltage, dcMaxCurrent,
				state -> stateResult.set(state), prevPBattery)); //
		assertTrue(stateResult.get());

		pBattery = 4000;
		assertEquals(4000, (int) AbstractGoodWe.postprocessPBattery1(pBattery, dcVoltage, dcMaxCurrent,
				state -> stateResult.set(state), prevPBattery)); //
		assertFalse(stateResult.get());

		pBattery = -4000;
		assertEquals(-4000, (int) AbstractGoodWe.postprocessPBattery1(pBattery, dcVoltage, dcMaxCurrent,
				state -> stateResult.set(state), prevPBattery)); //
		assertFalse(stateResult.get());

		/*
		 * One of the given values is null
		 */
		assertEquals(-100_000, (int) AbstractGoodWe.postprocessPBattery1(-100_000, null, dcMaxCurrent,
				state -> stateResult.set(state), prevPBattery)); //
		assertFalse(stateResult.get());

		assertEquals(-100_000, (int) AbstractGoodWe.postprocessPBattery1(-100_000, dcVoltage, null,
				state -> stateResult.set(state), prevPBattery)); //
		assertFalse(stateResult.get());

		Integer pBatteryNull = null;
		assertEquals(null, AbstractGoodWe.postprocessPBattery1(pBatteryNull, dcVoltage, dcMaxCurrent,
				state -> stateResult.set(state), prevPBattery)); //
		assertFalse(stateResult.get());

		/*
		 * Previous value was null
		 */
		prevPBattery = Optional.empty();
		pBattery = 200_000;
		assertEquals(5750, (int) AbstractGoodWe.postprocessPBattery1(pBattery, dcVoltage, dcMaxCurrent,
				state -> stateResult.set(state), prevPBattery)); //
		assertTrue(stateResult.get());

		pBattery = -100_000;
		assertEquals(-5750, (int) AbstractGoodWe.postprocessPBattery1(pBattery, dcVoltage, dcMaxCurrent,
				state -> stateResult.set(state), prevPBattery)); //
		assertTrue(stateResult.get());
	}
}
