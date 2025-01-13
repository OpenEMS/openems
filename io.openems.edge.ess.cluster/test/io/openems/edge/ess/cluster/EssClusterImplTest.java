package io.openems.edge.ess.cluster;

import static io.openems.edge.common.startstop.StartStoppable.ChannelId.START_STOP;
import static io.openems.edge.ess.api.AsymmetricEss.ChannelId.ACTIVE_POWER_L1;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.ACTIVE_POWER;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.GRID_MODE;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.REACTIVE_POWER;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.SOC;

import org.junit.Test;

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

	@Test
	public void testCluster() throws Exception {
		new ComponentTest(new EssClusterImpl()) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("addEss", new DummyManagedSymmetricEss("ess1")) //
				.addReference("addEss", new DummyManagedAsymmetricEss("ess2")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setEssIds("ess1", "ess2") //
						.setStartStop(StartStopConfig.START) //
						.build())
				.next(new TestCase() //
						.input("ess1", GRID_MODE, GridMode.ON_GRID) //
						.input("ess2", GRID_MODE, GridMode.ON_GRID) //
						.output(GRID_MODE, GridMode.ON_GRID) //
						.input("ess1", ACTIVE_POWER, 1234) //
						.input("ess2", ACTIVE_POWER, 9876) //
						.output(ACTIVE_POWER, 11110) //
						.input("ess1", REACTIVE_POWER, 1111) //
						.input("ess2", REACTIVE_POWER, 2222) //
						.output(REACTIVE_POWER, 3333) //
						.input("ess1", ACTIVE_CHARGE_ENERGY, 1) //
						.input("ess2", ACTIVE_CHARGE_ENERGY, 2) //
						.output(ACTIVE_CHARGE_ENERGY, 3L) //
						.input("ess2", ACTIVE_POWER_L1, 1111) //
						.output(ACTIVE_POWER_L1, 1234 / 3 + 1111) //
						.input("ess1", ALLOWED_CHARGE_POWER, 11) //
						.input("ess2", ALLOWED_CHARGE_POWER, 22) //
						.output(ALLOWED_CHARGE_POWER, 33) //
						.input("ess1", ALLOWED_DISCHARGE_POWER, 10) //
						.input("ess2", ALLOWED_DISCHARGE_POWER, 20) //
						.output(ALLOWED_DISCHARGE_POWER, 30) //
				);
	}

	@Test
	public void testGridMode() throws Exception {
		new ComponentTest(new EssClusterImpl()) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("addEss", new DummyManagedSymmetricEss("ess1")) //
				.addReference("addEss", new DummyManagedSymmetricEss("ess2")) //
				.addReference("addEss", new DummyManagedSymmetricEss("ess3")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setEssIds("ess1", "ess2", "ess3") //
						.setStartStop(StartStopConfig.START) //
						.build())
				.next(new TestCase() //
						.input("ess1", GRID_MODE, GridMode.ON_GRID) //
						.input("ess2", GRID_MODE, GridMode.ON_GRID) //
						.input("ess3", GRID_MODE, GridMode.ON_GRID) //
						.output(GRID_MODE, GridMode.ON_GRID) //
				) //
				.next(new TestCase() //
						.input("ess1", GRID_MODE, GridMode.OFF_GRID) //
						.input("ess2", GRID_MODE, GridMode.OFF_GRID) //
						.input("ess3", GRID_MODE, GridMode.OFF_GRID) //
						.output(GRID_MODE, GridMode.OFF_GRID) //
				) //
				.next(new TestCase() //
						.input("ess1", GRID_MODE, GridMode.OFF_GRID) //
						.input("ess2", GRID_MODE, GridMode.OFF_GRID) //
						.input("ess3", GRID_MODE, GridMode.UNDEFINED) //
						.output(GRID_MODE, GridMode.UNDEFINED) //
				) //
		;
	}

	@Test
	public void testSoc() throws Exception {
		new ComponentTest(new EssClusterImpl()) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("addEss", new DummyManagedSymmetricEss("ess1").withCapacity(50000)) //
				.addReference("addEss", new DummyManagedSymmetricEss("ess2").withCapacity(3000)) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setEssIds("ess1", "ess2") //
						.setStartStop(StartStopConfig.START) //
						.build())
				.next(new TestCase() //
						.input("ess1", SOC, 20) //
						.input("ess2", SOC, 90) //
						.output(SOC, 24) //
				) //
				.next(new TestCase() //
						.input("ess1", SOC, 21) //
						.output(SOC, 25) //
				) //
				.next(new TestCase() //
						.input("ess1", SOC, 100) //
						.output(SOC, 99) //
				) //
		;
	}

	@Test
	public void testStartStop() throws Exception {
		new ComponentTest(new EssClusterImpl()) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("addEss", new DummyManagedSymmetricEss("ess1")) //
				.addReference("addEss", new DummyManagedSymmetricEss("ess2")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setEssIds("ess1", "ess2") //
						.setStartStop(StartStopConfig.START) //
						.build())
				.next(new TestCase() //
						.input("ess1", START_STOP, StartStop.UNDEFINED) //
						.input("ess2", START_STOP, StartStop.STOP) //
						.output(START_STOP, StartStop.UNDEFINED)) //
				.next(new TestCase() //
						.input("ess1", START_STOP, StartStop.STOP) //
						.input("ess2", START_STOP, StartStop.STOP) //
						.output(START_STOP, StartStop.STOP)) //
				.next(new TestCase() //
						.input("ess1", START_STOP, StartStop.START) //
						.input("ess2", START_STOP, StartStop.STOP) //
						.output(START_STOP, StartStop.UNDEFINED)) //
				.next(new TestCase() //
						.input("ess1", START_STOP, StartStop.START) //
						.input("ess2", START_STOP, StartStop.START) //
						.output(START_STOP, StartStop.START)) //

		;
	}
}