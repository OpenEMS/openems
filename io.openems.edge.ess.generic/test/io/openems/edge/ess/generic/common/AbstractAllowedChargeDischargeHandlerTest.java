package io.openems.edge.ess.generic.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import io.openems.common.test.TestUtils;
import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.batteryinverter.test.DummyManagedSymmetricBatteryInverter;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.ess.generic.common.essprotection.EpVoltageRegulationHandler;
import io.openems.edge.ess.generic.common.essprotection.EssProtectionHandler.EssProtectionLimits;

public class AbstractAllowedChargeDischargeHandlerTest {

	@Test
	public void testCalculateMaxCurrent() {
		final var clock = TestUtils.createDummyClock();
		final var battery = new DummyBattery("batter0");
		final var batteryInverter = new DummyManagedSymmetricBatteryInverter("batteryInverter0");
		final var handler = new EpVoltageRegulationHandler(clock);

		Supplier<EssProtectionLimits> limits = () -> {
			clock.leap(500, ChronoUnit.MILLIS);
			return handler.calculateEssProtectionLimits(battery, batteryInverter);
		};

		// Without data
		assertNull(limits.get().chargeMaxCurrent());
		assertNull(limits.get().dischargeMaxCurrent());

		battery //
				.withStartStop(StartStop.START) //
				.withVoltage(957) //
				.withCurrent(-55) //
				.withChargeMaxVoltage(975) //
				.withDischargeMinVoltage(770) //
				.withInnerResistance(350); // [mOhm]
		batteryInverter //
				.withDcMinVoltage(650) //
				.withDcMaxVoltage(1315);

		// Initialize PT1 filter
		for (var i = 0; i < 100; i++) {
			limits.get();
		}

		battery //
				.withCurrent(-50);
		for (var i = 0; i < 20; i++) {
			limits.get();
		}

		assertEquals(103, limits.get().chargeMaxCurrent().intValue());

		battery //
				.withCurrent(-45);
		for (var i = 0; i < 20; i++) {
			limits.get();
		}
		assertEquals(99, limits.get().chargeMaxCurrent().intValue());

		battery //
				.withCurrent(-40);
		for (var i = 0; i < 20; i++) {
			limits.get();
		}
		assertEquals(94, limits.get().chargeMaxCurrent().intValue());
	}
}
