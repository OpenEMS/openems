package io.openems.edge.controller.ess.limittotaldischarge;

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

public class LimitTotalDischargeTest {

	@SuppressWarnings("all")
	private static class MyConfig extends AbstractComponentConfig implements Config {

		private final String essId;
		private final int minSoc;
		private final int forceChargeSoc;
		private final int forceChargePower;

		public MyConfig(String id, String essId, int minSoc, int forceChargeSoc, int forceChargePower) {
			super(Config.class, id);
			this.essId = essId;
			this.minSoc = minSoc;
			this.forceChargeSoc = forceChargeSoc;
			this.forceChargePower = forceChargePower;
		}

		@Override
		public String ess_id() {
			return this.essId;
		}

		@Override
		public int minSoc() {
			return this.minSoc;
		}

		@Override
		public int forceChargeSoc() {
			return this.forceChargeSoc;
		}

		@Override
		public int forceChargePower() {
			return this.forceChargePower;
		}

	}

	@Test
	public void test() throws Exception {
		// Initialize mocked Clock
		final TimeLeapClock clock = new TimeLeapClock();
		// Initialize Controller
		LimitTotalDischargeController controller = new LimitTotalDischargeController(clock);
		// Add referenced services
		DummyComponentManager componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;
		// Activate (twice, so that reference target is set)
		MyConfig config = new MyConfig("ctrl0", "ess0", 15, 10, 1000);
		controller.activate(null, config);
		controller.activate(null, config);
		// Prepare Channels
		ChannelAddress ess0Soc = new ChannelAddress("ess0", "Soc");
		ChannelAddress ess0SetActivePowerLessOrEquals = new ChannelAddress("ess0", "SetActivePowerLessOrEquals");
		ChannelAddress ctrl0AwaitingHysteresis = new ChannelAddress("ctrl0", "AwaitingHysteresis");
		// Build and run test
		ManagedSymmetricEss ess = new DummyManagedSymmetricEss("ess0");
		new ControllerTest(controller, componentManager, ess, controller) //
				.next(new TestCase() //
						.input(ess0Soc, 20) //
						.output(ess0SetActivePowerLessOrEquals, null).output(ctrl0AwaitingHysteresis, false)) //
				.next(new TestCase() //
						.input(ess0Soc, 15) //
						.output(ess0SetActivePowerLessOrEquals, 0).output(ctrl0AwaitingHysteresis, false)) //
				.next(new TestCase() //
						.input(ess0Soc, 16) //
						.output(ess0SetActivePowerLessOrEquals, 0).output(ctrl0AwaitingHysteresis, true)) //
				.next(new TestCase() //
						.timeleap(clock, 6, ChronoUnit.MINUTES).input(ess0Soc, 16) //
						.output(ess0SetActivePowerLessOrEquals, null).output(ctrl0AwaitingHysteresis, false)) //
				.run();
	}

}
