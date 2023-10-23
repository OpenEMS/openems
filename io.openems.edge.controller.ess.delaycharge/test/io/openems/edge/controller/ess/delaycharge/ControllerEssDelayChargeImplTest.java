package io.openems.edge.controller.ess.delaycharge;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class ControllerEssDelayChargeImplTest {

	private static final String CTRL_ID = "ctrl0";
	private static final ChannelAddress CTRL_CHARGE_POWER_LIMIT = new ChannelAddress(CTRL_ID, "ChargePowerLimit");

	private static final String ESS_ID = "ess0";

	@Test
	public void test() throws Exception {
		// Initialize mocked Clock
		final var clock = new TimeLeapClock(
				Instant.ofEpochMilli(1546300800000L /* Tuesday, 1. January 2019 00:00:00 */), ZoneId.of("UTC"));
		new ControllerTest(new ControllerEssDelayChargeImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addComponent(new DummyManagedSymmetricEss(ESS_ID) //
						.withSoc(20) //
						.withCapacity(9000)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setTargetHour(15) //
						.build())
				.next(new TestCase() //
						.timeleap(clock, 6, ChronoUnit.HOURS) // = 6 am
						.output(CTRL_CHARGE_POWER_LIMIT, 800))
				.next(new TestCase() //
						.timeleap(clock, 2, ChronoUnit.HOURS) // = 8 am
						.output(CTRL_CHARGE_POWER_LIMIT, 1028))
				.next(new TestCase() //
						.timeleap(clock, 2, ChronoUnit.HOURS) // = 10 am
						.output(CTRL_CHARGE_POWER_LIMIT, 1440))
				.next(new TestCase() //
						.timeleap(clock, 2, ChronoUnit.HOURS) // = 12 am
						.output(CTRL_CHARGE_POWER_LIMIT, 2400))
				.next(new TestCase() //
						.timeleap(clock, 2, ChronoUnit.HOURS) // = 14 am
						.output(CTRL_CHARGE_POWER_LIMIT, 7200))
				.next(new TestCase() //
						.timeleap(clock, 3, ChronoUnit.HOURS) // = 16 am
						.output(CTRL_CHARGE_POWER_LIMIT, 0));
	}

}
