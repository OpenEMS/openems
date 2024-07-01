package io.openems.edge.ess.generic.common;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.ess.generic.symmetric.AllowedChargeDischargeHandler;
import io.openems.edge.ess.generic.symmetric.EssGenericManagedSymmetricImpl;

public class AllowedChargeDischargeHandlerTest {

	@Test
	public void testStart() throws Exception {
		final var ess = new EssGenericManagedSymmetricImpl();
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final ClockProvider clockProvider = new DummyComponentManager(clock);
		new ComponentTest(ess) //
				.addReference("componentManager", clockProvider); //

		var sut = new AllowedChargeDischargeHandler(ess);

		sut.calculateAllowedChargeDischargePower(clockProvider, false, null, null, null);
		assertEquals(0, sut.lastBatteryAllowedChargePower, 0.001);
		assertEquals(0, sut.lastBatteryAllowedDischargePower, 0.001);
		clock.leap(1, ChronoUnit.SECONDS);

		sut.calculateAllowedChargeDischargePower(clockProvider, true, null, null, null);
		assertEquals(0, sut.lastBatteryAllowedChargePower, 0.001);
		assertEquals(0, sut.lastBatteryAllowedDischargePower, 0.001);
		clock.leap(1, ChronoUnit.SECONDS);

		sut.calculateAllowedChargeDischargePower(clockProvider, true, 9, -1, 500);
		assertEquals(225, sut.lastBatteryAllowedChargePower, 0.001);
		assertEquals(-475, sut.lastBatteryAllowedDischargePower, 0.001);

		clock.leap(250, ChronoUnit.MILLIS);
		sut.calculateAllowedChargeDischargePower(clockProvider, true, 9, -1, 500);
		clock.leap(250, ChronoUnit.MILLIS);
		sut.calculateAllowedChargeDischargePower(clockProvider, true, 9, -1, 500);
		assertEquals(-475, sut.lastBatteryAllowedDischargePower, 0.001);
		clock.leap(250, ChronoUnit.MILLIS);
		sut.calculateAllowedChargeDischargePower(clockProvider, true, 9, 0, 500);
		clock.leap(250, ChronoUnit.MILLIS);
		sut.calculateAllowedChargeDischargePower(clockProvider, true, 9, 0, 500);
		assertEquals(450, sut.lastBatteryAllowedChargePower, 0.001);
		assertEquals(0, sut.lastBatteryAllowedDischargePower, 0.001);

		clock.leap(1, ChronoUnit.SECONDS);
		sut.calculateAllowedChargeDischargePower(clockProvider, true, 9, 0, 500);
		assertEquals(675, sut.lastBatteryAllowedChargePower, 0.001);
		assertEquals(0, sut.lastBatteryAllowedDischargePower, 0.001);

		for (var i = 0; i < 15; i++) {
			clock.leap(1, ChronoUnit.SECONDS);
			sut.calculateAllowedChargeDischargePower(clockProvider, true, 9, 1, 500);
		}

		clock.leap(1, ChronoUnit.SECONDS);
		sut.calculateAllowedChargeDischargePower(clockProvider, true, 9, 1, 500);
		assertEquals(4275, sut.lastBatteryAllowedChargePower, 0.001);
		assertEquals(380, sut.lastBatteryAllowedDischargePower, 0.001);

		clock.leap(1, ChronoUnit.SECONDS);
		sut.calculateAllowedChargeDischargePower(clockProvider, true, 9, 1, 500);
		assertEquals(4500, sut.lastBatteryAllowedChargePower, 0.001);
		assertEquals(403.75, sut.lastBatteryAllowedDischargePower, 0.001);

		clock.leap(1, ChronoUnit.SECONDS);
		sut.calculateAllowedChargeDischargePower(clockProvider, true, 9, 2, 500);
		assertEquals(4500, sut.lastBatteryAllowedChargePower, 0.001);
		assertEquals(451.25, sut.lastBatteryAllowedDischargePower, 0.001);

		clock.leap(1, ChronoUnit.SECONDS);
		sut.calculateAllowedChargeDischargePower(clockProvider, true, 2, 0, 500);
		assertEquals(1000, sut.lastBatteryAllowedChargePower, 0.001);
		assertEquals(0, sut.lastBatteryAllowedDischargePower, 0.001);

		clock.leap(1, ChronoUnit.SECONDS);
		sut.calculateAllowedChargeDischargePower(clockProvider, true, 9, 9, 500);
		assertEquals(1225, sut.lastBatteryAllowedChargePower, 0.001);
		assertEquals(213.75, sut.lastBatteryAllowedDischargePower, 0.001);
	}

}
