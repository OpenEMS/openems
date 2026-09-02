package io.openems.edge.ess.generic.common.essprotection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.batteryinverter.test.DummyManagedSymmetricBatteryInverter;
import io.openems.edge.common.startstop.StartStop;

public class EpRampHandlerTest {

	private EpRampHandler handler;
	private DummyBattery battery;
	private DummyManagedSymmetricBatteryInverter inverter;

	@BeforeEach
	public void setup() {
		this.handler = new EpRampHandler();

		this.battery = new DummyBattery("battery0") //
				.withStartStop(StartStop.START) //
				.withVoltage(957) //
				.withCurrent(-55) //
				.withChargeMaxVoltage(975) //
				.withDischargeMinVoltage(600) //
				.withInnerResistance(350); // [mOhm]

		this.inverter = new DummyManagedSymmetricBatteryInverter("inverter0") //
				.withDcMinVoltage(650) //
				.withDcMaxVoltage(1315);

		// Missing charge max current.
		var result = this.handler.calculateEssProtectionLimits(this.battery, this.inverter);
		assertNotNull(result, "Result should not be null");
		assertNull(result.chargeMaxCurrent(), "Charge max current should be set");

		this.battery //
				.withVoltage(957) //
				.withDischargeMaxCurrent(40) // [A]
				.withChargeMaxCurrent(40); // [A]
	}

	@Test
	public void testChargeAtOptimalVoltage() {
		this.battery //
				.withVoltage(957) //
				.withDischargeMaxCurrent(40) // [A]
				.withChargeMaxCurrent(40); // [A]

		var result = this.handler.calculateEssProtectionLimits(this.battery, this.inverter);

		assertNotNull(result, "Result should not be null");
		assertNotNull(result.chargeMaxCurrent(), "Charge max current should be set");

		// Distance: 1315V - 957V = 358V → 100% ramp
		// Expected: 40A × 1.0 = 40A
		assertEquals(40, (int) result.chargeMaxCurrent()); // At 358V distance, ramp should be 100%
	}

	@Test
	public void testChargeCriticalZone() {
		this.battery //
				.withVoltage(1310) // Inverter max voltage: 1315V
				.withChargeMaxCurrent(40);

		var result = this.handler.calculateEssProtectionLimits(this.battery, this.inverter);

		assertEquals(20, result.chargeMaxCurrent().intValue()); // At 5V distance, ramp should be 50%
	}

	@Test
	public void testChargeAtMaximumVoltage() {
		this.battery //
				.withVoltage(1315) //
				.withChargeMaxCurrent(40);

		var result = this.handler.calculateEssProtectionLimits(this.battery, this.inverter);

		assertEquals(0, result.chargeMaxCurrent().intValue()); // At 0V distance, ramp should be 0%
	}

	@Test
	public void testChargeHysteresisStaysLimited() {
		// Enter ACTIVE_LIMIT — distance 5V → factor 50%
		this.battery.withVoltage(1310).withChargeMaxCurrent(40); // distance = 1315 - 1310 = 5V
		var result = this.handler.calculateEssProtectionLimits(this.battery, this.inverter);
		assertEquals(20, result.chargeMaxCurrent().intValue());

		// Voltage recovers slightly — distance 12V (in hysteresis band 10–13V)
		// → still ACTIVE_LIMIT, factor held at 50%
		this.battery.withVoltage(1303).withChargeMaxCurrent(40); // distance = 1315 - 1303 = 12V
		result = this.handler.calculateEssProtectionLimits(this.battery, this.inverter);
		assertEquals(20, result.chargeMaxCurrent().intValue());

		// Voltage recovers past release threshold — distance 13V → NO_LIMIT,
		// full current
		this.battery.withVoltage(1302).withChargeMaxCurrent(40); // distance = 1315 - 1302 = 13V
		result = this.handler.calculateEssProtectionLimits(this.battery, this.inverter);
		assertEquals(40, result.chargeMaxCurrent().intValue());
	}

	@Test
	public void testChargeHysteresisRatchetsDown() {
		// Enter ACTIVE_LIMIT — distance 8V → factor 80%
		this.battery.withVoltage(1307).withChargeMaxCurrent(40); // distance = 8V
		var result = this.handler.calculateEssProtectionLimits(this.battery, this.inverter);
		assertEquals(32, result.chargeMaxCurrent().intValue());

		// Voltage moves closer — distance 6V → factor drops to 60%
		this.battery.withVoltage(1309).withChargeMaxCurrent(40); // distance = 6V
		result = this.handler.calculateEssProtectionLimits(this.battery, this.inverter);
		assertEquals(24, result.chargeMaxCurrent().intValue());

		// Voltage recovers slightly — distance 7V → ramp says 70%, but ratchet
		// holds at 60% (min of 60% and 70%)
		this.battery.withVoltage(1308).withChargeMaxCurrent(40); // distance = 7V
		result = this.handler.calculateEssProtectionLimits(this.battery, this.inverter);
		assertEquals(24, result.chargeMaxCurrent().intValue());
	}

	@Test
	public void testDischargeHysteresisStaysLimited() {
		// Enter ACTIVE_LIMIT — distance 5V → factor 50%
		this.battery.withVoltage(655).withDischargeMaxCurrent(40); // distance = 655 - 650 = 5V
		var result = this.handler.calculateEssProtectionLimits(this.battery, this.inverter);
		assertEquals(20, result.dischargeMaxCurrent().intValue());

		// Voltage recovers slightly — distance 11V (in hysteresis band 10–13V)
		// → still ACTIVE_LIMIT, factor held at 50%
		this.battery.withVoltage(661).withDischargeMaxCurrent(40); // distance = 661 - 650 = 11V
		result = this.handler.calculateEssProtectionLimits(this.battery, this.inverter);
		assertEquals(20, result.dischargeMaxCurrent().intValue());

		// Voltage past release threshold — distance 13V → NO_LIMIT
		this.battery.withVoltage(663).withDischargeMaxCurrent(40); // distance = 663 - 650 = 13V
		result = this.handler.calculateEssProtectionLimits(this.battery, this.inverter);
		assertEquals(40, result.dischargeMaxCurrent().intValue());
	}

	record DischargeCase(int voltage, int expectedDischargeMaxCurrent) {
	}

	@Test
	public void testDischargeLowerLimitParameterized() {
		var cases = List.of(new DischargeCase(660, 40), // 10V distance -> 100%
				new DischargeCase(655, 20), // 5V distance -> 50%
				new DischargeCase(650, 0) // 0V distance -> 0%
		);

		for (var testCase : cases) {
			this.battery //
					.withVoltage(testCase.voltage()) //
					.withDischargeMaxCurrent(40);

			var result = this.handler.calculateEssProtectionLimits(this.battery, this.inverter);

			assertNotNull(result);
			assertEquals(testCase.expectedDischargeMaxCurrent(), result.dischargeMaxCurrent().intValue());
		}
	}
}
