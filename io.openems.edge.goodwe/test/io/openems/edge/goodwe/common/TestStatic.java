package io.openems.edge.goodwe.common;

import static io.openems.edge.battery.fenecon.home.BatteryFeneconHomeHardwareType.BATTERY_52;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHomeHardwareType.BATTERY_64;
import static io.openems.edge.goodwe.common.enums.GoodWeType.FENECON_50K;
import static io.openems.edge.goodwe.common.enums.GoodWeType.FENECON_FHI_10_DAH;
import static io.openems.edge.goodwe.common.enums.GoodWeType.FENECON_FHI_20_DAH;
import static io.openems.edge.goodwe.common.enums.GoodWeType.FENECON_FHI_29_9_DAH;
import static io.openems.edge.goodwe.common.enums.GoodWeType.FENECON_GEN2_10K;
import static io.openems.edge.goodwe.common.enums.GoodWeType.FENECON_GEN2_15K;
import static io.openems.edge.goodwe.common.enums.GoodWeType.FENECON_GEN2_6K;
import static io.openems.edge.goodwe.common.enums.GoodWeType.GOODWE_10K_BT;
import static io.openems.edge.goodwe.common.enums.GoodWeType.GOODWE_10K_ET;
import static io.openems.edge.goodwe.common.enums.GoodWeType.GOODWE_5K_BT;
import static io.openems.edge.goodwe.common.enums.GoodWeType.GOODWE_5K_ET;
import static io.openems.edge.goodwe.common.enums.GoodWeType.GOODWE_8K_BT;
import static io.openems.edge.goodwe.common.enums.GoodWeType.GOODWE_8K_ET;
import static io.openems.edge.goodwe.common.enums.GoodWeType.UNDEFINED;
import static io.openems.edge.goodwe.common.enums.GoodWeType.authorisedLimit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import io.openems.edge.goodwe.common.AbstractGoodWe.MaxAcPower;
import io.openems.edge.goodwe.common.enums.EmsPowerMode;

public class TestStatic {

	@Test
	public void testGetHardwareTypeFromSerialNr() {
		assertEquals(FENECON_FHI_10_DAH, AbstractGoodWe.getGoodWeTypeFromSerialNr("7010KETU22AW0901"));
		assertNotEquals(FENECON_FHI_10_DAH, AbstractGoodWe.getGoodWeTypeFromSerialNr("70000KETU22AW090"));

		assertEquals(FENECON_FHI_20_DAH, AbstractGoodWe.getGoodWeTypeFromSerialNr("9020KETT22AW0004"));
		assertNotEquals(FENECON_FHI_20_DAH, AbstractGoodWe.getGoodWeTypeFromSerialNr("9010KETT22AW0004"));

		assertEquals(FENECON_FHI_29_9_DAH, AbstractGoodWe.getGoodWeTypeFromSerialNr("9030KETT228W0004"));
		assertEquals(FENECON_FHI_29_9_DAH, AbstractGoodWe.getGoodWeTypeFromSerialNr("129K9ETT231W0159"));
		assertNotEquals(FENECON_FHI_29_9_DAH, AbstractGoodWe.getGoodWeTypeFromSerialNr("9020KETT228W0004"));
		assertNotEquals(FENECON_FHI_29_9_DAH, AbstractGoodWe.getGoodWeTypeFromSerialNr("929KETT231W0159"));
		assertNotEquals(FENECON_FHI_29_9_DAH, AbstractGoodWe.getGoodWeTypeFromSerialNr("928K9ETT231W0159"));

		assertEquals(FENECON_GEN2_6K, AbstractGoodWe.getGoodWeTypeFromSerialNr("96000EUB246L0002"));
		assertEquals(FENECON_GEN2_10K, AbstractGoodWe.getGoodWeTypeFromSerialNr("9010KEUB246L0001"));
		assertEquals(FENECON_GEN2_15K, AbstractGoodWe.getGoodWeTypeFromSerialNr("9015KEUB246L0003"));

		assertEquals(FENECON_50K, AbstractGoodWe.getGoodWeTypeFromSerialNr("9050KETF241W8012"));

		assertEquals(UNDEFINED, AbstractGoodWe.getGoodWeTypeFromSerialNr("9050KETT241W8012"));
		assertEquals(UNDEFINED, AbstractGoodWe.getGoodWeTypeFromSerialNr("9055KETF241W8012"));
		assertEquals(UNDEFINED, AbstractGoodWe.getGoodWeTypeFromSerialNr("9040KETT228W0004"));
		assertEquals(UNDEFINED, AbstractGoodWe.getGoodWeTypeFromSerialNr("9000KETT228W0004"));
		assertEquals(UNDEFINED, AbstractGoodWe.getGoodWeTypeFromSerialNr("ET2"));
		assertEquals(UNDEFINED, AbstractGoodWe.getGoodWeTypeFromSerialNr(""));
		assertEquals(UNDEFINED, AbstractGoodWe.getGoodWeTypeFromSerialNr(null));
	}

