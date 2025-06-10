package io.openems.edge.controller.ess.delaycharge;

import static io.openems.edge.controller.ess.delaycharge.ControllerEssDelayCharge.ChannelId.CHARGE_POWER_LIMIT;
import static java.time.temporal.ChronoUnit.HOURS;

import java.time.Instant;
import java.time.ZoneId;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class ControllerEssDelayChargeImplTest {

	@Test
	public void test() throws Exception {
		// Initialize mocked Clock
		final var clock = new TimeLeapClock(
				Instant.ofEpochMilli(1546300800000L /* Tuesday, 1. January 2019 00:00:00 */), ZoneId.of("UTC"));
		new ControllerTest(new ControllerEssDelayChargeImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addComponent(new DummyManagedSymmetricEss("ess0") //
						.withSoc(20) //
						.withCapacity(9000)) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setTargetHour(15) //
						.build())
				.next(new TestCase() //
						.timeleap(clock, 6, HOURS) // = 6 am
						.output(CHARGE_POWER_LIMIT, 800))
				.next(new TestCase() //
						.timeleap(clock, 2, HOURS) // = 8 am
						.output(CHARGE_POWER_LIMIT, 1028))
				.next(new TestCase() //
						.timeleap(clock, 2, HOURS) // = 10 am
						.output(CHARGE_POWER_LIMIT, 1440))
				.next(new TestCase() //
						.timeleap(clock, 2, HOURS) // = 12 am
						.output(CHARGE_POWER_LIMIT, 2400))
				.next(new TestCase() //
						.timeleap(clock, 2, HOURS) // = 14 am
						.output(CHARGE_POWER_LIMIT, 7200))
				.next(new TestCase() //
						.timeleap(clock, 3, HOURS) // = 16 am
						.output(CHARGE_POWER_LIMIT, 0)) //
				.deactivate();
	}
}
