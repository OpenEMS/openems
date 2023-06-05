package io.openems.edge.controller.evcs;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.filter.DisabledRampFilter;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.evcs.api.ChargeMode;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.test.DummyEvcsPower;
import io.openems.edge.evcs.test.DummyManagedEvcs;

public class EvcsControllerImplTest {

	private static final DummyEvcsPower EVCS_POWER = new DummyEvcsPower(new DisabledRampFilter());
	private static final DummyManagedEvcs EVCS = new DummyManagedEvcs("evcs0", EVCS_POWER);

	private static final String EVCS_ID = EVCS.id();
	private static final boolean DEFAULT_ENABLE_CHARGING = true;
	private static final ChargeMode DEFAULT_CHARGE_MODE = ChargeMode.EXCESS_POWER;
	private static final int DEFAULT_FORCE_CHARGE_MIN_POWER = 7360;
	private static final int DEFAULT_CHARGE_MIN_POWER = 0;
	private static final Priority DEFAULT_PRIORITY = Priority.CAR;
	private static final int DEFAULT_ENERGY_SESSION_LIMIT = 0;

	private static ChannelAddress sumGridActivePower = new ChannelAddress("_sum", "GridActivePower");
	private static ChannelAddress sumEssDischargePower = new ChannelAddress("_sum", "EssDischargePower");
	private static ChannelAddress sumEssSoc = new ChannelAddress("_sum", "EssSoc");
	private static ChannelAddress evcs0ChargePower = new ChannelAddress("evcs0", "ChargePower");
	private static ChannelAddress evcs0SetChargePowerLimit = new ChannelAddress("evcs0", "SetChargePowerLimit");
	private static ChannelAddress evcs0MaximumPower = new ChannelAddress("evcs0", "MaximumPower");
	private static ChannelAddress evcs0IsClustered = new ChannelAddress("evcs0", "IsClustered");
	private static ChannelAddress evcs0SetPowerRequest = new ChannelAddress("evcs0", "SetChargePowerRequest");
	private static ChannelAddress evcs0Status = new ChannelAddress("evcs0", "Status");
	private static ChannelAddress evcs0MaximumHardwarePower = new ChannelAddress("evcs0", "MaximumHardwarePower");