	@Test
	public void testAuthorisedLimit() {
		assertEquals(25, authorisedLimit(40, 25, 40).apply(BATTERY_52).intValue());
		assertEquals(40, authorisedLimit(40, 25, 40).apply(BATTERY_64).intValue());
		assertEquals(40, authorisedLimit(40, 25, 40).apply(null).intValue());

		assertEquals(25, FENECON_GEN2_6K.maxDcCurrent.apply(BATTERY_52).intValue());
		assertEquals(40, FENECON_GEN2_6K.maxDcCurrent.apply(BATTERY_64).intValue());
		assertEquals(25, FENECON_GEN2_10K.maxDcCurrent.apply(BATTERY_52).intValue());
		assertEquals(40, FENECON_GEN2_10K.maxDcCurrent.apply(BATTERY_64).intValue());
		assertEquals(25, FENECON_GEN2_15K.maxDcCurrent.apply(BATTERY_52).intValue());
		assertEquals(40, FENECON_GEN2_15K.maxDcCurrent.apply(BATTERY_64).intValue());
		assertEquals(25, FENECON_FHI_10_DAH.maxDcCurrent.apply(BATTERY_52).intValue());
		assertEquals(0, FENECON_FHI_10_DAH.maxDcCurrent.apply(BATTERY_64).intValue());
		assertEquals(0, FENECON_FHI_20_DAH.maxDcCurrent.apply(BATTERY_52).intValue());
		assertEquals(50, FENECON_FHI_20_DAH.maxDcCurrent.apply(BATTERY_64).intValue());
		assertEquals(0, FENECON_FHI_29_9_DAH.maxDcCurrent.apply(BATTERY_52).intValue());
		assertEquals(50, FENECON_FHI_29_9_DAH.maxDcCurrent.apply(BATTERY_64).intValue());
		assertEquals(25, GOODWE_8K_ET.maxDcCurrent.apply(BATTERY_52).intValue());
		assertEquals(25, GOODWE_8K_ET.maxDcCurrent.apply(BATTERY_64).intValue());
		assertEquals(25, GOODWE_8K_ET.maxDcCurrent.apply(null).intValue());

	}

