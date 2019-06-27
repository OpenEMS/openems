package io.openems.edge.controller.ess.delaycharge;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class DelayChargeTest {

	@SuppressWarnings("all")
	private static class MyConfig extends AbstractComponentConfig implements Config {

		private final String essId;
		private final int targetHour;

		public MyConfig(String id, String essId, int targetHour) {
			super(Config.class, id);
			this.essId = essId;
			this.targetHour = targetHour;
		}

		@Override
		public String ess_id() {
			return this.essId;
		}

		@Override
		public int targetHour() {
			return this.targetHour;
		}
	}

	@Test
	public void test() throws Exception {
		// Initialize mocked Clock
		final TimeLeapClock clock = new TimeLeapClock(
				Instant.ofEpochMilli(1546300800000l /* Tuesday, 1. January 2019 00:00:00 */), ZoneId.of("UTC"));

		LocalDateTime now = LocalDateTime.now(clock);
		System.out.println(now);
		// Initialize Controller
		DelayChargeController controller = new DelayChargeController(clock);
		// Add referenced services
		DummyComponentManager componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;
		// Activate (twice, so that reference target is set)
		MyConfig config = new MyConfig("ctrl0", "ess0", 15);
		controller.activate(null, config);
		// Prepare Channels
		ChannelAddress ess0Soc = new ChannelAddress("ess0", "Soc");
		ChannelAddress ess0Capacity = new ChannelAddress("ess0", "Capacity");
		ChannelAddress ctrl0ChargePowerLimit = new ChannelAddress("ctrl0", "ChargePowerLimit");
		// Build and run test
		ManagedSymmetricEss ess = new DummyManagedSymmetricEss("ess0");
		new ControllerTest(controller, componentManager, ess, controller) //
				.next(new TestCase() //
						.timeleap(clock, 6, ChronoUnit.HOURS) // = 6 am
						.input(ess0Soc, 20) //
						.input(ess0Capacity, 9000) //
						.output(ctrl0ChargePowerLimit, 800))
				.next(new TestCase() //
						.timeleap(clock, 2, ChronoUnit.HOURS) // = 8 am
						.output(ctrl0ChargePowerLimit, 1028))
				.next(new TestCase() //
						.timeleap(clock, 2, ChronoUnit.HOURS) // = 10 am
						.output(ctrl0ChargePowerLimit, 1440))
				.next(new TestCase() //
						.timeleap(clock, 2, ChronoUnit.HOURS) // = 12 am
						.output(ctrl0ChargePowerLimit, 2400))
				.next(new TestCase() //
						.timeleap(clock, 2, ChronoUnit.HOURS) // = 14 am
						.output(ctrl0ChargePowerLimit, 7200))
				.next(new TestCase() //
						.timeleap(clock, 3, ChronoUnit.HOURS) // = 16 am
						.output(ctrl0ChargePowerLimit, 0))
				.run();
	}

}
