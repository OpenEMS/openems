package io.openems.edge.evcs.cluster;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.filter.DisabledRampFilter;
import io.openems.edge.common.filter.RampFilter;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.evcs.api.ChargeState;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.test.DummyEvcsPower;
import io.openems.edge.evcs.test.DummyManagedEvcs;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class EvcsClusterPeakShavingImplTest {
	// TODO: Add eventually something like DummyEvcsController

	private static final DummyManagedSymmetricEss ESS = new DummyManagedSymmetricEss("ess0") //
			.withMaxApparentPower(30000);
	private static final DummyElectricityMeter METER = new DummyElectricityMeter("meter0");
	private static final DummyEvcsPower EVCS_POWER = new DummyEvcsPower(new DisabledRampFilter());
	private static final DummyManagedEvcs EVCS0 = new DummyManagedEvcs("evcs0", EVCS_POWER);
	private static final DummyManagedEvcs EVCS1 = new DummyManagedEvcs("evcs1", EVCS_POWER);
	private static final DummyManagedEvcs EVCS2 = new DummyManagedEvcs("evcs2", EVCS_POWER);
	private static final DummyManagedEvcs EVCS3 = new DummyManagedEvcs("evcs3", EVCS_POWER);
	private static final DummyManagedEvcs EVCS4 = new DummyManagedEvcs("evcs4", EVCS_POWER);

	private static final DummyEvcsPower EVCS_POWER_WITH_FILTER = new DummyEvcsPower(new RampFilter());
	private static final DummyManagedEvcs EVCS5 = new DummyManagedEvcs("evcs5", EVCS_POWER_WITH_FILTER);

	private static final int HARDWARE_POWER_LIMIT_PER_PHASE = 7000;

	private static final ChannelAddress SUM_ESS_ACTIVE_POWER = new ChannelAddress("_sum", "EssActivePower");
	private static final ChannelAddress METER_GRID_ACTIVE_POWER = new ChannelAddress("meter0", "ActivePower");
	private static final ChannelAddress METER_GRID_ACTIVE_POWER_L1 = new ChannelAddress("meter0", "ActivePowerL1");
	private static final ChannelAddress METER_GRID_ACTIVE_POWER_L2 = new ChannelAddress("meter0", "ActivePowerL2");
	private static final ChannelAddress METER_GRID_ACTIVE_POWER_L3 = new ChannelAddress("meter0", "ActivePowerL3");
	private static final ChannelAddress ESS_ALLOWED_DISCHARGE_POWER = new ChannelAddress("ess0",
			"AllowedDischargePower");

	private static final ChannelAddress EVCS_CLUSTER_MAXIMUM_POWER_TO_DISTRIBUTE = new ChannelAddress("evcsCluster0",
			"MaximumPowerToDistribute");
	private static final ChannelAddress EVCS_CLUSTER_STATUS = new ChannelAddress("evcsCluster0", "EvcsClusterStatus");

	private static final ChannelAddress EVCS0_STATUS = new ChannelAddress("evcs0", "Status");
	private static final ChannelAddress EVCS0_CHARGE_POWER = new ChannelAddress("evcs0", "ChargePower");
	private static final ChannelAddress EVCS0_MAXIMUM_POWER = new ChannelAddress("evcs0", "MaximumPower");
	private static final ChannelAddress EVCS0_SET_POWER_REQUEST = new ChannelAddress("evcs0", "SetChargePowerRequest");
	private static final ChannelAddress EVCS0_SET_CHARGE_POWER_LIMIT = new ChannelAddress("evcs0",
			"SetChargePowerLimit");
	private static final ChannelAddress EVCS0_MAXIMUM_HARDWARE_POWER = new ChannelAddress("evcs0",
			"MaximumHardwarePower");
	private static final ChannelAddress EVCS0_MINIMUM_HARDWARE_POWER = new ChannelAddress("evcs0",
			"MinimumHardwarePower");
	private static final ChannelAddress EVCS0_CHARE_STATE = new ChannelAddress("evcs0", "ChargeState");

	private static final ChannelAddress EVCS1_STATUS = new ChannelAddress("evcs1", "Status");
	private static final ChannelAddress EVCS1_CHARGE_POWER = new ChannelAddress("evcs1", "ChargePower");
	private static final ChannelAddress EVCS1_MAXIMUM_POWER = new ChannelAddress("evcs1", "MaximumPower");
	private static final ChannelAddress EVCS1_SET_POWER_REQUEST = new ChannelAddress("evcs1", "SetChargePowerRequest");
	private static final ChannelAddress EVCS1_SET_CHARGE_POWER_LIMIT = new ChannelAddress("evcs1",
			"SetChargePowerLimit");
	private static final ChannelAddress EVCS1_MAXIMUM_HARDWARE_POWER = new ChannelAddress("evcs1",
			"MaximumHardwarePower");
	private static final ChannelAddress EVCS1_MINIMUM_HARDWARE_POWER = new ChannelAddress("evcs1",
			"MinimumHardwarePower");
	private static final ChannelAddress EVCS1_CHARGE_STATE = new ChannelAddress("evcs1", "ChargeState");

	private static final ChannelAddress EVCS2_STATUS = new ChannelAddress("evcs2", "Status");
	private static final ChannelAddress EVCS2_SET_POWER_REQUEST = new ChannelAddress("evcs2", "SetChargePowerRequest");
	private static final ChannelAddress EVCS2_CHARGE_STATE = new ChannelAddress("evcs2", "ChargeState");

	private static final ChannelAddress EVCS3_STATUS = new ChannelAddress("evcs3", "Status");
	private static final ChannelAddress EVCS3_SET_POWER_REQUEST = new ChannelAddress("evcs3", "SetChargePowerRequest");
	private static final ChannelAddress EVCS3_CHARGE_STATE = new ChannelAddress("evcs3", "ChargeState");

	private static final ChannelAddress EVCS4_STATUS = new ChannelAddress("evcs4", "Status");
	private static final ChannelAddress EVCS4_SET_POWER_REQUEST = new ChannelAddress("evcs4", "SetChargePowerRequest");

	private static final ChannelAddress EVCS5_STATUS = new ChannelAddress("evcs5", "Status");
	private static final ChannelAddress EVCS5_CHARGE_POWER = new ChannelAddress("evcs5", "ChargePower");
	private static final ChannelAddress EVCS5_SET_POWER_REQUEST = new ChannelAddress("evcs5", "SetChargePowerRequest");
	private static final ChannelAddress EVCS5_SET_CHARGE_POWER_LIMIT = new ChannelAddress("evcs5",
			"SetChargePowerLimit");
	private static final ChannelAddress EVCS5_MAXIMUM_HARDWARE_POWER = new ChannelAddress("evcs5",
			"MaximumHardwarePower");
	private static final ChannelAddress EVCS5_CHARGE_STATE = new ChannelAddress("evcs5", "ChargeState");

	@Test
	public void clusterMaximum_essActivePowerTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1" };

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
						.setEssId(ESS.id()) //
						.setMeterId(METER.id()) //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds(evcsIds) //
						.build()) //
				.next(new TestCase() //
						.input(EVCS0_CHARE_STATE, ChargeState.CHARGING) //
						.input(SUM_ESS_ACTIVE_POWER, 0) //
						.input(METER_GRID_ACTIVE_POWER, 0) //
						.input(METER_GRID_ACTIVE_POWER_L1, 0) //
						.input(METER_GRID_ACTIVE_POWER_L2, 0) //
						.input(METER_GRID_ACTIVE_POWER_L3, 0) //
						.input(ESS_ALLOWED_DISCHARGE_POWER, 0) //
						.output(EVCS_CLUSTER_MAXIMUM_POWER_TO_DISTRIBUTE, 21000)) //
				.next(new TestCase() //
						.input(SUM_ESS_ACTIVE_POWER, -5000) //
						.output(EVCS_CLUSTER_MAXIMUM_POWER_TO_DISTRIBUTE, 26000)) //
				.next(new TestCase() //
						.input(SUM_ESS_ACTIVE_POWER, 6000) //
						.output(EVCS_CLUSTER_MAXIMUM_POWER_TO_DISTRIBUTE, 15000)) //
		;
	}

	@Test
	public void clusterMaximum_symmetricGridPowerTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1" };

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
						.setEssId(ESS.id()) //
						.setMeterId(METER.id()) //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds(evcsIds) //
						.build()) //
				.next(new TestCase() //
						.input(EVCS0_CHARE_STATE, ChargeState.CHARGING) //
						.input(METER_GRID_ACTIVE_POWER, -6000) //
						.input(METER_GRID_ACTIVE_POWER_L1, -2000) //
						.input(METER_GRID_ACTIVE_POWER_L2, -2000) //
						.input(METER_GRID_ACTIVE_POWER_L3, -2000) //
						.input(ESS_ALLOWED_DISCHARGE_POWER, 0)) //
				.next(new TestCase() //
						.output(EVCS_CLUSTER_MAXIMUM_POWER_TO_DISTRIBUTE, 27000)) //
				.next(new TestCase() //
						.input(METER_GRID_ACTIVE_POWER, 4500) //
						.input(METER_GRID_ACTIVE_POWER_L1, 1500) //
						.input(METER_GRID_ACTIVE_POWER_L2, 1500) //
						.input(METER_GRID_ACTIVE_POWER_L3, 1500) //
						.input(ESS_ALLOWED_DISCHARGE_POWER, 0) //
						.output(EVCS_CLUSTER_MAXIMUM_POWER_TO_DISTRIBUTE, 16500)) //
		;
	}

	@Test
	public void clusterMaximum_assymmetricGridPowerTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1" };

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
						.setEssId(ESS.id()) //
						.setMeterId(METER.id()) //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds(evcsIds) //
						.build()) //
				.next(new TestCase() //
						.input(EVCS0_CHARE_STATE, ChargeState.CHARGING) //
						.input(METER_GRID_ACTIVE_POWER, -4000) //
						.input(METER_GRID_ACTIVE_POWER_L1, -2000) //
						.input(METER_GRID_ACTIVE_POWER_L2, -1000) //
						.input(METER_GRID_ACTIVE_POWER_L3, -1000) //
						.input(ESS_ALLOWED_DISCHARGE_POWER, 0) //
						.output(EVCS_CLUSTER_MAXIMUM_POWER_TO_DISTRIBUTE, 24000)) //
				.next(new TestCase() //
						.input(METER_GRID_ACTIVE_POWER, 4500) //
						.input(METER_GRID_ACTIVE_POWER_L1, 3000) //
						.input(METER_GRID_ACTIVE_POWER_L2, 1500) //
						.input(METER_GRID_ACTIVE_POWER_L3, 500) //
						.input(ESS_ALLOWED_DISCHARGE_POWER, 0) //
						.output(EVCS_CLUSTER_MAXIMUM_POWER_TO_DISTRIBUTE, 12000)) //
		;
	}

	@Test
	public void clusterMaximum_symmetricGridPower_essActivePowerTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1" };

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
						.setEssId(ESS.id()) //
						.setMeterId(METER.id()) //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds(evcsIds) //
						.build()) //
				.next(new TestCase() //
						.input(EVCS0_CHARE_STATE, ChargeState.CHARGING) //
						.input(METER_GRID_ACTIVE_POWER, -6000) //
						.input(METER_GRID_ACTIVE_POWER_L1, -2000) //
						.input(METER_GRID_ACTIVE_POWER_L2, -2000) //
						.input(METER_GRID_ACTIVE_POWER_L3, -2000) //
						.input(SUM_ESS_ACTIVE_POWER, -6000) //
						.input(ESS_ALLOWED_DISCHARGE_POWER, 0) //
						.output(EVCS_CLUSTER_MAXIMUM_POWER_TO_DISTRIBUTE, 33000)) //
				.next(new TestCase() //
						.input(METER_GRID_ACTIVE_POWER, 4500) //
						.input(METER_GRID_ACTIVE_POWER_L1, 1500) //
						.input(METER_GRID_ACTIVE_POWER_L2, 1500) //
						.input(METER_GRID_ACTIVE_POWER_L3, 1500) //
						.input(SUM_ESS_ACTIVE_POWER, 3000) //
						.input(ESS_ALLOWED_DISCHARGE_POWER, 0) //
						.output(EVCS_CLUSTER_MAXIMUM_POWER_TO_DISTRIBUTE, 13500)) //
		;
	}

	@Test
	public void clusterMaximum_essAllowedDischargePowerTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1" };

		var sut = new EvcsClusterPeakShavingImpl();
		var test = new ComponentTest(sut) //
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
						.setEssId(ESS.id()) //
						.setMeterId(METER.id()) //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds(evcsIds) //
						.build()); //
		test//
				.next(new TestCase() //
						.input(EVCS0_CHARE_STATE, ChargeState.CHARGING) //
						.input(METER_GRID_ACTIVE_POWER, -6000) //
						.input(METER_GRID_ACTIVE_POWER_L1, -2000) //
						.input(METER_GRID_ACTIVE_POWER_L2, -2000) //
						.input(METER_GRID_ACTIVE_POWER_L3, -2000) //
						.input(SUM_ESS_ACTIVE_POWER, -6000) //
						.input(ESS_ALLOWED_DISCHARGE_POWER, 10000) //
						.onBeforeControllersCallbacks(() -> sut.run()) //
						.output(EVCS_CLUSTER_MAXIMUM_POWER_TO_DISTRIBUTE, 63000)) //
				.next(new TestCase() //
						.input(METER_GRID_ACTIVE_POWER, 4500) //
						.input(METER_GRID_ACTIVE_POWER_L1, 1500) //
						.input(METER_GRID_ACTIVE_POWER_L2, 1500) //
						.input(METER_GRID_ACTIVE_POWER_L3, 1500) //
						.input(SUM_ESS_ACTIVE_POWER, 3000) //
						.input(ESS_ALLOWED_DISCHARGE_POWER, 20000) //
						.onBeforeControllersCallbacks(() -> sut.run()) //
						.output(EVCS_CLUSTER_MAXIMUM_POWER_TO_DISTRIBUTE, 43500)) //
		;
	}

	@Test
	public void clusterDistribution_nothingToChargeTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1" };

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
						.setEssId(ESS.id()) //
						.setMeterId(METER.id()) //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds(evcsIds) //
						.build()) //
				.next(new TestCase() //
						.input(EVCS0_CHARE_STATE, ChargeState.CHARGING)) //
				.next(new TestCase() //
						.input(SUM_ESS_ACTIVE_POWER, 0) //
						.input(METER_GRID_ACTIVE_POWER, 0) //
						.input(METER_GRID_ACTIVE_POWER_L1, 0) //
						.input(METER_GRID_ACTIVE_POWER_L2, 0) //
						.input(METER_GRID_ACTIVE_POWER_L3, 0) //
						.input(ESS_ALLOWED_DISCHARGE_POWER, 0) //
						.input(EVCS0_SET_POWER_REQUEST, 0) //
						.output(EVCS0_SET_CHARGE_POWER_LIMIT, 0)) //
				.next(new TestCase() //
						.input(SUM_ESS_ACTIVE_POWER, 0) //
						.input(METER_GRID_ACTIVE_POWER, 21000) //
						.input(METER_GRID_ACTIVE_POWER_L1, 7000) //
						.input(METER_GRID_ACTIVE_POWER_L2, 7000) //
						.input(METER_GRID_ACTIVE_POWER_L3, 7000) //
						.input(EVCS0_SET_POWER_REQUEST, 15000) //
						.input(ESS_ALLOWED_DISCHARGE_POWER, 0) //
						.output(EVCS_CLUSTER_MAXIMUM_POWER_TO_DISTRIBUTE, 0) //
						.output(EVCS0_SET_CHARGE_POWER_LIMIT, 0)) //
				.next(new TestCase() //
						.input(SUM_ESS_ACTIVE_POWER, 0) //
						.input(METER_GRID_ACTIVE_POWER, 15000) //
						.input(METER_GRID_ACTIVE_POWER_L1, 5000) //
						.input(METER_GRID_ACTIVE_POWER_L2, 5000) //
						.input(METER_GRID_ACTIVE_POWER_L3, 5000) //
						.input(EVCS0_SET_POWER_REQUEST, 15000) //
						.input(EVCS1_SET_POWER_REQUEST, 15000) //
						.input(EVCS0_MAXIMUM_POWER, 22000) //
						.input(EVCS1_MAXIMUM_POWER, 22000) //
						.input(EVCS0_MINIMUM_HARDWARE_POWER, 4500) //
						.input(EVCS1_MINIMUM_HARDWARE_POWER, 4500) //
						.input(EVCS0_MAXIMUM_HARDWARE_POWER, 22000) //
						.input(EVCS1_MAXIMUM_HARDWARE_POWER, 22000) //
						.input(EVCS0_STATUS, Status.CHARGING) //
						.input(EVCS1_STATUS, Status.CHARGING) //
						.input(ESS_ALLOWED_DISCHARGE_POWER, 0) //
						.output(EVCS_CLUSTER_MAXIMUM_POWER_TO_DISTRIBUTE, 6000) //
						.output(EVCS0_SET_CHARGE_POWER_LIMIT, 6000) //
						.output(EVCS1_SET_CHARGE_POWER_LIMIT, 0)) //
		;
	}

	@Test
	public void clusterDistribution_chargeTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1", "evcs2", "evcs3", "evcs4" };

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
						.setEssId(ESS.id()) //
						.setMeterId(METER.id()) //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds(evcsIds) //
						.build()) //
				.next(new TestCase() //
						.input(EVCS0_CHARE_STATE, ChargeState.CHARGING) //
						.input(SUM_ESS_ACTIVE_POWER, 0) //
						.input(METER_GRID_ACTIVE_POWER, 0) //
						.input(METER_GRID_ACTIVE_POWER_L1, 0) //
						.input(METER_GRID_ACTIVE_POWER_L2, 0) //
						.input(METER_GRID_ACTIVE_POWER_L3, 0) //
						.input(ESS_ALLOWED_DISCHARGE_POWER, 30000) //
						.input(EVCS0_SET_POWER_REQUEST, 15000) //
						.input(EVCS1_SET_POWER_REQUEST, 15000) //
						.input(EVCS2_SET_POWER_REQUEST, 15000) //
						.input(EVCS3_SET_POWER_REQUEST, 15000) //
						.input(EVCS4_SET_POWER_REQUEST, 15000) //
						.input(EVCS0_STATUS, Status.CHARGING) //
						.input(EVCS1_STATUS, Status.CHARGING) //
						.input(EVCS2_STATUS, Status.CHARGING) //
						.input(EVCS3_STATUS, Status.CHARGING) //
						.input(EVCS4_STATUS, Status.CHARGING) //
						.input(EVCS0_MAXIMUM_POWER, null) //
						.input(EVCS1_MAXIMUM_POWER, null) //
						.input(EVCS0_MAXIMUM_HARDWARE_POWER, 22000) //
						.input(EVCS1_MAXIMUM_HARDWARE_POWER, 22000) //
						.input(EVCS0_CHARGE_POWER, 0) //
						.input(EVCS1_CHARGE_POWER, 0)) //
		;
	}

	@Test
	public void clusterDistribution_chargeTest2() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1" };

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
						.setEssId(ESS.id()) //
						.setMeterId(METER.id()) //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds(evcsIds) //
						.build()) //
				.next(new TestCase() //
						.input(EVCS0_CHARE_STATE, ChargeState.CHARGING) //
						.input(SUM_ESS_ACTIVE_POWER, 0) //
						.input(METER_GRID_ACTIVE_POWER, 0) //
						.input(METER_GRID_ACTIVE_POWER_L1, 0) //
						.input(METER_GRID_ACTIVE_POWER_L2, 0) //
						.input(METER_GRID_ACTIVE_POWER_L3, 0) //
						.input(ESS_ALLOWED_DISCHARGE_POWER, 0) //
						.input(EVCS0_SET_POWER_REQUEST, 15000) //
						.input(EVCS1_SET_POWER_REQUEST, 15000) //
						// TODO: The charge power of an EVCS has to be checked if it really charges
						// this amount)
						.input(EVCS0_CHARGE_POWER, 11000) //
						.input(EVCS1_CHARGE_POWER, 22000) //
						.input(EVCS0_MAXIMUM_POWER, 22000) //
						.input(EVCS1_MAXIMUM_POWER, 22000) //
						.input(EVCS0_MAXIMUM_HARDWARE_POWER, 22000) //
						.input(EVCS1_MAXIMUM_HARDWARE_POWER, 22000) //
						.input(EVCS0_STATUS, Status.CHARGING) //
						.input(EVCS1_STATUS, Status.CHARGING)) //
				.next(new TestCase() //
						.output(EVCS_CLUSTER_MAXIMUM_POWER_TO_DISTRIBUTE, 54000) //
						.output(EVCS0_SET_CHARGE_POWER_LIMIT, 15000) //
						.output(EVCS1_SET_CHARGE_POWER_LIMIT, 15000)) //
		;
	}

	@Test
	public void clusterDistribution_chargeTest_maximumHardwarePowerTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1" };

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
						.setEssId(ESS.id()) //
						.setMeterId(METER.id()) //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds(evcsIds) //
						.build()) //
				.next(new TestCase() //
						.input(EVCS0_CHARE_STATE, ChargeState.CHARGING) //
						.input(SUM_ESS_ACTIVE_POWER, 0) //
						.input(METER_GRID_ACTIVE_POWER, 0) //
						.input(METER_GRID_ACTIVE_POWER_L1, 0) //
						.input(METER_GRID_ACTIVE_POWER_L2, 0) //
						.input(METER_GRID_ACTIVE_POWER_L3, 0) //
						.input(ESS_ALLOWED_DISCHARGE_POWER, 0) //
						.input(EVCS0_SET_POWER_REQUEST, 15000) //
						.input(EVCS1_SET_POWER_REQUEST, 15000) //
						.input(EVCS0_CHARGE_POWER, 11000) //
						.input(EVCS1_CHARGE_POWER, 0) //
						.input(EVCS0_MAXIMUM_POWER, null) //
						.input(EVCS1_MAXIMUM_POWER, null) //
						.input(EVCS0_MAXIMUM_HARDWARE_POWER, 11000) //
						.input(EVCS1_MAXIMUM_HARDWARE_POWER, 11000) //
						.input(EVCS0_STATUS, Status.CHARGING) //
						.input(EVCS1_STATUS, Status.CHARGING)) //
				.next(new TestCase() //
						.output(EVCS_CLUSTER_MAXIMUM_POWER_TO_DISTRIBUTE, 32000) //
						.output(EVCS0_SET_CHARGE_POWER_LIMIT, 11000) //
						.output(EVCS1_SET_CHARGE_POWER_LIMIT, 11000)) //
		;
	}

	@Test
	public void clusterDistribution_chargeTest_maximumPowerTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1" };

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
						.setEssId(ESS.id()) //
						.setMeterId(METER.id()) //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds(evcsIds) //
						.build()) //
				.next(new TestCase() //
						.input(EVCS0_CHARE_STATE, ChargeState.CHARGING) //
						.input(SUM_ESS_ACTIVE_POWER, 0) //
						.input(METER_GRID_ACTIVE_POWER, 0) //
						.input(METER_GRID_ACTIVE_POWER_L1, 0) //
						.input(METER_GRID_ACTIVE_POWER_L2, 0) //
						.input(METER_GRID_ACTIVE_POWER_L3, 0) //
						.input(ESS_ALLOWED_DISCHARGE_POWER, 0) //
						.input(EVCS0_SET_POWER_REQUEST, 15000) //
						.input(EVCS1_SET_POWER_REQUEST, 15000) //
						.input(EVCS0_CHARGE_POWER, 0) //
						.input(EVCS1_CHARGE_POWER, 0) //
						.input(EVCS0_MAXIMUM_POWER, 5000) //
						.input(EVCS1_MAXIMUM_POWER, 9000) //
						.input(EVCS0_MAXIMUM_HARDWARE_POWER, 22000) //
						.input(EVCS1_MAXIMUM_HARDWARE_POWER, 22000) //
						.input(EVCS0_STATUS, Status.CHARGING) //
						.input(EVCS1_STATUS, Status.CHARGING)) //
				.next(new TestCase() //
						.output(EVCS_CLUSTER_MAXIMUM_POWER_TO_DISTRIBUTE, 21000) //
						.output(EVCS0_SET_CHARGE_POWER_LIMIT, 15000) //
						.output(EVCS1_SET_CHARGE_POWER_LIMIT, 15000)) //
		;
	}

	@Test
	public void clusterDistribution_chargeTest_minimumPowerTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1" };

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
						.setEssId(ESS.id()) //
						.setMeterId(METER.id()) //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds(evcsIds) //
						.build()) //
				.next(new TestCase() //
						.input(EVCS0_CHARE_STATE, ChargeState.CHARGING) //
						.input(SUM_ESS_ACTIVE_POWER, 0) //
						.input(METER_GRID_ACTIVE_POWER, 0) //
						.input(METER_GRID_ACTIVE_POWER_L1, 0) //
						.input(METER_GRID_ACTIVE_POWER_L2, 0) //
						.input(METER_GRID_ACTIVE_POWER_L3, 0) //
						.input(ESS_ALLOWED_DISCHARGE_POWER, 0) //
						.input(EVCS0_SET_POWER_REQUEST, 15000) //
						.input(EVCS1_SET_POWER_REQUEST, 15000) //
						.input(EVCS0_CHARGE_POWER, 0) //
						.input(EVCS1_CHARGE_POWER, 0) //
						.input(EVCS0_MAXIMUM_POWER, 5000) //
						.input(EVCS1_MAXIMUM_POWER, 9000) //
						.input(EVCS0_MAXIMUM_HARDWARE_POWER, 22000) //
						.input(EVCS1_MAXIMUM_HARDWARE_POWER, 22000) //
						.input(EVCS0_MINIMUM_HARDWARE_POWER, 4500) //
						.input(EVCS1_MINIMUM_HARDWARE_POWER, 4500) //
						.input(EVCS0_STATUS, Status.READY_FOR_CHARGING) //
						.input(EVCS1_STATUS, Status.READY_FOR_CHARGING)) //
				.next(new TestCase() //
						.output(EVCS_CLUSTER_MAXIMUM_POWER_TO_DISTRIBUTE, 21000) //
						.output(EVCS0_SET_CHARGE_POWER_LIMIT, 4500) //
						.output(EVCS1_SET_CHARGE_POWER_LIMIT, 4500)) //
				.next(new TestCase() //
						.input(EVCS0_MINIMUM_HARDWARE_POWER, 9000) //
						.input(EVCS1_MINIMUM_HARDWARE_POWER, 6900) //
						.output(EVCS0_SET_CHARGE_POWER_LIMIT, 9000) //
						.output(EVCS1_SET_CHARGE_POWER_LIMIT, 6900)) //
		;
	}

	@Test
	public void clusterDistribution_filterTest() throws Exception {
		String[] evcsIds = { "evcs5" };

		int initialPowerFromCluster = 4500;

		new ComponentTest(new EvcsClusterPeakShavingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("sum", new DummySum()) //
				.addReference("addEvcs", EVCS5) //
				.addReference("meter", METER) //
				.addReference("ess", ESS) //
				.activate(MyConfig.create() //
						.setEssId(ESS.id()) //
						.setMeterId(METER.id()) //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds(evcsIds) //
						.build()) //
				.next(new TestCase() //
						.input(SUM_ESS_ACTIVE_POWER, 0) //
						.input(METER_GRID_ACTIVE_POWER, 0) //
						.input(METER_GRID_ACTIVE_POWER_L1, 0) //
						.input(METER_GRID_ACTIVE_POWER_L2, 0) //
						.input(METER_GRID_ACTIVE_POWER_L3, 0) //
						.input(ESS_ALLOWED_DISCHARGE_POWER, 0) //
						.input(EVCS5_SET_POWER_REQUEST, 22000) //
						.input(EVCS5_CHARGE_POWER, 0) //
						.input(EVCS5_CHARGE_STATE, ChargeState.NOT_CHARGING) //
						.input(EVCS5_MAXIMUM_HARDWARE_POWER, 22080) //
						.input(EVCS5_STATUS, Status.READY_FOR_CHARGING) //
						.input(EVCS_CLUSTER_STATUS, EvcsClusterStatus.REGULAR)) //
				.next(new TestCase() //
						// Cannot test charge states of evcs because the WriteHandler is not triggered
						// in a Cluster test.
						// Would expect Increasing
						// .output(evcs5ChargeState, ChargeState.INCREASING)) //
						// .output(evcs5ChargeState, ChargeState.INCREASING) //
						// .output(evcsClusterStatus, EvcsClusterStatus.INCREASING)) //
						.output(EVCS_CLUSTER_MAXIMUM_POWER_TO_DISTRIBUTE, 21000) //
						.output(EVCS5_SET_CHARGE_POWER_LIMIT, initialPowerFromCluster) //
				);
	}

	@Test
	public void clusterStatusTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1", "evcs2", "evcs3" };

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
						.setEssId(ESS.id()) //
						.setMeterId(METER.id()) //
						.setHardwarePowerLimit(HARDWARE_POWER_LIMIT_PER_PHASE) //
						.setEvcsIds(evcsIds) //
						.build()) //
				.next(new TestCase() //
						.input(EVCS0_CHARE_STATE, ChargeState.UNDEFINED) //
						.input(EVCS1_CHARGE_STATE, ChargeState.UNDEFINED) //
						.input(EVCS2_CHARGE_STATE, ChargeState.UNDEFINED) //
						.input(EVCS3_CHARGE_STATE, ChargeState.UNDEFINED)) //
				.next(new TestCase() //
						.output(EVCS_CLUSTER_STATUS, EvcsClusterStatus.UNDEFINED)) //
				.next(new TestCase() //
						.input(EVCS0_CHARE_STATE, ChargeState.INCREASING) //
						.input(EVCS1_CHARGE_STATE, ChargeState.UNDEFINED) //
						.input(EVCS2_CHARGE_STATE, ChargeState.UNDEFINED) //
						.input(EVCS3_CHARGE_STATE, ChargeState.UNDEFINED)) //
				.next(new TestCase() //
						.output(EVCS_CLUSTER_STATUS, EvcsClusterStatus.INCREASING)) //
				.next(new TestCase() //
						.input(EVCS0_CHARE_STATE, ChargeState.CHARGING) //
						.input(EVCS1_CHARGE_STATE, ChargeState.UNDEFINED) //
						.input(EVCS2_CHARGE_STATE, ChargeState.UNDEFINED) //
						.input(EVCS3_CHARGE_STATE, ChargeState.UNDEFINED)) //
				.next(new TestCase() //
						.output(EVCS_CLUSTER_STATUS, EvcsClusterStatus.REGULAR)) //
				.next(new TestCase() //
						.input(EVCS0_CHARE_STATE, ChargeState.CHARGING) //
						.input(EVCS1_CHARGE_STATE, ChargeState.DECREASING) //
						.input(EVCS2_CHARGE_STATE, ChargeState.INCREASING) //
						.input(EVCS3_CHARGE_STATE, ChargeState.UNDEFINED)) //
				.next(new TestCase() //
						.output(EVCS_CLUSTER_STATUS, EvcsClusterStatus.DECREASING)) //
				.next(new TestCase() //
						.input(EVCS0_CHARE_STATE, ChargeState.CHARGING) //
						.input(EVCS1_CHARGE_STATE, ChargeState.CHARGING) //
						.input(EVCS2_CHARGE_STATE, ChargeState.CHARGING) //
						.input(EVCS3_CHARGE_STATE, ChargeState.CHARGING)) //
				.next(new TestCase() //
						.output(EVCS_CLUSTER_STATUS, EvcsClusterStatus.REGULAR)) //
		;
	}
}
