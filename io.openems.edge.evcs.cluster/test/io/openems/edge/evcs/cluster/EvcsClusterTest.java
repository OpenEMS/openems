package io.openems.edge.evcs.cluster;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.evcs.test.DummyManagedEvcs;
import io.openems.edge.meter.test.DummyAsymmetricMeter;

/**
 * Tried to test that Component without run method. This is currently not
 * working.
 *
 */
public class EvcsClusterTest {

	private final static String CTRL_ID = "ctrl0";

	private final static String SUM_ID = "_sum";
	private final static ChannelAddress SUM_ESS_ACTIVE_POWER = new ChannelAddress(SUM_ID, "EssActivePower");

	private final static String EVCS0_ID = "evcs0";
	private final static ChannelAddress EVCS0_CHARGE_POWER = new ChannelAddress(EVCS0_ID, "ChargePower");
	private final static ChannelAddress EVCS0_SET_CHARGE_POWER_LIMIT = new ChannelAddress(EVCS0_ID,
			"SetChargePowerLimit");

	private final static String EVCS1_ID = "evcs1";
	private final static ChannelAddress EVCS1_CHARGE_POWER = new ChannelAddress(EVCS1_ID, "ChargePower");
	private final static ChannelAddress EVCS1_SET_CHARGE_POWER_LIMIT = new ChannelAddress(EVCS1_ID,
			"SetChargePowerLimit");

	private final static String ESS_ID = "ess0";

	private final static String METER_ID = "meter0";
	private final static ChannelAddress METER_ACTIVE_POWER = new ChannelAddress(METER_ID, "ActivePower");
	private final static ChannelAddress METER_ACTIVE_POWER_L1 = new ChannelAddress(METER_ID, "ActivePowerL1");
	private final static ChannelAddress METER_ACTIVE_POWER_L2 = new ChannelAddress(METER_ID, "ActivePowerL2");
	private final static ChannelAddress METER_ACTIVE_POWER_L3 = new ChannelAddress(METER_ID, "ActivePowerL3");

//	private class EvcsClusterComponentTest
//			extends AbstractComponentTest<EvcsClusterComponentTest, AbstractEvcsCluster> {
//
//		public EvcsClusterComponentTest(AbstractEvcsCluster cluster, OpenemsComponent... components) {
//			super(cluster);
//			for (OpenemsComponent component : components) {
//				this.addComponent(component);
//			}
//		}
//
//		@Override
//		protected void onAfterControllers() throws OpenemsNamedException {
//			/*
//			 * Warn: Maximum power is not correct, because the evcs power of the whole
//			 * cluster is still zero
//			 */
//			this.getSut().getMaximumPowerToDistribute();
//			this.getSut().limitEvcss();
//		}
//
//		@Override
//		protected EvcsClusterComponentTest self() {
//			return this;
//		}
//	}

	// TODO needs a fix by Sebastian Asen
//	@Test
	public void peakShavingClusterTest1() throws Exception {
		new ComponentTest(new EvcsClusterPeakShaving()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID) //
						.withMaxApparentPower(20_000) //
						.withAllowedChargePower(20_000) //
						.withAllowedDischargePower(20_000)) //
				.addReference("addEvcs", new DummyManagedEvcs(EVCS0_ID)) //
				.addReference("addEvcs", new DummyManagedEvcs(EVCS1_ID)) //
				.addComponent(new DummyAsymmetricMeter(METER_ID)) //
				.activate(MyConfigPeakShaving.create() //
						.setId(CTRL_ID) //
						.setDebugMode(true) //
						.setHardwarePowerLimitPerPhase(7360) //
						.setEvcsIds(EVCS0_ID, EVCS1_ID).setEssId(ESS_ID) //
						.setMeterId(METER_ID) //
						.build())

				/*
				 * Conditions: - Storage max 60kW - Grid max 22kW i.e. 6kW per phase - Evcss
				 * 22kW + 3kW - Grid (6|3|3) 12kW - Storage (4,33|4,33|4,33) 13kW
				 */
				.next(new TestCase() //
						.input(SUM_ESS_ACTIVE_POWER, 13000) //
						.input(EVCS0_CHARGE_POWER, 22000) //
						.input(EVCS1_CHARGE_POWER, 3000) //
						.input(METER_ACTIVE_POWER, 12000) //
						.input(METER_ACTIVE_POWER_L1, 6000) //
						.input(METER_ACTIVE_POWER_L2, 3000) //
						.input(METER_ACTIVE_POWER_L3, 3000) //
						.output(EVCS0_SET_CHARGE_POWER_LIMIT, 22000) //
						.output(EVCS1_SET_CHARGE_POWER_LIMIT, 22000))
				.run();
	}
}