	@Test
	public void testGetHardwareTypeFromGoodWeString() {
		assertEquals(GOODWE_10K_BT, AbstractGoodWe.getGoodWeTypeFromStringValue("GW10K-BT"));
		assertEquals(GOODWE_10K_ET, AbstractGoodWe.getGoodWeTypeFromStringValue("GW10K-ET"));
		assertEquals(GOODWE_5K_BT, AbstractGoodWe.getGoodWeTypeFromStringValue("GW5K-BT"));
		assertEquals(GOODWE_5K_ET, AbstractGoodWe.getGoodWeTypeFromStringValue("GW5K-ET"));
		assertEquals(GOODWE_8K_BT, AbstractGoodWe.getGoodWeTypeFromStringValue("GW8K-BT"));
		assertEquals(GOODWE_8K_ET, AbstractGoodWe.getGoodWeTypeFromStringValue("GW8K-ET"));
		assertEquals(FENECON_FHI_10_DAH, AbstractGoodWe.getGoodWeTypeFromStringValue("FHI-10-DAH"));
		assertEquals(UNDEFINED, AbstractGoodWe.getGoodWeTypeFromStringValue("ET2"));
		assertEquals(UNDEFINED, AbstractGoodWe.getGoodWeTypeFromStringValue(""));
		assertEquals(UNDEFINED, AbstractGoodWe.getGoodWeTypeFromStringValue(null));
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

	@Test
	public void testIgnoreImpossibleMinimumPower() {

		var dcPower = 200_000; // W
		var powerMode = EmsPowerMode.AUTO;
		var powerSet = 0;

		assertEquals(dcPower, (int) AbstractGoodWe.ignoreImpossibleMinPower(dcPower, 50 /* SoC */,
				25 /* batteryCurrent */, powerMode, powerSet));

		dcPower = 10;
		assertEquals(dcPower, (int) AbstractGoodWe.ignoreImpossibleMinPower(dcPower, 50, 25, powerMode, powerSet));

		dcPower = 10;
		assertEquals(dcPower, (int) AbstractGoodWe.ignoreImpossibleMinPower(dcPower, 0, 2, powerMode, powerSet));
		assertEquals(dcPower, (int) AbstractGoodWe.ignoreImpossibleMinPower(dcPower, 2, 0, powerMode, powerSet));
		assertEquals(dcPower,
				(int) AbstractGoodWe.ignoreImpossibleMinPower(dcPower, 95, 0, EmsPowerMode.CHARGE_BAT, 1000));

		assertEquals(dcPower, (int) AbstractGoodWe.ignoreImpossibleMinPower(dcPower, null, 0, powerMode, powerSet));
		assertEquals(dcPower, (int) AbstractGoodWe.ignoreImpossibleMinPower(dcPower, 0, null, powerMode, powerSet));
		assertEquals(dcPower, (int) AbstractGoodWe.ignoreImpossibleMinPower(dcPower, 0, 0, null, powerSet));
		assertEquals(dcPower, (int) AbstractGoodWe.ignoreImpossibleMinPower(dcPower, 0, 0, powerMode, null));
		assertNull(AbstractGoodWe.ignoreImpossibleMinPower(null, null, null, null, null));

		/*
		 * Ignore impossible value
		 */
		assertEquals(0, (int) AbstractGoodWe.ignoreImpossibleMinPower(dcPower, 0, 0, powerMode, powerSet));
		assertEquals(0, (int) AbstractGoodWe.ignoreImpossibleMinPower(dcPower, 100, 0, powerMode, powerSet));
		assertEquals(0,
				(int) AbstractGoodWe.ignoreImpossibleMinPower(dcPower, 95, 0, EmsPowerMode.CHARGE_BAT, powerSet));
		assertEquals(0,
				(int) AbstractGoodWe.ignoreImpossibleMinPower(dcPower, 95, 0, EmsPowerMode.DISCHARGE_BAT, powerSet));
	}

	@Test
	public void testCalculateDcLimitation() {
		assertEquals(0, (int) AbstractGoodWe.calculateDcLimitation(0, Optional.of(230), 30_000));
		assertEquals(11500, (int) AbstractGoodWe.calculateDcLimitation(50, Optional.of(230), 30_000));
		assertEquals(9000,
				(int) AbstractGoodWe.calculateDcLimitation(50, Optional.of(230), FENECON_GEN2_6K.maxBatChargeP));
		assertEquals(6600,
				(int) AbstractGoodWe.calculateDcLimitation(50, Optional.of(230), FENECON_GEN2_6K.maxBatDischargeP));
		assertEquals(0, (int) AbstractGoodWe.calculateDcLimitation(50, Optional.empty(), 30_000));
		assertEquals(20000, (int) AbstractGoodWe.calculateDcLimitation(50, Optional.of(400), 30_000));
		assertEquals(-800, (int) AbstractGoodWe.calculateDcLimitation(-2, Optional.of(400), 30_000));
		assertEquals(30_000, (int) AbstractGoodWe.calculateDcLimitation(null, Optional.of(400), 30_000));
	}

	@Test
	public void testCalculateMaxAcPower() {
		final var volt = Optional.of(400); // V

		// Limited by ApparentPower
		assertEquals(new MaxAcPower(-10_000, 10_000),
				AbstractGoodWe.calculateMaxAcPower(10_000, 50, 50, volt, 10_000, 10_000, 0));

		// Limited by inverter DC maximum
		assertEquals(new MaxAcPower(-9_000, 6_600), AbstractGoodWe.calculateMaxAcPower(10_000, 50, 50, volt,
				FENECON_GEN2_6K.maxBatChargeP, FENECON_GEN2_6K.maxBatDischargeP, 0));

		// Limited by BMS
		assertEquals(new MaxAcPower(-20_000, 20_000),
				AbstractGoodWe.calculateMaxAcPower(30_000, 50, 50, volt, 30_000, 30_000, 0));
		assertEquals(new MaxAcPower(-20_000, 10_000),
				AbstractGoodWe.calculateMaxAcPower(30_000, 50, 25, volt, 30_000, 30_000, 0));
		assertEquals(new MaxAcPower(-10_000, 20_000),
				AbstractGoodWe.calculateMaxAcPower(30_000, 25, 50, volt, 30_000, 30_000, 0));
		assertEquals(new MaxAcPower(800, 25_000),
				AbstractGoodWe.calculateMaxAcPower(30_000, -2 /* Force Discharge */, 50, volt, 30_000, 30_000, 5000));
		assertEquals(new MaxAcPower(-20_000, -800),
				AbstractGoodWe.calculateMaxAcPower(30_000, 50, -2 /* Force Charge */, volt, 30_000, 30_000, 0));

		// Limited by PV
		assertEquals(new MaxAcPower(0, 20_000 /* 10 kW battery + 10kW pv */),
				AbstractGoodWe.calculateMaxAcPower(30000, 50, 50, volt, 10_000, 10_000, 10_000 /* 10kW PV */));
		assertEquals(new MaxAcPower(-2000 /* 5kW - 3kW */, 8000 /* 5kW + 3kW */),
				AbstractGoodWe.calculateMaxAcPower(30000, 10, 10, Optional.of(500), 10_000, 10_000, 3000 /* 3kW PV */));

		assertEquals(new MaxAcPower(1000, 5000), // Positive MaxAcImport was not allowed before refactoring
				AbstractGoodWe.calculateMaxAcPower(5_000, -2 /* Force Discharge */, 50, Optional.of(500), 30_000,
						30_000, 3000));
	}
}
