package io.openems.edge.controller.evcs;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.test.DummyManagedEvcs;

public class EvcsControllerTest {

	@SuppressWarnings("all")
	private static class MyConfig extends AbstractComponentConfig implements Config {

		private final String id;
		private final String alias;
		private final boolean enabled;
		private final String evcsId;
		private final boolean enabledCharging;
		private final ChargeMode chargeMode;
		private final int forceChargeMinPower;
		private final int defaultChargeMinPower;
		private final Priority priority;
		private final String essId;
		private final int energySessionLimit;

		public MyConfig(String id, String alias, boolean enabled, String evcsId, boolean enabledCharging,
				ChargeMode chargeMode, int forceChargeMinPower, int defaultChargeMinPower, Priority priority,
				String essId, int energySessionLimit) {
			super(Config.class, id);
			this.id = id;
			this.alias = alias;
			this.enabled = enabled;
			this.evcsId = evcsId;
			this.enabledCharging = enabledCharging;
			this.chargeMode = chargeMode;
			this.forceChargeMinPower = forceChargeMinPower;
			this.defaultChargeMinPower = defaultChargeMinPower;
			this.priority = priority;
			this.essId = essId;
			this.energySessionLimit = energySessionLimit;
		}

		@Override
		public String id() {
			return this.id;
		}

		@Override
		public String evcs_id() {
			return this.evcsId;
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

		@Override
		public String ess_id() {
			return this.essId;
		}

		@Override
		public int energySessionLimit() {
			return this.energySessionLimit;
		}
	}

	private static EvcsController controller;
	private static DummyComponentManager componentManager;
	private static DummySum sum;

	@Test
	public void excessChargeTest1() throws Exception {
		// Initialize mocked Clock
		final TimeLeapClock clock = new TimeLeapClock();

		// Initialize Controller
		controller = new EvcsController(clock);

		// Add referenced services
		componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;

		sum = new DummySum();
		controller.sum = sum;

		// Activate (twice, so that reference target is set)
		MyConfig config = new MyConfig("ctrl0", "", true, "evcs0", true, ChargeMode.EXCESS_POWER, 3333, 0,
				Priority.CAR, "ess0", 0);
		controller.activate(null, config);
		controller.activate(null, config);
		// Prepare Channels

		ChannelAddress sumGridActivePower = new ChannelAddress("_sum", "GridActivePower");
		ChannelAddress sumEssActivePower = new ChannelAddress("_sum", "EssActivePower");
		ChannelAddress evcs0ChargePower = new ChannelAddress("evcs0", "ChargePower");
		ChannelAddress evcs0SetChargePowerLimit = new ChannelAddress("evcs0", "SetChargePowerLimit");

		// Build and run test
		ManagedSymmetricEss ess = new DummyManagedSymmetricEss("ess0");
		ManagedEvcs evcs = new DummyManagedEvcs("evcs0");

		new ControllerTest(controller, componentManager, evcs, controller, sum, ess) //
				.next(new TestCase() //
						.input(sumEssActivePower, -6000) //
						.input(sumGridActivePower, 0) //
						.input(evcs0ChargePower, 0) //
						.output(evcs0SetChargePowerLimit, 6000)) //
				.run();
	}

	@Test
	public void excessChargeTest2() throws Exception {
		
		// Initialize mocked Clock
		final TimeLeapClock clock = new TimeLeapClock();

		// Initialize Controller
		controller = new EvcsController(clock);

		// Add referenced services
		componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;

		sum = new DummySum();
		controller.sum = sum;

		// Activate (twice, so that reference target is set)
		MyConfig config = new MyConfig("ctrl0", "", true, "evcs0", true, ChargeMode.EXCESS_POWER, 3333, 0,
				Priority.STORAGE, "ess0", 0);
		controller.activate(null, config);
		controller.activate(null, config);
		
		// Prepare Channels
		ChannelAddress sumGridActivePower = new ChannelAddress("_sum", "GridActivePower");
		ChannelAddress sumEssActivePower = new ChannelAddress("_sum", "EssActivePower");
		ChannelAddress evcs0ChargePower = new ChannelAddress("evcs0", "ChargePower");
		ChannelAddress evcs0SetChargePowerLimit = new ChannelAddress("evcs0", "SetChargePowerLimit");

		Power power = new DummyPower(30000);
		
		// Build and run test
		ManagedSymmetricEss ess = new DummyManagedSymmetricEss("ess0", power);
		ManagedEvcs evcs = new DummyManagedEvcs("evcs0");

		new ControllerTest(controller, componentManager, evcs, controller, sum, ess) //
				.next(new TestCase() //
						.input(sumEssActivePower, -5000) //
						.input(sumGridActivePower, -40000) //
						.input(evcs0ChargePower, 5000) //
						.output(evcs0SetChargePowerLimit, 20000))
				.run();
	}

	@Test
	public void clusterTest() throws Exception {
		// Initialize mocked Clock
		final TimeLeapClock clock = new TimeLeapClock();

		// Initialize Controller
		controller = new EvcsController(clock);

		// Add referenced services
		componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;

		sum = new DummySum();
		controller.sum = sum;

		// Activate (twice, so that reference target is set)
		MyConfig config = new MyConfig("ctrl0", "", true, "evcs0", true, ChargeMode.EXCESS_POWER, 3333, 0,
				Priority.CAR, "ess0", 0);
		controller.activate(null, config);
		controller.activate(null, config);
		// Prepare Channels

		ChannelAddress sumGridActivePower = new ChannelAddress("_sum", "GridActivePower");
		ChannelAddress sumEssActivePower = new ChannelAddress("_sum", "EssActivePower");
		ChannelAddress evcs0ChargePower = new ChannelAddress("evcs0", "ChargePower");
		ChannelAddress evcs0MaximumPower = new ChannelAddress("evcs0", "MaximumPower");
		ChannelAddress evcs0IsClustered = new ChannelAddress("evcs0", "IsClustered");
		ChannelAddress evcs0SetPowerRequest = new ChannelAddress("evcs0", "SetChargePowerRequest");
		ChannelAddress evcs0Status = new ChannelAddress("evcs0", "Status");

		// Build and run test
		ManagedSymmetricEss ess = new DummyManagedSymmetricEss("ess0");
		ManagedEvcs evcs = new DummyManagedEvcs("evcs0");

		new ControllerTest(controller, componentManager, evcs, controller, sum, ess) //
				.next(new TestCase().input(sumEssActivePower, -10000) //
						.input(sumGridActivePower, 0).input(evcs0ChargePower, 0).input(evcs0MaximumPower, 6000)
						.input(evcs0IsClustered, true).input(evcs0Status, Status.READY_FOR_CHARGING)
						.output(evcs0SetPowerRequest, 10000))
				.run();
	}
}
