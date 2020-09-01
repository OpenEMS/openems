package io.openems.edge.evcs.cluster;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.filter.DisabledRampFilter;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.test.DummyEvcsPower;
import io.openems.edge.evcs.test.DummyManagedEvcs;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.test.DummyAsymmetricMeter;

/**
 * Tried to test that Component without run method. This is currently not
 * working.
 *
 */
public class EvcsClusterTest {

	@SuppressWarnings("all")
	private static class MyConfig extends AbstractComponentConfig implements ConfigPeakShaving {

		private final String id;
		private final String alias;
		private final boolean enabled;
		private final boolean debugMode;
		private final int hardwarePowerLimit;
		private final String[] evcs_ids;
		private final String Evcs_target;
		private final String ess_id;
		private final String meter_id;

		public MyConfig(String id, String alias, boolean enabled, boolean debugMode, int hardwarePowerLimit,
				String[] evcs_ids, String ess_id, String meter_id) {
			super(ConfigPeakShaving.class, id);
			this.id = id;
			this.alias = alias;
			this.enabled = enabled;
			this.debugMode = debugMode;
			this.hardwarePowerLimit = hardwarePowerLimit;
			this.evcs_ids = evcs_ids;
			this.Evcs_target = "";
			this.ess_id = ess_id;
			this.meter_id = meter_id;
		}

		@Override
		public boolean debugMode() {
			return this.debugMode;
		}

		@Override
		public int hardwarePowerLimitPerPhase() {
			return this.hardwarePowerLimit;
		}

		@Override
		public String[] evcs_ids() {
			return this.evcs_ids;
		}

		@Override
		public String Evcs_target() {
			return this.Evcs_target;
		}

		@Override
		public String ess_id() {
			return this.ess_id;
		}

		@Override
		public String meter_id() {
			return this.meter_id;
		}
	}

	private class EvcsClusterComponentTest
			extends AbstractComponentTest<EvcsClusterComponentTest, AbstractEvcsCluster> {

		public EvcsClusterComponentTest(AbstractEvcsCluster cluster, OpenemsComponent... components) {
			super(cluster);
			for (OpenemsComponent component : components) {
				this.addComponent(component);
			}
		}

		@Override
		protected void executeController() throws OpenemsNamedException {
			/*
			 * Warn: Maximum power is not correct, because the evcs power of the whole
			 * cluster is still zero
			 */
			this.getSut().getMaximumPowerToDistribute();
			this.getSut().limitEvcss();
		}

		@Override
		protected EvcsClusterComponentTest self() {
			return this;
		}
	}

	private static EvcsClusterPeakShaving peakshavingCluster;
	// private static AbstractEvcsCluster selfConsumptionCluster;
	private static DummyComponentManager componentManager;
	private static DummyConfigurationAdmin configurationAdmin;
	private static DummySum sum;

	@Test
	public void peakShavingClusterTest1() throws Exception {

		// Initialize Component
		peakshavingCluster = new EvcsClusterPeakShaving();

		// Add referenced services
		componentManager = new DummyComponentManager();
		configurationAdmin = new DummyConfigurationAdmin();

		peakshavingCluster.componentManager = componentManager;
		peakshavingCluster.cm = configurationAdmin;

		sum = new DummySum();
		peakshavingCluster.sum = sum;
		String[] evcss = { "evcs0", "evcs1" };
		MyConfig config = new MyConfig("evcsCluster0", "", true, true, 7360, evcss, "ess0", "meter0");

		// Activate (twice, so that reference target is set)
		peakshavingCluster.activate(null, config);
		peakshavingCluster.activate(null, config);

		// Prepare Channels
		ChannelAddress sumEssActivePower = new ChannelAddress("_sum", "EssActivePower");
		ChannelAddress evcs0ChargePower = new ChannelAddress("evcs0", "ChargePower");
		ChannelAddress evcs1ChargePower = new ChannelAddress("evcs1", "ChargePower");
		ChannelAddress evcs0SetChargePowerLimit = new ChannelAddress("evcs0", "SetChargePowerLimit");
		ChannelAddress evcs1SetChargePowerLimit = new ChannelAddress("evcs1", "SetChargePowerLimit");
		ChannelAddress meter0GridActivePower = new ChannelAddress("meter0", "ActivePower");
		ChannelAddress meter0ActivePower1 = new ChannelAddress("meter0", "ActivePowerL1");
		ChannelAddress meter0ActivePower2 = new ChannelAddress("meter0", "ActivePowerL2");
		ChannelAddress meter0ActivePower3 = new ChannelAddress("meter0", "ActivePowerL3");

		Power power = new DummyPower(20000);
		EvcsPower evcsPower = new DummyEvcsPower(new DisabledRampFilter());

		// Build and run test
		ManagedSymmetricEss ess0 = new DummyManagedSymmetricEss("ess0", power);
		AsymmetricMeter meter0 = new DummyAsymmetricMeter("meter0");
		ManagedEvcs evcs1 = new DummyManagedEvcs("evcs0", evcsPower);
		ManagedEvcs evcs2 = new DummyManagedEvcs("evcs1", evcsPower);

		/*
		 * Conditions: - Storage max 60kW - Grid max 22kW i.e. 6kW per phase - Evcss
		 * 22kW + 3kW - Grid (6|3|3) 12kW - Storage (4,33|4,33|4,33) 13kW
		 */

		new EvcsClusterComponentTest(peakshavingCluster, componentManager, peakshavingCluster, evcs1, evcs2, sum, ess0,
				meter0) //
						.next(new TestCase() //
								.input(sumEssActivePower, 13000) //
								.input(evcs0ChargePower, 22000) //
								.input(evcs1ChargePower, 3000) //
								.input(meter0GridActivePower, 12000) //
								.input(meter0ActivePower1, 6000) //
								.input(meter0ActivePower2, 3000) //
								.input(meter0ActivePower3, 3000) //
								.output(evcs0SetChargePowerLimit, 22000) //
								.output(evcs1SetChargePowerLimit, 22000))
						.run();
	}
}
