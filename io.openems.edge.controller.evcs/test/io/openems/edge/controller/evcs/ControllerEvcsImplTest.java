package io.openems.edge.controller.evcs;

import static io.openems.edge.common.sum.Sum.ChannelId.ESS_DISCHARGE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.ESS_SOC;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.common.test.TestUtils.createDummyClock;
import static io.openems.edge.controller.evcs.ControllerEvcs.ChannelId.AWAITING_HYSTERESIS;
import static io.openems.edge.controller.evcs.Priority.CAR;
import static io.openems.edge.evcs.api.ChargeMode.EXCESS_POWER;
import static io.openems.edge.evcs.api.ChargeMode.FORCE_CHARGE;
import static io.openems.edge.evcs.api.Evcs.ChannelId.MAXIMUM_HARDWARE_POWER;
import static io.openems.edge.evcs.api.Evcs.ChannelId.MAXIMUM_POWER;
import static io.openems.edge.evcs.api.Evcs.ChannelId.STATUS;
import static io.openems.edge.evcs.api.ManagedEvcs.ChannelId.IS_CLUSTERED;
import static io.openems.edge.evcs.api.ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT;
import static io.openems.edge.evcs.api.ManagedEvcs.ChannelId.SET_CHARGE_POWER_REQUEST;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.test.DummyManagedEvcs;

public class ControllerEvcsImplTest {

	private static final int DEFAULT_FORCE_CHARGE_MIN_POWER = 7360;
	private static final int DEFAULT_CHARGE_MIN_POWER = 0;

