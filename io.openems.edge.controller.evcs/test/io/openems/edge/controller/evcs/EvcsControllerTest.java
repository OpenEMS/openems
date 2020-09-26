package io.openems.edge.controller.evcs;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.test.DummyManagedEvcs;

public class EvcsControllerTest {

	private static final String SUM_ID = "_sum";
	private static final ChannelAddress SUM_GRID_ACTIVE_POWER = new ChannelAddress(SUM_ID, "GridActivePower");
	private static final ChannelAddress SUM_ESS_ACTIVE_POWER = new ChannelAddress(SUM_ID, "EssActivePower");

	private static final String CTRL_ID = "ctrl0";

	private static final String ESS_ID = "ess0";
	private static final ChannelAddress ESS_ALLOWED_CHARGE_POWER = new ChannelAddress(ESS_ID, "AllowedChargePower");

	private static final String EVCS_ID = "evcs0";
	private static final ChannelAddress EVCS_CHARGE_POWER = new ChannelAddress(EVCS_ID, "ChargePower");
	private static final ChannelAddress EVCS_SET_CHARGE_POWER_LIMIT = new ChannelAddress(EVCS_ID,
			"SetChargePowerLimit");
	private static final ChannelAddress EVCS_SET_CHARGE_POWER_REQUEST = new ChannelAddress(EVCS_ID,
			"SetChargePowerRequest");
	private static final ChannelAddress EVCS_MAXIMUM_POWER = new ChannelAddress(EVCS_ID, "MaximumPower");
	private static final ChannelAddress EVCS_IS_CLUSTERED = new ChannelAddress(EVCS_ID, "IsClustered");
	private static final ChannelAddress EVCS_STATUS = new ChannelAddress(EVCS_ID, "Status");

	@Test
	public void excessChargeTest1() throws Exception {
		new ControllerTest(new EvcsController()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", new DummyManagedEvcs(EVCS_ID)) //
				.addComponent(new DummyManagedSymmetricEss(ESS_ID) //
						.withSoc(20) //
						.withCapacity(9000)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setEvcsId(EVCS_ID) //
						.setDebugMode(false) //
						.setEnabledCharging(true) //
						.setChargeMode(ChargeMode.EXCESS_POWER) //
						.setForceChargeMinPower(3333) //
						.setEnergySessionLimit(0) //
						.setPriority(Priority.CAR) //
						.build())
				.next(new TestCase() //
						.input(SUM_ESS_ACTIVE_POWER, -6000) //
						.input(SUM_GRID_ACTIVE_POWER, 0) //
						.input(EVCS_CHARGE_POWER, 0) //
						.output(EVCS_SET_CHARGE_POWER_LIMIT, 6000)) //
				.run();
	}

	// TODO needs fix by Sebastian Asen
	// @Test
	protected void excessChargeTest2() throws Exception {
		new ControllerTest(new EvcsController()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", new DummyManagedEvcs(EVCS_ID)) //
				.addComponent(new DummyManagedSymmetricEss(ESS_ID) //
						.withSoc(20) //
						.withCapacity(9000) //
						.withMaxApparentPower(30_000)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setEvcsId(EVCS_ID) //
						.setDebugMode(false) //
						.setEnabledCharging(true) //
						.setChargeMode(ChargeMode.EXCESS_POWER) //
						.setForceChargeMinPower(3333) //
						.setEnergySessionLimit(0) //
						.setPriority(Priority.STORAGE) //
						.build())
				.next(new TestCase() //
						.input(SUM_ESS_ACTIVE_POWER, -5000) //
						.input(SUM_GRID_ACTIVE_POWER, -40000) //
						.input(EVCS_CHARGE_POWER, 5000) //
						.input(ESS_ALLOWED_CHARGE_POWER, 30000) //
						.output(EVCS_SET_CHARGE_POWER_LIMIT, 20000));
	}

	@Test
	public void clusterTest() throws Exception {
		final ComponentManager componentManager = new DummyComponentManager();
		new ControllerTest(new EvcsController()) //
				.addReference("componentManager", componentManager) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", new DummyManagedEvcs(EVCS_ID)) //
				.addComponent(new DummyManagedSymmetricEss(ESS_ID) //
						.withSoc(20) //
						.withCapacity(9000) //
						.withMaxApparentPower(30_000)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setEvcsId(EVCS_ID) //
						.setDebugMode(false) //
						.setEnabledCharging(true) //
						.setChargeMode(ChargeMode.EXCESS_POWER) //
						.setForceChargeMinPower(3333) //
						.setEnergySessionLimit(0) //
						.setPriority(Priority.CAR) //
						.build())
				.next(new TestCase() //
						.input(SUM_ESS_ACTIVE_POWER, -10000) //
						.input(SUM_GRID_ACTIVE_POWER, 0) //
						.input(EVCS_CHARGE_POWER, 0) //
						.input(EVCS_MAXIMUM_POWER, 6000) //
						.input(EVCS_IS_CLUSTERED, true) //
						.input(EVCS_STATUS, Status.READY_FOR_CHARGING) //
						.output(EVCS_SET_CHARGE_POWER_REQUEST, 10000));
	}
}
