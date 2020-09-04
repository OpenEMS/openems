package io.openems.edge.controller.evcs;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.filter.DisabledRampFilter;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.test.DummyEvcsPower;
import io.openems.edge.evcs.test.DummyManagedEvcs;

public class EvcsControllerTest {

	private static final DummyPower POWER = new DummyPower(30000);
	private static final DummyManagedSymmetricEss ESS = new DummyManagedSymmetricEss("ess0", POWER);
	private static final DummyEvcsPower EVCS_POWER = new DummyEvcsPower(new DisabledRampFilter());
	private static final DummyManagedEvcs EVCS = new DummyManagedEvcs("evcs0", EVCS_POWER);

	private static String EVCS_ID = EVCS.id();
	private static boolean ENABLE_CHARGING;
	private static ChargeMode CHARGE_MODE;
	private static int FORCE_CHARGE_MIN_POWER = 7360;
	private static int DEFAULT_CHARGE_MIN_POWER = 0;
	private static Priority PRIORITY = Priority.CAR;
	private static String ESS_ID = "ess0";
	private static int ENERGY_SESSION_LIMIT = 0;

	private static ChannelAddress sumGridActivePower = new ChannelAddress("_sum", "GridActivePower");
	private static ChannelAddress sumEssActivePower = new ChannelAddress("_sum", "EssActivePower");
	private static ChannelAddress evcs0ChargePower = new ChannelAddress("evcs0", "ChargePower");
	private static ChannelAddress evcs0SetChargePowerLimit = new ChannelAddress("evcs0", "SetChargePowerLimit");
	private static ChannelAddress essAllowedChargePower = new ChannelAddress("ess0", "AllowedChargePower");
	private static ChannelAddress evcs0MaximumPower = new ChannelAddress("evcs0", "MaximumPower");
	private static ChannelAddress evcs0IsClustered = new ChannelAddress("evcs0", "IsClustered");
	private static ChannelAddress evcs0SetPowerRequest = new ChannelAddress("evcs0", "SetChargePowerRequest");
	private static ChannelAddress evcs0Status = new ChannelAddress("evcs0", "Status");
	private static ChannelAddress evcs0MaximumHardwarePower = new ChannelAddress("evcs0", "MaximumHardwarePower");
	private static ChannelAddress evcs0DefaultChargeMinPower = new ChannelAddress("ctrlEvcs0", "_PropertyDefaultChargeMinPower");
	private static ChannelAddress evcs0ForceChargeMinPower = new ChannelAddress("ctrlEvcs0", "_PropertyForceChargeMinPower");
	