	@Test
	public void excessChargeTest1() throws Exception {

		new ControllerTest(new ControllerEvcsImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", EVCS) //
				.activate(MyConfig.create() //
						.setId("ctrlEvcs0") //
						.setEvcsId(EVCS_ID) //
						.setEnableCharging(DEFAULT_ENABLE_CHARGING) //
						.setChargeMode(DEFAULT_CHARGE_MODE) //
						.setForceChargeMinPower(DEFAULT_FORCE_CHARGE_MIN_POWER) //
						.setDefaultChargeMinPower(DEFAULT_CHARGE_MIN_POWER) //
						.setPriority(DEFAULT_PRIORITY) //
						.setEnergySessionLimit(DEFAULT_ENERGY_SESSION_LIMIT) //
						.build()) //
				.next(new TestCase() //
						.input(sumEssDischargePower, -6000) //
						.input(evcs0IsClustered, false) //
						.input(sumGridActivePower, 0) //
						.input(evcs0ChargePower, 0) //
						.output(evcs0SetChargePowerLimit, 6000)) //
		;
	}

	@Test
	public void excessChargeTest2() throws Exception {

		final var test = new ControllerTest(new ControllerEvcsImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", EVCS) //
				.activate(MyConfig.create() //
						.setId("ctrlEvcs0") //
						.setEvcsId(EVCS_ID) //
						.setEnableCharging(DEFAULT_ENABLE_CHARGING) //
						.setChargeMode(DEFAULT_CHARGE_MODE) //
						.setForceChargeMinPower(DEFAULT_FORCE_CHARGE_MIN_POWER) //
						.setDefaultChargeMinPower(DEFAULT_CHARGE_MIN_POWER) //
						.setPriority(Priority.STORAGE) //
						.setEnergySessionLimit(DEFAULT_ENERGY_SESSION_LIMIT) //
						.build()); //

		test.next(new TestCase() //
				.input(sumEssSoc, 50) //
				.input(sumEssDischargePower, -5000) //
				.input(evcs0IsClustered, false) //
				.input(sumGridActivePower, -40000) //
				.input(evcs0ChargePower, 5000) //
				.input(evcs0MaximumHardwarePower, 22080) //
				.output(evcs0SetChargePowerLimit, 44800));
	}

	@Test
	public void forceChargeTest() throws Exception {

		new ControllerTest(new ControllerEvcsImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", EVCS) //
				.activate(MyConfig.create() //
						.setId("ctrlEvcs0") //
						.setEvcsId(EVCS_ID) //
						.setEnableCharging(DEFAULT_ENABLE_CHARGING) //
						.setChargeMode(ChargeMode.FORCE_CHARGE) //
						.setForceChargeMinPower(DEFAULT_FORCE_CHARGE_MIN_POWER) //
						.setDefaultChargeMinPower(DEFAULT_CHARGE_MIN_POWER) //
						.setPriority(DEFAULT_PRIORITY) //
						.setEnergySessionLimit(DEFAULT_ENERGY_SESSION_LIMIT) //
						.build()) //
				.next(new TestCase() //
						.input(sumEssDischargePower, -5000) //
						.input(evcs0IsClustered, false) //
						.input(sumGridActivePower, -40000) //
						.input(evcs0ChargePower, 5000) //
						.output(evcs0SetChargePowerLimit, 22080));
	}

	@Test
	public void chargingDisabledTest() throws Exception {

		new ControllerTest(new ControllerEvcsImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", EVCS) //
				.activate(MyConfig.create() //
						.setId("ctrlEvcs0") //
						.setEvcsId(EVCS_ID) //
						.setEnableCharging(false) //
						.setChargeMode(DEFAULT_CHARGE_MODE) //
						.setForceChargeMinPower(DEFAULT_FORCE_CHARGE_MIN_POWER) //
						.setDefaultChargeMinPower(DEFAULT_CHARGE_MIN_POWER) //
						.setPriority(DEFAULT_PRIORITY) //
						.setEnergySessionLimit(DEFAULT_ENERGY_SESSION_LIMIT) //
						.build()) //
				.next(new TestCase() //
						.output(evcs0SetChargePowerLimit, 0));
	}

	@Test
	public void wrongConfigParametersTest() throws Exception {

		var cm = new DummyConfigurationAdmin();
		new ControllerTest(new ControllerEvcsImpl()) //
				.addReference("cm", cm) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", EVCS) //
				.activate(MyConfig.create() //
						.setId("ctrlEvcs0") //
						.setEvcsId(EVCS_ID) //
						.setEnableCharging(DEFAULT_ENABLE_CHARGING) //
						.setChargeMode(DEFAULT_CHARGE_MODE) //
						.setForceChargeMinPower(30_000) //
						.setDefaultChargeMinPower(30_000) //
						.setPriority(DEFAULT_PRIORITY) //
						.setEnergySessionLimit(DEFAULT_ENERGY_SESSION_LIMIT) //
						.build()) //
				.next(new TestCase() //
						.input(evcs0MaximumHardwarePower, 12000));

		assertEquals(12000,
				(int) (Integer) cm.getConfiguration("ctrlEvcs0").getProperties().get("defaultChargeMinPower"));
	}

	@Test
	public void clusterTest() throws Exception {

		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);

		new ControllerTest(new ControllerEvcsImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", EVCS) //
				.activate(MyConfig.create() //
						.setId("ctrlEvcs0") //
						.setEvcsId(EVCS_ID) //
						.setEnableCharging(DEFAULT_ENABLE_CHARGING) //
						.setChargeMode(DEFAULT_CHARGE_MODE) //
						.setForceChargeMinPower(3_333) //
						.setDefaultChargeMinPower(DEFAULT_CHARGE_MIN_POWER) //
						.setPriority(DEFAULT_PRIORITY) //
						.setEnergySessionLimit(DEFAULT_ENERGY_SESSION_LIMIT) //
						.build()) //
				.next(new TestCase() //
						.input(sumEssDischargePower, -10000) //
						.input(evcs0IsClustered, true) //
						.input(sumGridActivePower, 0) //
						.input(evcs0ChargePower, 0) //
						.input(evcs0Status, Status.CHARGING) //
						.output(evcs0SetPowerRequest, 10000))
				.next(new TestCase() //
						.input(sumEssDischargePower, -6000) //
						.input(evcs0IsClustered, true) //
						.input(sumGridActivePower, 0) //
						.input(evcs0ChargePower, 0) //
						.input(evcs0Status, Status.NOT_READY_FOR_CHARGING) //
						.output(evcs0SetPowerRequest, 0)) //
				.next(new TestCase() //
						.input(sumEssDischargePower, -6000) //
						.input(evcs0IsClustered, true) //
						.input(sumGridActivePower, 0) //
						.input(evcs0ChargePower, 0) //
						.input(evcs0Status, null) //
						.output(evcs0SetPowerRequest, 0)) //
				.next(new TestCase() //
						.input(sumEssDischargePower, -6000) //
						.input(evcs0IsClustered, true) //
						.input(sumGridActivePower, 0) //
						.input(evcs0ChargePower, 0) //
						.input(evcs0Status, Status.CHARGING_REJECTED) //
						.output(evcs0SetPowerRequest, 6000) //
						.output(evcs0MaximumPower, null)) //
		;
	}

	@Test
	public void clusterTestDisabledCharging() throws Exception {

		new ControllerTest(new ControllerEvcsImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", EVCS) //
				.activate(MyConfig.create() //
						.setId("ctrlEvcs0") //
						.setEvcsId(EVCS_ID) //
						.setEnableCharging(false) //
						.setChargeMode(DEFAULT_CHARGE_MODE) //
						.setForceChargeMinPower(3_333) //
						.setDefaultChargeMinPower(DEFAULT_CHARGE_MIN_POWER) //
						.setPriority(DEFAULT_PRIORITY) //
						.setEnergySessionLimit(DEFAULT_ENERGY_SESSION_LIMIT) //
						.build()) //
				.next(new TestCase() //
						.input(sumEssDischargePower, -10000) //
						.input(evcs0IsClustered, true) //
						.input(sumGridActivePower, 0) //
						.input(evcs0ChargePower, 0) //
						.input(evcs0Status, Status.CHARGING) //
						.output(evcs0SetChargePowerLimit, 0))
				.next(new TestCase() //
						.input(sumEssDischargePower, -6000) //
						.input(evcs0IsClustered, true) //
						.input(sumGridActivePower, 0) //
						.input(evcs0ChargePower, 0) //
						.input(evcs0Status, Status.NOT_READY_FOR_CHARGING) //
						.output(evcs0SetChargePowerLimit, 0)) //
				.next(new TestCase() //
						.input(sumEssDischargePower, -6000) //
						.input(evcs0IsClustered, true) //
						.input(sumGridActivePower, 0) //
						.input(evcs0ChargePower, 0) //
						.input(evcs0Status, null) //
						.output(evcs0SetChargePowerLimit, 0)) //
				.next(new TestCase() //
						.input(sumEssDischargePower, -6000) //
						.input(evcs0IsClustered, true) //
						.input(sumGridActivePower, 0) //
						.input(evcs0ChargePower, 0) //
						.input(evcs0Status, Status.CHARGING_REJECTED) //
						.output(evcs0SetChargePowerLimit, 0) //
						.output(evcs0MaximumPower, null)) //
		;
	}
}
