package io.openems.edge.ess.generic.common;

import static io.openems.edge.ess.generic.common.AbstractAllowedChargeDischargeHandler.VOLTAGE_CONTROL_FILTER_TIME_CONSTANT;
import static io.openems.edge.ess.generic.common.AbstractAllowedChargeDischargeHandler.calculateMaxCurrent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

import org.junit.Test;

import io.openems.common.test.TestUtils;
import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.batteryinverter.test.DummyManagedSymmetricBatteryInverter;
import io.openems.edge.common.filter.PT1Filter;
import io.openems.edge.common.startstop.StartStop;

public class AbstractAllowedChargeDischargeHandlerTest {

	@Test
	public void testCalculateMaxCurrent() {
		final var clock = TestUtils.createDummyClock();
		final var battery = new DummyBattery("batter0");
		final var batteryInverter = new DummyManagedSymmetricBatteryInverter("batteryInverter0");
		final var pt1Filter = new PT1Filter(clock, VOLTAGE_CONTROL_FILTER_TIME_CONSTANT);
		Supplier<Integer> maxCurrent = () -> {
			clock.leap(500, ChronoUnit.MILLIS);
			return calculateMaxCurrent(battery, batteryInverter, pt1Filter, //
					Math::min, (a, b) -> a - b, true /* invert */);
		};

		// Without data
		assertNull(maxCurrent.get());

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
			maxCurrent.get();
		}

		battery //
				.withCurrent(-50);
		for (var i = 0; i < 20; i++) {
			maxCurrent.get();
		}
		assertEquals(103, maxCurrent.get().intValue());

		battery //
				.withCurrent(-45);
		for (var i = 0; i < 20; i++) {
			maxCurrent.get();
		}
		assertEquals(99, maxCurrent.get().intValue());

		battery //
				.withCurrent(-40);
		for (var i = 0; i < 20; i++) {
			maxCurrent.get();
		}
		assertEquals(94, maxCurrent.get().intValue());
	}

}
