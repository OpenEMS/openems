package io.openems.edge.ess.cluster;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.test.DummyManagedAsymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;

public class EssClusterImplTest {

	private static final String CLUSTER_ID = "ess0";
	private static final String ESS1_ID = "ess1";
	private static final String ESS2_ID = "ess2";
	private static final String ESS3_ID = "ess3";
	private static final ChannelAddress CLUSTER_GRID_MODE = new ChannelAddress(CLUSTER_ID, "GridMode");
	private static final ChannelAddress ESS1_GRID_MODE = new ChannelAddress(ESS1_ID, "GridMode");
	private static final ChannelAddress ESS2_GRID_MODE = new ChannelAddress(ESS2_ID, "GridMode");
	private static final ChannelAddress ESS3_GRID_MODE = new ChannelAddress(ESS3_ID, "GridMode");
	private static final ChannelAddress CLUSTER_SOC = new ChannelAddress(CLUSTER_ID, "Soc");
	private static final ChannelAddress ESS1_SOC = new ChannelAddress(ESS1_ID, "Soc");
	private static final ChannelAddress ESS2_SOC = new ChannelAddress(ESS2_ID, "Soc");
	private static final ChannelAddress CLUSTER_ACTIVE_POWER = new ChannelAddress(CLUSTER_ID, "ActivePower");
	private static final ChannelAddress ESS1_ACTIVE_POWER = new ChannelAddress(ESS1_ID, "ActivePower");
	private static final ChannelAddress ESS2_ACTIVE_POWER = new ChannelAddress(ESS2_ID, "ActivePower");
	private static final ChannelAddress CLUSTER_REACTIVE_POWER = new ChannelAddress(CLUSTER_ID, "ReactivePower");
	private static final ChannelAddress ESS1_REACTIVE_POWER = new ChannelAddress(ESS1_ID, "ReactivePower");
	private static final ChannelAddress ESS2_REACTIVE_POWER = new ChannelAddress(ESS2_ID, "ReactivePower");
	private static final ChannelAddress CLUSTER_ACTIVE_POWER_L1 = new ChannelAddress(CLUSTER_ID, "ActivePowerL1");
	private static final ChannelAddress ESS2_ACTIVE_POWER_L1 = new ChannelAddress(ESS2_ID, "ActivePowerL1");
	private static final ChannelAddress CLUSTER_ACTIVE_CHARGE_ENERGY = new ChannelAddress(CLUSTER_ID,
			"ActiveChargeEnergy");
	private static final ChannelAddress ESS1_ACTIVE_CHARGE_ENERGY = new ChannelAddress(ESS1_ID, "ActiveChargeEnergy");
	private static final ChannelAddress ESS2_ACTIVE_CHARGE_ENERGY = new ChannelAddress(ESS2_ID, "ActiveChargeEnergy");
	private static final ChannelAddress CLUSTER_ALLOWED_CHARGE_POWER = new ChannelAddress(CLUSTER_ID,
			"AllowedChargePower");
	private static final ChannelAddress ESS1_ALLOWED_CHARGE_POWER = new ChannelAddress(ESS1_ID, "AllowedChargePower");
	private static final ChannelAddress ESS2_ALLOWED_CHARGE_POWER = new ChannelAddress(ESS2_ID, "AllowedChargePower");
	private static final ChannelAddress CLUSTER_ALLOWED_DISCHARGE_POWER = new ChannelAddress(CLUSTER_ID,
			"AllowedDischargePower");
	private static final ChannelAddress ESS1_ALLOWED_DISCHARGE_POWER = new ChannelAddress(ESS1_ID,
			"AllowedDischargePower");
	private static final ChannelAddress ESS2_ALLOWED_DISCHARGE_POWER = new ChannelAddress(ESS2_ID,
			"AllowedDischargePower");
	private static final ChannelAddress CLUSTER_START_STOP = new ChannelAddress(CLUSTER_ID, "StartStop");
	private static final ChannelAddress ESS1_START_STOP = new ChannelAddress(ESS1_ID, "StartStop");
	private static final ChannelAddress ESS2_START_STOP = new ChannelAddress(ESS2_ID, "StartStop");

