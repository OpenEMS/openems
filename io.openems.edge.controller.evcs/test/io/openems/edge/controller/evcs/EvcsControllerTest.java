package io.openems.edge.controller.evcs;

import org.junit.Test;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.keba.kecontact.test.DummyKebaKeContact;

@SuppressWarnings("restriction")
public class EvcsControllerTest {

	@SuppressWarnings("all")
	private static class MyConfig extends AbstractComponentConfig implements Config {

		private final String id;
		private final String alias;
		private final boolean enabled;
		private final String evcs_id;
		private final boolean enabledCharging;
		private final ChargeMode chargeMode;
		private final int forceChargeMinPower;
		private final int defaultChargeMinPower;
		private final Priority priority;

		public MyConfig(String id, String alias, boolean enabled, String evcs_id, boolean enabledCharging,
				ChargeMode chargeMode, int forceChargeMinPower, int defaultChargeMinPower, Priority priority) {
			super(Config.class, id);
			this.id = id;
			this.alias = alias;
			this.enabled = enabled;
			this.evcs_id = evcs_id;
			this.enabledCharging = enabledCharging;
			this.chargeMode = chargeMode;
			this.forceChargeMinPower = forceChargeMinPower;
			this.defaultChargeMinPower = defaultChargeMinPower;
			this.priority = priority;
		}

		@Override
		public String id() {
			return this.id;
		}

		@Override
		public String evcs_id() {
			return this.evcs_id;
		}

		@Override
		public boolean enabledCharging() {
			return this.enabledCharging;
		}

		@Override
		public ChargeMode chargeMode() {
			return this.chargeMode;
		}

		@Override
		public int forceChargeMinPower() {
			return this.forceChargeMinPower;
		}

		@Override
		public int defaultChargeMinPower() {
			return this.defaultChargeMinPower;
		}

		@Override
		public Priority priority() {
			return this.priority;
		}

	}

	@Test
	public void test() throws Exception {
		// Initialize mocked Clock
		final TimeLeapClock clock = new TimeLeapClock();
		
		// Initialize Controller
		EvcsController controller = new EvcsController(clock);
		
		// Add referenced services
		DummyComponentManager componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;
		
		DummySum sum = new DummySum();
		controller.sum = sum;

		
		// Activate (twice, so that reference target is set)
		MyConfig config = new MyConfig("ctrl0", "", true, "evcs0", true, ChargeMode.EXCESS_POWER, 10000, 0, Priority.CAR);
		controller.activate(null, config);
		controller.activate(null, config);
		// Prepare Channels

		ChannelAddress sumGridActivePower = new ChannelAddress("_sum", "GridActivePower");
		ChannelAddress sumEssActivePower = new ChannelAddress("_sum", "EssActivePower");
		ChannelAddress evcs0ChargePower = new ChannelAddress("evcs0", "ChargePower");
		ChannelAddress evcs0MaximumPower = new ChannelAddress("evcs0", "MaximumPower");
		//ChannelAddress evcs0CurrUser = new ChannelAddress("evcs0", "CurrUser");
		ChannelAddress evcs0SetPower = new ChannelAddress("evcs0", "SetChargePower");
		
		// Build and run test
		ManagedEvcs evcs = new DummyKebaKeContact("evcs0");
		
		new ControllerTest(controller, componentManager, evcs, controller, sum) //
				.next(new TestCase() //
						.input(sumEssActivePower, -6000) //
						.input(sumGridActivePower, 0)
						.input(evcs0ChargePower, 0)
						.output(evcs0SetPower, 6000)) //
				.next(new TestCase() //
						.input(sumEssActivePower, -10000) //
						.input(sumGridActivePower, 0)
						.input(evcs0ChargePower, 0)
						.input(evcs0MaximumPower, 6000)
						.output(evcs0SetPower, 6000))
						.run();
	}
}