	@Test
	public void excessChargeTest1() throws Exception {
		new ControllerTest(new ControllerEvcsImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", DummyManagedEvcs.ofDisabled("evcs0")) //
				.activate(MyConfig.create() //
						.setId("ctrlEvcs0") //
						.setEvcsId("evcs0") //
						.setEnableCharging(true) //
						.setChargeMode(EXCESS_POWER) //
						.setForceChargeMinPower(DEFAULT_FORCE_CHARGE_MIN_POWER) //
						.setDefaultChargeMinPower(DEFAULT_CHARGE_MIN_POWER) //
						.setPriority(CAR) //
						.setEnergySessionLimit(0) //
						.build()) //
				.next(new TestCase() //
						.input(ESS_DISCHARGE_POWER, -6000) //
						.input("evcs0", IS_CLUSTERED, false) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input("evcs0", ACTIVE_POWER, 0) //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 6000)) //
				.deactivate();
	}

	@Test
	public void excessChargeTest2() throws Exception {
		new ControllerTest(new ControllerEvcsImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", DummyManagedEvcs.ofDisabled("evcs0")) //
				.activate(MyConfig.create() //
						.setId("ctrlEvcs0") //
						.setEvcsId("evcs0") //
						.setEnableCharging(true) //
						.setChargeMode(EXCESS_POWER) //
						.setForceChargeMinPower(DEFAULT_FORCE_CHARGE_MIN_POWER) //
						.setDefaultChargeMinPower(DEFAULT_CHARGE_MIN_POWER) //
						.setPriority(Priority.STORAGE) //
						.setEnergySessionLimit(0) //
						.build()) //
				.next(new TestCase() //
						.input(ESS_SOC, 50) //
						.input(ESS_DISCHARGE_POWER, -5000) //
						.input("evcs0", IS_CLUSTERED, false) //
						.input(GRID_ACTIVE_POWER, -40000) //
						.input("evcs0", ACTIVE_POWER, 5000) //
						.input("evcs0", MAXIMUM_HARDWARE_POWER, 22080) //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 44800)) //
				.deactivate();
	}

	@Test
	public void forceChargeTest() throws Exception {
		new ControllerTest(new ControllerEvcsImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", DummyManagedEvcs.ofDisabled("evcs0")) //
				.activate(MyConfig.create() //
						.setId("ctrlEvcs0") //
						.setEvcsId("evcs0") //
						.setEnableCharging(true) //
						.setChargeMode(FORCE_CHARGE) //
						.setForceChargeMinPower(DEFAULT_FORCE_CHARGE_MIN_POWER) //
						.setDefaultChargeMinPower(DEFAULT_CHARGE_MIN_POWER) //
						.setPriority(CAR) // s
						.setEnergySessionLimit(0) //
						.build()) //
				.next(new TestCase() //
						.input(ESS_DISCHARGE_POWER, -5000) //
						.input("evcs0", IS_CLUSTERED, false) //
						.input(GRID_ACTIVE_POWER, -40000) //
						.input("evcs0", ACTIVE_POWER, 5000) //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 22080)) //
				.deactivate();
	}

	@Test
	public void chargingDisabledTest() throws Exception {
		new ControllerTest(new ControllerEvcsImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", DummyManagedEvcs.ofDisabled("evcs0")) //
				.activate(MyConfig.create() //
						.setId("ctrlEvcs0") //
						.setEvcsId("evcs0") //
						.setEnableCharging(false) //
						.setChargeMode(EXCESS_POWER) //
						.setForceChargeMinPower(DEFAULT_FORCE_CHARGE_MIN_POWER) //
						.setDefaultChargeMinPower(DEFAULT_CHARGE_MIN_POWER) //
						.setPriority(CAR) //
						.setEnergySessionLimit(0) //
						.build()) //
				.next(new TestCase() //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 0)) //
				.deactivate();
	}

	@Test
	public void wrongConfigParametersTest() throws Exception {
		var cm = new DummyConfigurationAdmin();
		new ControllerTest(new ControllerEvcsImpl()) //
				.addReference("cm", cm) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", DummyManagedEvcs.ofDisabled("evcs0")) //
				.activate(MyConfig.create() //
						.setId("ctrlEvcs0") //
						.setEvcsId("evcs0") //
						.setEnableCharging(true) //
						.setChargeMode(EXCESS_POWER) //
						.setForceChargeMinPower(30_000) //
						.setDefaultChargeMinPower(30_000) //
						.setPriority(CAR) //
						.setEnergySessionLimit(0) //
						.build()) //
				.next(new TestCase() //
						.input("evcs0", MAXIMUM_HARDWARE_POWER, 12000)) //
				.deactivate();

		assertEquals(12000,
				(int) (Integer) cm.getConfiguration("ctrlEvcs0").getProperties().get("defaultChargeMinPower"));
	}

	@Test
	public void clusterTest() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerEvcsImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", DummyManagedEvcs.ofDisabled("evcs0")) //
				.activate(MyConfig.create() //
						.setId("ctrlEvcs0") //
						.setEvcsId("evcs0") //
						.setEnableCharging(true) //
						.setChargeMode(EXCESS_POWER) //
						.setForceChargeMinPower(3_333) //
						.setDefaultChargeMinPower(DEFAULT_CHARGE_MIN_POWER) //
						.setPriority(CAR) //
						.setEnergySessionLimit(0) //
						.build()) //
				.next(new TestCase() //
						.input(ESS_DISCHARGE_POWER, -10000) //
						.input("evcs0", IS_CLUSTERED, true) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input("evcs0", ACTIVE_POWER, 0) //
						.input("evcs0", STATUS, Status.CHARGING) //
						.output("evcs0", SET_CHARGE_POWER_REQUEST, 10000))
				.next(new TestCase() //
						.input(ESS_DISCHARGE_POWER, -6000) //
						.input("evcs0", IS_CLUSTERED, true) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input("evcs0", ACTIVE_POWER, 0) //
						.input("evcs0", STATUS, Status.NOT_READY_FOR_CHARGING) //
						.output("evcs0", SET_CHARGE_POWER_REQUEST, 0)) // f
				.next(new TestCase() //
						.input(ESS_DISCHARGE_POWER, -6000) //
						.input("evcs0", IS_CLUSTERED, true) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input("evcs0", ACTIVE_POWER, 0) //
						.input("evcs0", STATUS, null) //
						.output("evcs0", SET_CHARGE_POWER_REQUEST, 0)) //
				.next(new TestCase() //
						.input(ESS_DISCHARGE_POWER, -6000) //
						.input("evcs0", IS_CLUSTERED, true) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input("evcs0", ACTIVE_POWER, 0) //
						.input("evcs0", STATUS, Status.CHARGING_REJECTED) //
						.output("evcs0", SET_CHARGE_POWER_REQUEST, 6000) //
						.output("evcs0", MAXIMUM_POWER, null)) //
				.deactivate();
	}

	@Test
	public void clusterTestDisabledCharging() throws Exception {
		new ControllerTest(new ControllerEvcsImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", DummyManagedEvcs.ofDisabled("evcs0")) //
				.activate(MyConfig.create() //
						.setId("ctrlEvcs0") //
						.setEvcsId("evcs0") //
						.setEnableCharging(false) //
						.setChargeMode(EXCESS_POWER) //
						.setForceChargeMinPower(3_333) //
						.setDefaultChargeMinPower(DEFAULT_CHARGE_MIN_POWER) //
						.setPriority(CAR) //
						.setEnergySessionLimit(0) //
						.build()) //
				.next(new TestCase() //
						.input(ESS_DISCHARGE_POWER, -10000) //
						.input("evcs0", IS_CLUSTERED, true) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input("evcs0", ACTIVE_POWER, 0) //
						.input("evcs0", STATUS, Status.CHARGING) //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 0))
				.next(new TestCase() //
						.input(ESS_DISCHARGE_POWER, -6000) //
						.input("evcs0", IS_CLUSTERED, true) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input("evcs0", ACTIVE_POWER, 0) //
						.input("evcs0", STATUS, Status.NOT_READY_FOR_CHARGING) //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 0)) //
				.next(new TestCase() //
						.input(ESS_DISCHARGE_POWER, -6000) //
						.input("evcs0", IS_CLUSTERED, true) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input("evcs0", ACTIVE_POWER, 0) //
						.input("evcs0", STATUS, null) //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 0)) //
				.next(new TestCase() //
						.input(ESS_DISCHARGE_POWER, -6000) //
						.input("evcs0", IS_CLUSTERED, true) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input("evcs0", ACTIVE_POWER, 0) //
						.input("evcs0", STATUS, Status.CHARGING_REJECTED) //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 0) //
						.output("evcs0", MAXIMUM_POWER, null)) //
				.deactivate();
	}

	@Test
	public void hysteresisTest() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerEvcsImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", DummyManagedEvcs.ofDisabled("evcs0")) //
				.activate(MyConfig.create() //
						.setId("ctrlEvcs0") //
						.setEvcsId("evcs0") //
						.setEnableCharging(true) //
						.setChargeMode(EXCESS_POWER) //
						.setForceChargeMinPower(DEFAULT_FORCE_CHARGE_MIN_POWER) //
						.setDefaultChargeMinPower(DEFAULT_CHARGE_MIN_POWER) //
						.setPriority(CAR) //
						.setEnergySessionLimit(0) //
						.setExcessChargeHystersis(120) //
						.setExcessChargePauseHysteresis(30) //
						.build()) //
				.next(new TestCase() //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input("evcs0", IS_CLUSTERED, false) //
						.input(GRID_ACTIVE_POWER, -6_000) //
						.input("evcs0", ACTIVE_POWER, 0) //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 6_000)) //
				.next(new TestCase() //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(GRID_ACTIVE_POWER, -200) //
						.input("evcs0", ACTIVE_POWER, 5800) //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 6_000)) //
				.next(new TestCase() //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(GRID_ACTIVE_POWER, 500) //
						.input("evcs0", ACTIVE_POWER, 5800) //
						.input("evcs0", Evcs.ChannelId.MINIMUM_HARDWARE_POWER, Evcs.DEFAULT_MINIMUM_HARDWARE_POWER)
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 5300))

				// Active hysteresis
				.next(new TestCase() //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(GRID_ACTIVE_POWER, 1000) //
						.input("evcs0", ACTIVE_POWER, 5000) //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 5_300) //
						.output(AWAITING_HYSTERESIS, true)) //
				.next(new TestCase() //
						.timeleap(clock, 6, MINUTES)) //

				// Passed hysteresis
				.next(new TestCase() //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(GRID_ACTIVE_POWER, 1000) //
						.input("evcs0", ACTIVE_POWER, 5000) //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 0) //
						.output(AWAITING_HYSTERESIS, false)) //

				// Active hysteresis
				.next(new TestCase() //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(GRID_ACTIVE_POWER, -5000) //
						.input("evcs0", ACTIVE_POWER, 0) //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 0) //
						.output(AWAITING_HYSTERESIS, true))

				.next(new TestCase() //
						.timeleap(clock, 1, MINUTES)) //

				// New charge process starting after another 30 seconds
				.next(new TestCase() //
						.input(ESS_DISCHARGE_POWER, 0) //
						.input(GRID_ACTIVE_POWER, -5000) //
						.input("evcs0", ACTIVE_POWER, 0) //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 5000) //
						.output(AWAITING_HYSTERESIS, false)) //
				.deactivate();
	}
}