	@Test
	public void testCluster() throws Exception {
		new ComponentTest(new EssClusterImpl()) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("addEss", new DummyManagedSymmetricEss(ESS1_ID)) //
				.addReference("addEss", new DummyManagedAsymmetricEss(ESS2_ID)) //
				.activate(MyConfig.create() //
						.setId(CLUSTER_ID) //
						.setEssIds(ESS1_ID, ESS2_ID) //
						.setStartStop(StartStopConfig.START) //
						.build())
				.next(new TestCase() //
						.input(ESS1_GRID_MODE, GridMode.ON_GRID) //
						.input(ESS2_GRID_MODE, GridMode.ON_GRID) //
						.output(CLUSTER_GRID_MODE, GridMode.ON_GRID) //
						.input(ESS1_ACTIVE_POWER, 1234) //
						.input(ESS2_ACTIVE_POWER, 9876) //
						.output(CLUSTER_ACTIVE_POWER, 11110) //
						.input(ESS1_REACTIVE_POWER, 1111) //
						.input(ESS2_REACTIVE_POWER, 2222) //
						.output(CLUSTER_REACTIVE_POWER, 3333) //
						.input(ESS1_ACTIVE_CHARGE_ENERGY, 1) //
						.input(ESS2_ACTIVE_CHARGE_ENERGY, 2) //
						.output(CLUSTER_ACTIVE_CHARGE_ENERGY, 3L) //
						.input(ESS2_ACTIVE_POWER_L1, 1111) //
						.output(CLUSTER_ACTIVE_POWER_L1, 1234 / 3 + 1111) //
						.input(ESS1_ALLOWED_CHARGE_POWER, 11) //
						.input(ESS2_ALLOWED_CHARGE_POWER, 22) //
						.output(CLUSTER_ALLOWED_CHARGE_POWER, 33) //
						.input(ESS1_ALLOWED_DISCHARGE_POWER, 10) //
						.input(ESS2_ALLOWED_DISCHARGE_POWER, 20) //
						.output(CLUSTER_ALLOWED_DISCHARGE_POWER, 30) //
				);
	}

	@Test
	public void testGridMode() throws Exception {
		new ComponentTest(new EssClusterImpl()) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("addEss", new DummyManagedSymmetricEss(ESS1_ID)) //
				.addReference("addEss", new DummyManagedSymmetricEss(ESS2_ID)) //
				.addReference("addEss", new DummyManagedSymmetricEss(ESS3_ID)) //
				.activate(MyConfig.create() //
						.setId(CLUSTER_ID) //
						.setEssIds(ESS1_ID, ESS2_ID, ESS3_ID) //
						.setStartStop(StartStopConfig.START) //
						.build())
				.next(new TestCase() //
						.input(ESS1_GRID_MODE, GridMode.ON_GRID) //
						.input(ESS2_GRID_MODE, GridMode.ON_GRID) //
						.input(ESS3_GRID_MODE, GridMode.ON_GRID) //
						.output(CLUSTER_GRID_MODE, GridMode.ON_GRID) //
				) //
				.next(new TestCase() //
						.input(ESS1_GRID_MODE, GridMode.OFF_GRID) //
						.input(ESS2_GRID_MODE, GridMode.OFF_GRID) //
						.input(ESS3_GRID_MODE, GridMode.OFF_GRID) //
						.output(CLUSTER_GRID_MODE, GridMode.OFF_GRID) //
				) //
				.next(new TestCase() //
						.input(ESS1_GRID_MODE, GridMode.OFF_GRID) //
						.input(ESS2_GRID_MODE, GridMode.OFF_GRID) //
						.input(ESS3_GRID_MODE, GridMode.UNDEFINED) //
						.output(CLUSTER_GRID_MODE, GridMode.UNDEFINED) //
				) //
		;
	}

	@Test
	public void testSoc() throws Exception {
		new ComponentTest(new EssClusterImpl()) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("addEss", new DummyManagedSymmetricEss(ESS1_ID).withCapacity(50000)) //
				.addReference("addEss", new DummyManagedSymmetricEss(ESS2_ID).withCapacity(3000)) //
				.activate(MyConfig.create() //
						.setId(CLUSTER_ID) //
						.setEssIds(ESS1_ID, ESS2_ID) //
						.setStartStop(StartStopConfig.START) //
						.build())
				.next(new TestCase() //
						.input(ESS1_SOC, 20) //
						.input(ESS2_SOC, 90) //
						.output(CLUSTER_SOC, 24) //
				) //
				.next(new TestCase() //
						.input(ESS1_SOC, 21) //
						.output(CLUSTER_SOC, 25) //
				) //
				.next(new TestCase() //
						.input(ESS1_SOC, 100) //
						.output(CLUSTER_SOC, 99) //
				) //
		;
	}

	@Test
	public void testStartStop() throws Exception {
		new ComponentTest(new EssClusterImpl()) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("addEss", new DummyManagedSymmetricEss(ESS1_ID)) //
				.addReference("addEss", new DummyManagedSymmetricEss(ESS2_ID)) //
				.activate(MyConfig.create() //
						.setId(CLUSTER_ID) //
						.setEssIds(ESS1_ID, ESS2_ID) //
						.setStartStop(StartStopConfig.START) //
						.build())
				.next(new TestCase() //
						.input(ESS1_START_STOP, StartStop.UNDEFINED) //
						.input(ESS2_START_STOP, StartStop.STOP) //
						.output(CLUSTER_START_STOP, StartStop.UNDEFINED)) //
				.next(new TestCase() //
						.input(ESS1_START_STOP, StartStop.STOP) //
						.input(ESS2_START_STOP, StartStop.STOP) //
						.output(CLUSTER_START_STOP, StartStop.STOP)) //
				.next(new TestCase() //
						.input(ESS1_START_STOP, StartStop.START) //
						.input(ESS2_START_STOP, StartStop.STOP) //
						.output(CLUSTER_START_STOP, StartStop.UNDEFINED)) //
				.next(new TestCase() //
						.input(ESS1_START_STOP, StartStop.START) //
						.input(ESS2_START_STOP, StartStop.START) //
						.output(CLUSTER_START_STOP, StartStop.START)) //

		;
	}
}