	@Test
	public void excessChargeTest1() throws Exception {

		ENABLE_CHARGING = true;
		CHARGE_MODE = ChargeMode.EXCESS_POWER;
		PRIORITY = Priority.CAR;
		
		new ControllerTest(new EvcsController()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", EVCS) //
				.addReference("ess", ESS) //
				.activate(MyConfig.create() //
						.setEvcsId(EVCS_ID) //
						.setEnableCharging(ENABLE_CHARGING) //
						.setChargeMode(CHARGE_MODE) //
						.setForceChargeMinPower(FORCE_CHARGE_MIN_POWER) //
						.setDefaultChargeMinPower(DEFAULT_CHARGE_MIN_POWER) //
						.setPriority(PRIORITY) //
						.setEssId(ESS_ID) //
						.setEnergySessionLimit(ENERGY_SESSION_LIMIT) //
						.build()) //
				.next(new TestCase().input(sumEssActivePower, -6000) //
						.input(evcs0IsClustered, false) //
						.input(sumGridActivePower, 0) //
						.input(evcs0ChargePower, 0) //
						.output(evcs0SetChargePowerLimit, 6000)) //
		;
	}

	@Test
	public void excessChargeTest2() throws Exception {

		ENABLE_CHARGING = true;
		CHARGE_MODE = ChargeMode.EXCESS_POWER;
		PRIORITY = Priority.STORAGE;

		new ControllerTest(new EvcsController()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", EVCS) //
				.addReference("ess", ESS) //
				.activate(MyConfig.create() //
						.setEvcsId(EVCS_ID) //
						.setEnableCharging(ENABLE_CHARGING) //
						.setChargeMode(CHARGE_MODE) //
						.setForceChargeMinPower(FORCE_CHARGE_MIN_POWER) //
						.setDefaultChargeMinPower(DEFAULT_CHARGE_MIN_POWER) //
						.setPriority(PRIORITY) //
						.setEssId(ESS_ID) //
						.setEnergySessionLimit(ENERGY_SESSION_LIMIT) //
						.build()) //
				.next(new TestCase() //
						.input(sumEssActivePower, -5000) //
						.input(evcs0IsClustered, false) //
						.input(sumGridActivePower, -40000) //
						.input(evcs0ChargePower, 5000) //
						.input(essAllowedChargePower, 30000) //
						.output(evcs0SetChargePowerLimit, 20000))

		;
	}
	
	@Test
	public void forceChargeTest() throws Exception {

		ENABLE_CHARGING = true;
		FORCE_CHARGE_MIN_POWER = 7360;
		CHARGE_MODE = ChargeMode.FORCE_CHARGE;
		PRIORITY = Priority.CAR;
		
		new ControllerTest(new EvcsController()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", EVCS) //
				.addReference("ess", ESS) //
				.activate(MyConfig.create() //
						.setEvcsId(EVCS_ID) //
						.setEnableCharging(ENABLE_CHARGING) //
						.setChargeMode(CHARGE_MODE) //
						.setForceChargeMinPower(FORCE_CHARGE_MIN_POWER) //
						.setDefaultChargeMinPower(DEFAULT_CHARGE_MIN_POWER) //
						.setPriority(PRIORITY) //
						.setEssId(ESS_ID) //
						.setEnergySessionLimit(ENERGY_SESSION_LIMIT) //
						.build()) //
				.next(new TestCase() //
						.input(sumEssActivePower, -5000) //
						.input(evcs0IsClustered, false) //
						.input(sumGridActivePower, -40000) //
						.input(evcs0ChargePower, 5000) //
						.input(essAllowedChargePower, 30000) //
						.output(evcs0SetChargePowerLimit, 22080))
		;
	}

	@Test
	public void chargingDisabledTest() throws Exception {

		ENABLE_CHARGING = false;
		
		new ControllerTest(new EvcsController()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", EVCS) //
				.addReference("ess", ESS) //
				.activate(MyConfig.create() //
						.setEvcsId(EVCS_ID) //
						.setEnableCharging(ENABLE_CHARGING) //
						.setChargeMode(CHARGE_MODE) //
						.setForceChargeMinPower(FORCE_CHARGE_MIN_POWER) //
						.setDefaultChargeMinPower(DEFAULT_CHARGE_MIN_POWER) //
						.setPriority(PRIORITY) //
						.setEssId(ESS_ID) //
						.setEnergySessionLimit(ENERGY_SESSION_LIMIT) //
						.build()) //
				.next(new TestCase() //
						.output(evcs0SetChargePowerLimit, 0))
		;
	}
	
	@Test
	public void wrongConfigParametersTest() throws Exception {
		
		DEFAULT_CHARGE_MIN_POWER = 30000;
		FORCE_CHARGE_MIN_POWER = 30000;
		
		new ControllerTest(new EvcsController()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", EVCS) //
				.addReference("ess", ESS) //
				.activate(MyConfig.create() //
						.setEvcsId(EVCS_ID) //
						.setEnableCharging(ENABLE_CHARGING) //
						.setChargeMode(CHARGE_MODE) //
						.setForceChargeMinPower(FORCE_CHARGE_MIN_POWER) //
						.setDefaultChargeMinPower(DEFAULT_CHARGE_MIN_POWER) //
						.setPriority(PRIORITY) //
						.setEssId(ESS_ID) //
						.setEnergySessionLimit(ENERGY_SESSION_LIMIT) //
						.build()) //
				.next(new TestCase() //
						.input(evcs0MaximumHardwarePower, 11000)
						.output(evcs0DefaultChargeMinPower, 11000)
						.output(evcs0ForceChargeMinPower, 11000))
		;
	}


	@Test
	public void clusterTest() throws Exception {

		ENABLE_CHARGING = true;
		FORCE_CHARGE_MIN_POWER = 3333;
		CHARGE_MODE = ChargeMode.EXCESS_POWER;
		PRIORITY = Priority.CAR;

		new ControllerTest(new EvcsController()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("evcs", EVCS) //
				.addReference("ess", ESS) //
				.activate(MyConfig.create() //
						.setEvcsId(EVCS_ID) //
						.setEnableCharging(ENABLE_CHARGING) //
						.setChargeMode(CHARGE_MODE) //
						.setForceChargeMinPower(FORCE_CHARGE_MIN_POWER) //
						.setDefaultChargeMinPower(DEFAULT_CHARGE_MIN_POWER) //
						.setPriority(PRIORITY) //
						.setEssId(ESS_ID) //
						.setEnergySessionLimit(ENERGY_SESSION_LIMIT) //
						.build()) //
				.next(new TestCase() //
						.input(sumEssActivePower, -10000) //
						.input(evcs0IsClustered, true) //
						.input(sumGridActivePower, 0) //
						.input(evcs0ChargePower, 0) //
						.input(evcs0Status, Status.CHARGING) //
						.output(evcs0SetPowerRequest, 10000))
				.next(new TestCase().input(sumEssActivePower, -6000) //
						.input(evcs0IsClustered, true) //
						.input(sumGridActivePower, 0) //
						.input(evcs0ChargePower, 0) //
						.input(evcs0Status, Status.NOT_READY_FOR_CHARGING) //
						.output(evcs0SetPowerRequest, 0)) //
				.next(new TestCase().input(sumEssActivePower, -6000) //
						.input(evcs0IsClustered, true) //
						.input(sumGridActivePower, 0) //
						.input(evcs0ChargePower, 0) //
						.input(evcs0Status, null) //
						.output(evcs0SetPowerRequest, 0)) //
				.next(new TestCase().input(sumEssActivePower, -6000) //
						.input(evcs0IsClustered, true) //
						.input(sumGridActivePower, 0) //
						.input(evcs0ChargePower, 0) //
						.input(evcs0Status, Status.CHARGING_REJECTED) //
						.output(evcs0SetPowerRequest, 6000) //
						.output(evcs0MaximumPower, null)) //
		;
	}
}
