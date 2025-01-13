package io.openems.edge.evcs.cluster;

import static io.openems.edge.common.sum.Sum.ChannelId.ESS_ACTIVE_POWER;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER;
import static io.openems.edge.evcs.api.Evcs.ChannelId.MAXIMUM_HARDWARE_POWER;
import static io.openems.edge.evcs.api.Evcs.ChannelId.MAXIMUM_POWER;
import static io.openems.edge.evcs.api.Evcs.ChannelId.MINIMUM_HARDWARE_POWER;
import static io.openems.edge.evcs.api.Evcs.ChannelId.STATUS;
import static io.openems.edge.evcs.api.ManagedEvcs.ChannelId.CHARGE_STATE;
import static io.openems.edge.evcs.api.ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT;
import static io.openems.edge.evcs.api.ManagedEvcs.ChannelId.SET_CHARGE_POWER_REQUEST;
import static io.openems.edge.evcs.cluster.EvcsClusterPeakShaving.ChannelId.EVCS_CLUSTER_STATUS;
import static io.openems.edge.evcs.cluster.EvcsClusterPeakShaving.ChannelId.MAXIMUM_POWER_TO_DISTRIBUTE;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L1;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L2;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L3;

import org.junit.Test;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.evcs.api.ChargeState;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.test.DummyManagedEvcs;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class EvcsClusterPeakShavingImplTest {
	// TODO: Add eventually something like DummyEvcsController

	private static final DummyManagedSymmetricEss ESS = new DummyManagedSymmetricEss("ess0") //
			.withMaxApparentPower(30000);
	private static final DummyElectricityMeter METER = new DummyElectricityMeter("meter0");
	private static final DummyManagedEvcs EVCS0 = DummyManagedEvcs.ofDisabled("evcs0");
	private static final DummyManagedEvcs EVCS1 = DummyManagedEvcs.ofDisabled("evcs1");
	private static final DummyManagedEvcs EVCS2 = DummyManagedEvcs.ofDisabled("evcs2");
	private static final DummyManagedEvcs EVCS3 = DummyManagedEvcs.ofDisabled("evcs3");
	private static final DummyManagedEvcs EVCS4 = DummyManagedEvcs.ofDisabled("evcs4");
	private static final DummyManagedEvcs EVCS5 = DummyManagedEvcs.ofDisabled("evcs5");

	private static final int HARDWARE_POWER_LIMIT_PER_PHASE = 7000;

	@Test
	public void clusterMaximum_essActivePowerTest() throws Exception {
		new ComponentTest(new EvcsClusterPeakShavingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("addEvcs", EVCS0) //
				.addReference("addEvcs", EVCS1) //
				.addReference("meter", METER) //
				.addReference("ess", ESS) //
				.activate(MyConfig.create() //
						.setId("evcsCluster0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds("evcs0", "evcs1") //
						.build()) //
				.next(new TestCase() //
						.input("evcs0", CHARGE_STATE, ChargeState.CHARGING) //
						.input(ESS_ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER_L1, 0) //
						.input("meter0", ACTIVE_POWER_L2, 0) //
						.input("meter0", ACTIVE_POWER_L3, 0) //
						.input("ess0", ALLOWED_DISCHARGE_POWER, 0) //
						.output("evcsCluster0", MAXIMUM_POWER_TO_DISTRIBUTE, 21000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, -5000) //
						.output("evcsCluster0", MAXIMUM_POWER_TO_DISTRIBUTE, 26000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 6000) //
						.output("evcsCluster0", MAXIMUM_POWER_TO_DISTRIBUTE, 15000)) //
		;
	}

	@Test
	public void clusterMaximum_symmetricGridPowerTest() throws Exception {
		new ComponentTest(new EvcsClusterPeakShavingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("addEvcs", EVCS0) //
				.addReference("addEvcs", EVCS1) //
				.addReference("meter", METER) //
				.addReference("ess", ESS) //
				.activate(MyConfig.create() //
						.setId("evcsCluster0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds("evcs0", "evcs1") //
						.build()) //
				.next(new TestCase() //
						.input("evcs0", CHARGE_STATE, ChargeState.CHARGING) //
						.input("meter0", ACTIVE_POWER, -6000) //
						.input("meter0", ACTIVE_POWER_L1, -2000) //
						.input("meter0", ACTIVE_POWER_L2, -2000) //
						.input("meter0", ACTIVE_POWER_L3, -2000) //
						.input("ess0", ALLOWED_DISCHARGE_POWER, 0)) //
				.next(new TestCase() //
						.output("evcsCluster0", MAXIMUM_POWER_TO_DISTRIBUTE, 27000)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, 4500) //
						.input("meter0", ACTIVE_POWER_L1, 1500) //
						.input("meter0", ACTIVE_POWER_L2, 1500) //
						.input("meter0", ACTIVE_POWER_L3, 1500) //
						.input("ess0", ALLOWED_DISCHARGE_POWER, 0) //
						.output("evcsCluster0", MAXIMUM_POWER_TO_DISTRIBUTE, 16500)) //
		;
	}

	@Test
	public void clusterMaximum_assymmetricGridPowerTest() throws Exception {
		new ComponentTest(new EvcsClusterPeakShavingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("addEvcs", EVCS0) //
				.addReference("addEvcs", EVCS1) //
				.addReference("meter", METER) //
				.addReference("ess", ESS) //
				.activate(MyConfig.create() //
						.setId("evcsCluster0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds("evcs0", "evcs1") //
						.build()) //
				.next(new TestCase() //
						.input("evcs0", CHARGE_STATE, ChargeState.CHARGING) //
						.input("meter0", ACTIVE_POWER, -4000) //
						.input("meter0", ACTIVE_POWER_L1, -2000) //
						.input("meter0", ACTIVE_POWER_L2, -1000) //
						.input("meter0", ACTIVE_POWER_L3, -1000) //
						.input("ess0", ALLOWED_DISCHARGE_POWER, 0) //
						.output("evcsCluster0", MAXIMUM_POWER_TO_DISTRIBUTE, 24000)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, 4500) //
						.input("meter0", ACTIVE_POWER_L1, 3000) //
						.input("meter0", ACTIVE_POWER_L2, 1500) //
						.input("meter0", ACTIVE_POWER_L3, 500) //
						.input("ess0", ALLOWED_DISCHARGE_POWER, 0) //
						.output("evcsCluster0", MAXIMUM_POWER_TO_DISTRIBUTE, 12000)) //
		;
	}

	@Test
	public void clusterMaximum_symmetricGridPower_essActivePowerTest() throws Exception {
		new ComponentTest(new EvcsClusterPeakShavingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("addEvcs", EVCS0) //
				.addReference("addEvcs", EVCS1) //
				.addReference("meter", METER) //
				.addReference("ess", ESS) //
				.activate(MyConfig.create() //
						.setId("evcsCluster0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds("evcs0", "evcs1") //
						.build()) //
				.next(new TestCase() //
						.input("evcs0", CHARGE_STATE, ChargeState.CHARGING) //
						.input("meter0", ACTIVE_POWER, -6000) //
						.input("meter0", ACTIVE_POWER_L1, -2000) //
						.input("meter0", ACTIVE_POWER_L2, -2000) //
						.input("meter0", ACTIVE_POWER_L3, -2000) //
						.input(ESS_ACTIVE_POWER, -6000) //
						.input("ess0", ALLOWED_DISCHARGE_POWER, 0) //
						.output("evcsCluster0", MAXIMUM_POWER_TO_DISTRIBUTE, 33000)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, 4500) //
						.input("meter0", ACTIVE_POWER_L1, 1500) //
						.input("meter0", ACTIVE_POWER_L2, 1500) //
						.input("meter0", ACTIVE_POWER_L3, 1500) //
						.input(ESS_ACTIVE_POWER, 3000) //
						.input("ess0", ALLOWED_DISCHARGE_POWER, 0) //
						.output("evcsCluster0", MAXIMUM_POWER_TO_DISTRIBUTE, 13500)) //
		;
	}

	@Test
	public void clusterMaximum_essAllowedDischargePowerTest() throws Exception {
		var sut = new EvcsClusterPeakShavingImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("addEvcs", EVCS0) //
				.addReference("addEvcs", EVCS1) //
				.addReference("meter", METER) //
				.addReference("ess", ESS) //
				.activate(MyConfig.create() //
						.setId("evcsCluster0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds("evcs0", "evcs1") //
						.build()) //
				.next(new TestCase() //
						.input("evcs0", CHARGE_STATE, ChargeState.CHARGING) //
						.input("meter0", ACTIVE_POWER, -6000) //
						.input("meter0", ACTIVE_POWER_L1, -2000) //
						.input("meter0", ACTIVE_POWER_L2, -2000) //
						.input("meter0", ACTIVE_POWER_L3, -2000) //
						.input(ESS_ACTIVE_POWER, -6000) //
						.input("ess0", ALLOWED_DISCHARGE_POWER, 10000) //
						.onBeforeControllersCallbacks(() -> sut.run()) //
						.output("evcsCluster0", MAXIMUM_POWER_TO_DISTRIBUTE, 63000)) //
				.next(new TestCase() //
						.input("meter0", ACTIVE_POWER, 4500) //
						.input("meter0", ACTIVE_POWER_L1, 1500) //
						.input("meter0", ACTIVE_POWER_L2, 1500) //
						.input("meter0", ACTIVE_POWER_L3, 1500) //
						.input(ESS_ACTIVE_POWER, 3000) //
						.input("ess0", ALLOWED_DISCHARGE_POWER, 20000) //
						.onBeforeControllersCallbacks(() -> sut.run()) //
						.output("evcsCluster0", MAXIMUM_POWER_TO_DISTRIBUTE, 43500)) //
		;
	}

	@Test
	public void clusterDistribution_nothingToChargeTest() throws Exception {
		new ComponentTest(new EvcsClusterPeakShavingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("addEvcs", EVCS0) //
				.addReference("addEvcs", EVCS1) //
				.addReference("meter", METER) //
				.addReference("ess", ESS) //
				.activate(MyConfig.create() //
						.setId("evcsCluster0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds("evcs0", "evcs1") //
						.build()) //
				.next(new TestCase() //
						.input("evcs0", CHARGE_STATE, ChargeState.CHARGING)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER_L1, 0) //
						.input("meter0", ACTIVE_POWER_L2, 0) //
						.input("meter0", ACTIVE_POWER_L3, 0) //
						.input("ess0", ALLOWED_DISCHARGE_POWER, 0) //
						.input("evcs0", SET_CHARGE_POWER_REQUEST, 0) //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 0)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER, 21000) //
						.input("meter0", ACTIVE_POWER_L1, 7000) //
						.input("meter0", ACTIVE_POWER_L2, 7000) //
						.input("meter0", ACTIVE_POWER_L3, 7000) //
						.input("evcs0", SET_CHARGE_POWER_REQUEST, 15000) //
						.input("ess0", ALLOWED_DISCHARGE_POWER, 0) //
						.output("evcsCluster0", MAXIMUM_POWER_TO_DISTRIBUTE, 0) //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 0)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER, 15000) //
						.input("meter0", ACTIVE_POWER_L1, 5000) //
						.input("meter0", ACTIVE_POWER_L2, 5000) //
						.input("meter0", ACTIVE_POWER_L3, 5000) //
						.input("evcs0", SET_CHARGE_POWER_REQUEST, 15000) //
						.input("evcs1", SET_CHARGE_POWER_REQUEST, 15000) //
						.input("evcs0", MAXIMUM_POWER, 22000) //
						.input("evcs1", MAXIMUM_POWER, 22000) //
						.input("evcs0", MINIMUM_HARDWARE_POWER, 4500) //
						.input("evcs1", MINIMUM_HARDWARE_POWER, 4500) //
						.input("evcs0", MAXIMUM_HARDWARE_POWER, 22000) //
						.input("evcs1", MAXIMUM_HARDWARE_POWER, 22000) //
						.input("evcs0", STATUS, Status.CHARGING) //
						.input("evcs1", STATUS, Status.CHARGING) //
						.input("ess0", ALLOWED_DISCHARGE_POWER, 0) //
						.output("evcsCluster0", MAXIMUM_POWER_TO_DISTRIBUTE, 6000) //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 6000) //
						.output("evcs1", SET_CHARGE_POWER_LIMIT, 0)) //
		;
	}

	@Test
	public void clusterDistribution_chargeTest() throws Exception {
		new ComponentTest(new EvcsClusterPeakShavingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("addEvcs", EVCS0) //
				.addReference("addEvcs", EVCS1) //
				.addReference("addEvcs", EVCS2) //
				.addReference("addEvcs", EVCS3) //
				.addReference("addEvcs", EVCS4) //
				.addReference("meter", METER) //
				.addReference("ess", ESS) //
				.activate(MyConfig.create() //
						.setId("evcsCluster0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds("evcs0", "evcs1", "evcs2", "evcs3", "evcs4") //
						.build()) //
				.next(new TestCase() //
						.input("evcs0", CHARGE_STATE, ChargeState.CHARGING) //
						.input(ESS_ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER_L1, 0) //
						.input("meter0", ACTIVE_POWER_L2, 0) //
						.input("meter0", ACTIVE_POWER_L3, 0) //
						.input("ess0", ALLOWED_DISCHARGE_POWER, 30000) //
						.input("evcs0", SET_CHARGE_POWER_REQUEST, 15000) //
						.input("evcs1", SET_CHARGE_POWER_REQUEST, 15000) //
						.input("evcs2", SET_CHARGE_POWER_REQUEST, 15000) //
						.input("evcs3", SET_CHARGE_POWER_REQUEST, 15000) //
						.input("evcs4", SET_CHARGE_POWER_REQUEST, 15000) //
						.input("evcs0", STATUS, Status.CHARGING) //
						.input("evcs1", STATUS, Status.CHARGING) //
						.input("evcs2", STATUS, Status.CHARGING) //
						.input("evcs3", STATUS, Status.CHARGING) //
						.input("evcs4", STATUS, Status.CHARGING) //
						.input("evcs0", MAXIMUM_POWER, null) //
						.input("evcs1", MAXIMUM_POWER, null) //
						.input("evcs0", MAXIMUM_HARDWARE_POWER, 22000) //
						.input("evcs1", MAXIMUM_HARDWARE_POWER, 22000) //
						.input("evcs0", ACTIVE_POWER, 0) //
						.input("evcs1", ACTIVE_POWER, 0)) //
		;
	}

	@Test
	public void clusterDistribution_chargeTest2() throws Exception {
		new ComponentTest(new EvcsClusterPeakShavingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("addEvcs", EVCS0) //
				.addReference("addEvcs", EVCS1) //
				.addReference("meter", METER) //
				.addReference("ess", ESS) //
				.activate(MyConfig.create() //
						.setId("evcsCluster0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds("evcs0", "evcs1") //
						.build()) //
				.next(new TestCase() //
						.input("evcs0", CHARGE_STATE, ChargeState.CHARGING) //
						.input(ESS_ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER_L1, 0) //
						.input("meter0", ACTIVE_POWER_L2, 0) //
						.input("meter0", ACTIVE_POWER_L3, 0) //
						.input("ess0", ALLOWED_DISCHARGE_POWER, 0) //
						.input("evcs0", SET_CHARGE_POWER_REQUEST, 15000) //
						.input("evcs1", SET_CHARGE_POWER_REQUEST, 15000) //
						// TODO: The charge power of an EVCS has to be checked if it really charges
						// this amount)
						.input("evcs0", ACTIVE_POWER, 11000) //
						.input("evcs1", ACTIVE_POWER, 22000) //
						.input("evcs0", MAXIMUM_POWER, 22000) //
						.input("evcs1", MAXIMUM_POWER, 22000) //
						.input("evcs0", MAXIMUM_HARDWARE_POWER, 22000) //
						.input("evcs1", MAXIMUM_HARDWARE_POWER, 22000) //
						.input("evcs0", STATUS, Status.CHARGING) //
						.input("evcs1", STATUS, Status.CHARGING)) //
				.next(new TestCase() //
						.output("evcsCluster0", MAXIMUM_POWER_TO_DISTRIBUTE, 54000) //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 15000) //
						.output("evcs1", SET_CHARGE_POWER_LIMIT, 15000)) //
		;
	}

	@Test
	public void clusterDistribution_chargeTest_maximumHardwarePowerTest() throws Exception {
		new ComponentTest(new EvcsClusterPeakShavingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("addEvcs", EVCS0) //
				.addReference("addEvcs", EVCS1) //
				.addReference("meter", METER) //
				.addReference("ess", ESS) //
				.activate(MyConfig.create() //
						.setId("evcsCluster0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds("evcs0", "evcs1") //
						.build()) //
				.next(new TestCase() //
						.input("evcs0", CHARGE_STATE, ChargeState.CHARGING) //
						.input(ESS_ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER_L1, 0) //
						.input("meter0", ACTIVE_POWER_L2, 0) //
						.input("meter0", ACTIVE_POWER_L3, 0) //
						.input("ess0", ALLOWED_DISCHARGE_POWER, 0) //
						.input("evcs0", SET_CHARGE_POWER_REQUEST, 15000) //
						.input("evcs1", SET_CHARGE_POWER_REQUEST, 15000) //
						.input("evcs0", ACTIVE_POWER, 11000) //
						.input("evcs1", ACTIVE_POWER, 0) //
						.input("evcs0", MAXIMUM_POWER, null) //
						.input("evcs1", MAXIMUM_POWER, null) //
						.input("evcs0", MAXIMUM_HARDWARE_POWER, 11000) //
						.input("evcs1", MAXIMUM_HARDWARE_POWER, 11000) //
						.input("evcs0", STATUS, Status.CHARGING) //
						.input("evcs1", STATUS, Status.CHARGING)) //
				.next(new TestCase() //
						.output("evcsCluster0", MAXIMUM_POWER_TO_DISTRIBUTE, 32000) //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 11000) //
						.output("evcs1", SET_CHARGE_POWER_LIMIT, 11000)) //
		;
	}

	@Test
	public void clusterDistribution_chargeTest_maximumPowerTest() throws Exception {
		new ComponentTest(new EvcsClusterPeakShavingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("addEvcs", EVCS0) //
				.addReference("addEvcs", EVCS1) //
				.addReference("meter", METER) //
				.addReference("ess", ESS) //
				.activate(MyConfig.create() //
						.setId("evcsCluster0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds("evcs0", "evcs1") //
						.build()) //
				.next(new TestCase() //
						.input("evcs0", CHARGE_STATE, ChargeState.CHARGING) //
						.input(ESS_ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER_L1, 0) //
						.input("meter0", ACTIVE_POWER_L2, 0) //
						.input("meter0", ACTIVE_POWER_L3, 0) //
						.input("ess0", ALLOWED_DISCHARGE_POWER, 0) //
						.input("evcs0", SET_CHARGE_POWER_REQUEST, 15000) //
						.input("evcs1", SET_CHARGE_POWER_REQUEST, 15000) //
						.input("evcs0", ACTIVE_POWER, 0) //
						.input("evcs1", ACTIVE_POWER, 0) //
						.input("evcs0", MAXIMUM_POWER, 5000) //
						.input("evcs1", MAXIMUM_POWER, 9000) //
						.input("evcs0", MAXIMUM_HARDWARE_POWER, 22000) //
						.input("evcs1", MAXIMUM_HARDWARE_POWER, 22000) //
						.input("evcs0", STATUS, Status.CHARGING) //
						.input("evcs1", STATUS, Status.CHARGING)) //
				.next(new TestCase() //
						.output("evcsCluster0", MAXIMUM_POWER_TO_DISTRIBUTE, 21000) //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 15000) //
						.output("evcs1", SET_CHARGE_POWER_LIMIT, 15000)) //
		;
	}

	@Test
	public void clusterDistribution_chargeTest_minimumPowerTest() throws Exception {
		new ComponentTest(new EvcsClusterPeakShavingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("addEvcs", EVCS0) //
				.addReference("addEvcs", EVCS1) //
				.addReference("meter", METER) //
				.addReference("ess", ESS) //
				.activate(MyConfig.create() //
						.setId("evcsCluster0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds("evcs0", "evcs1") //
						.build()) //
				.next(new TestCase() //
						.input("evcs0", CHARGE_STATE, ChargeState.CHARGING) //
						.input(ESS_ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER_L1, 0) //
						.input("meter0", ACTIVE_POWER_L2, 0) //
						.input("meter0", ACTIVE_POWER_L3, 0) //
						.input("ess0", ALLOWED_DISCHARGE_POWER, 0) //
						.input("evcs0", SET_CHARGE_POWER_REQUEST, 15000) //
						.input("evcs1", SET_CHARGE_POWER_REQUEST, 15000) //
						.input("evcs0", ACTIVE_POWER, 0) //
						.input("evcs1", ACTIVE_POWER, 0) //
						.input("evcs0", MAXIMUM_POWER, 5000) //
						.input("evcs1", MAXIMUM_POWER, 9000) //
						.input("evcs0", MAXIMUM_HARDWARE_POWER, 22000) //
						.input("evcs1", MAXIMUM_HARDWARE_POWER, 22000) //
						.input("evcs0", MINIMUM_HARDWARE_POWER, 4500) //
						.input("evcs1", MINIMUM_HARDWARE_POWER, 4500) //
						.input("evcs0", STATUS, Status.READY_FOR_CHARGING) //
						.input("evcs1", STATUS, Status.READY_FOR_CHARGING)) //
				.next(new TestCase() //
						.output("evcsCluster0", MAXIMUM_POWER_TO_DISTRIBUTE, 21000) //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 4500) //
						.output("evcs1", SET_CHARGE_POWER_LIMIT, 4500)) //
				.next(new TestCase() //
						.input("evcs0", MINIMUM_HARDWARE_POWER, 9000) //
						.input("evcs1", MINIMUM_HARDWARE_POWER, 6900) //
						.output("evcs0", SET_CHARGE_POWER_LIMIT, 9000) //
						.output("evcs1", SET_CHARGE_POWER_LIMIT, 6900)) //
		;
	}

	@Test
	public void clusterDistribution_filterTest() throws Exception {
		new ComponentTest(new EvcsClusterPeakShavingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("addEvcs", EVCS5) //
				.addReference("meter", METER) //
				.addReference("ess", ESS) //
				.activate(MyConfig.create() //
						.setId("evcsCluster0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds("evcs5") //
						.build()) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER_L1, 0) //
						.input("meter0", ACTIVE_POWER_L2, 0) //
						.input("meter0", ACTIVE_POWER_L3, 0) //
						.input("ess0", ALLOWED_DISCHARGE_POWER, 0) //
						.input("evcs5", SET_CHARGE_POWER_REQUEST, 22000) //
						.input("evcs5", ACTIVE_POWER, 0) //
						.input("evcs5", CHARGE_STATE, ChargeState.NOT_CHARGING) //
						.input("evcs5", MAXIMUM_HARDWARE_POWER, 22080) //
						.input("evcs5", STATUS, Status.READY_FOR_CHARGING) //
						.input("evcsCluster0", EVCS_CLUSTER_STATUS, EvcsClusterStatus.REGULAR)) //
				.next(new TestCase() //
						// Cannot test charge states of evcs because the WriteHandler is not triggered
						// in a Cluster test.
						// Would expect Increasing
						// .output(evcs5ChargeState, ChargeState.INCREASING)) //
						// .output(evcs5ChargeState, ChargeState.INCREASING) //
						// .output(evcsClusterStatus, EvcsClusterStatus.INCREASING)) //
						.output("evcsCluster0", MAXIMUM_POWER_TO_DISTRIBUTE, 21000) //
						.output("evcs5", SET_CHARGE_POWER_LIMIT, 4500) //
				);
	}

	@Test
	public void clusterStatusTest() throws Exception {
		new ComponentTest(new EvcsClusterPeakShavingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("addEvcs", EVCS0) //
				.addReference("addEvcs", EVCS1) //
				.addReference("addEvcs", EVCS2) //
				.addReference("addEvcs", EVCS3) //
				.addReference("meter", METER) //
				.addReference("ess", ESS) //
				.activate(MyConfig.create() //
						.setId("evcsCluster0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds("evcs0", "evcs1", "evcs2", "evcs3") //
						.build()) //
				.next(new TestCase() //
						.input("evcs0", CHARGE_STATE, ChargeState.UNDEFINED) //
						.input("evcs1", CHARGE_STATE, ChargeState.UNDEFINED) //
						.input("evcs2", CHARGE_STATE, ChargeState.UNDEFINED) //
						.input("evcs3", CHARGE_STATE, ChargeState.UNDEFINED)) //
				.next(new TestCase() //
						.output("evcsCluster0", EVCS_CLUSTER_STATUS, EvcsClusterStatus.UNDEFINED)) //
				.next(new TestCase() //
						.input("evcs0", CHARGE_STATE, ChargeState.INCREASING) //
						.input("evcs1", CHARGE_STATE, ChargeState.UNDEFINED) //
						.input("evcs2", CHARGE_STATE, ChargeState.UNDEFINED) //
						.input("evcs3", CHARGE_STATE, ChargeState.UNDEFINED)) //
				.next(new TestCase() //
						.output("evcsCluster0", EVCS_CLUSTER_STATUS, EvcsClusterStatus.INCREASING)) //
				.next(new TestCase() //
						.input("evcs0", CHARGE_STATE, ChargeState.CHARGING) //
						.input("evcs1", CHARGE_STATE, ChargeState.UNDEFINED) //
						.input("evcs2", CHARGE_STATE, ChargeState.UNDEFINED) //
						.input("evcs3", CHARGE_STATE, ChargeState.UNDEFINED)) //
				.next(new TestCase() //
						.output("evcsCluster0", EVCS_CLUSTER_STATUS, EvcsClusterStatus.REGULAR)) //
				.next(new TestCase() //
						.input("evcs0", CHARGE_STATE, ChargeState.CHARGING) //
						.input("evcs1", CHARGE_STATE, ChargeState.DECREASING) //
						.input("evcs2", CHARGE_STATE, ChargeState.INCREASING) //
						.input("evcs3", CHARGE_STATE, ChargeState.UNDEFINED)) //
				.next(new TestCase() //
						.output("evcsCluster0", EVCS_CLUSTER_STATUS, EvcsClusterStatus.DECREASING)) //
				.next(new TestCase() //
						.input("evcs0", CHARGE_STATE, ChargeState.CHARGING) //
						.input("evcs1", CHARGE_STATE, ChargeState.CHARGING) //
						.input("evcs2", CHARGE_STATE, ChargeState.CHARGING) //
						.input("evcs3", CHARGE_STATE, ChargeState.CHARGING)) //
				.next(new TestCase() //
						.output("evcsCluster0", EVCS_CLUSTER_STATUS, EvcsClusterStatus.REGULAR)) //
		;
	}
}
