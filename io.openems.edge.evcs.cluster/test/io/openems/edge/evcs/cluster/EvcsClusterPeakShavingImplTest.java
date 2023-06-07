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
import io.openems.edge.meter.test.DummyAsymmetricMeter;

public class EvcsClusterPeakShavingImplTest {
	// TODO: Add eventually something like DummyEvcsController

	private static final DummyManagedSymmetricEss ESS = new DummyManagedSymmetricEss("ess0", 30000);
	private static final DummyAsymmetricMeter METER = new DummyAsymmetricMeter("meter0");
	private static final DummyEvcsPower EVCS_POWER = new DummyEvcsPower(new DisabledRampFilter());
	private static final DummyManagedEvcs EVCS0 = new DummyManagedEvcs("evcs0", EVCS_POWER);
	private static final DummyManagedEvcs EVCS1 = new DummyManagedEvcs("evcs1", EVCS_POWER);
	private static final DummyManagedEvcs EVCS2 = new DummyManagedEvcs("evcs2", EVCS_POWER);
	private static final DummyManagedEvcs EVCS3 = new DummyManagedEvcs("evcs3", EVCS_POWER);
	private static final DummyManagedEvcs EVCS4 = new DummyManagedEvcs("evcs4", EVCS_POWER);

	private static final DummyEvcsPower EVCS_POWER_WITH_FILTER = new DummyEvcsPower(new RampFilter());
	private static final DummyManagedEvcs EVCS5 = new DummyManagedEvcs("evcs5", EVCS_POWER_WITH_FILTER);

	private static int HARDWARE_POWER_LIMIT_PER_PHASE = 7000;
	private static String EVCS_TARGET;

	private static ChannelAddress sumEssActivePower = new ChannelAddress("_sum", "EssActivePower");
	private static ChannelAddress meterGridActivePower = new ChannelAddress("meter0", "ActivePower");
	private static ChannelAddress meterGridActivePowerL1 = new ChannelAddress("meter0", "ActivePowerL1");
	private static ChannelAddress meterGridActivePowerL2 = new ChannelAddress("meter0", "ActivePowerL2");
	private static ChannelAddress meterGridActivePowerL3 = new ChannelAddress("meter0", "ActivePowerL3");
	private static ChannelAddress essAllowedDischargePower = new ChannelAddress("ess0", "AllowedDischargePower");

	private static ChannelAddress evcsClusterMaximumPowerToDistribute = new ChannelAddress("evcsCluster0",
			"MaximumPowerToDistribute");
	private static ChannelAddress evcsClusterStatus = new ChannelAddress("evcsCluster0", "EvcsClusterStatus");

	private static ChannelAddress evcs0Status = new ChannelAddress("evcs0", "Status");
	private static ChannelAddress evcs0ChargePower = new ChannelAddress("evcs0", "ChargePower");
	private static ChannelAddress evcs0MaximumPower = new ChannelAddress("evcs0", "MaximumPower");
	private static ChannelAddress evcs0SetPowerRequest = new ChannelAddress("evcs0", "SetChargePowerRequest");
	private static ChannelAddress evcs0SetChargePowerLimit = new ChannelAddress("evcs0", "SetChargePowerLimit");
	private static ChannelAddress evcs0MaximumHardwarePower = new ChannelAddress("evcs0", "MaximumHardwarePower");
	private static ChannelAddress evcs0MinimumHardwarePower = new ChannelAddress("evcs0", "MinimumHardwarePower");
	private static ChannelAddress evcs0ChargeState = new ChannelAddress("evcs0", "ChargeState");

	private static ChannelAddress evcs1Status = new ChannelAddress("evcs1", "Status");
	private static ChannelAddress evcs1ChargePower = new ChannelAddress("evcs1", "ChargePower");
	private static ChannelAddress evcs1MaximumPower = new ChannelAddress("evcs1", "MaximumPower");
	private static ChannelAddress evcs1SetPowerRequest = new ChannelAddress("evcs1", "SetChargePowerRequest");
	private static ChannelAddress evcs1SetChargePowerLimit = new ChannelAddress("evcs1", "SetChargePowerLimit");
	private static ChannelAddress evcs1MaximumHardwarePower = new ChannelAddress("evcs1", "MaximumHardwarePower");
	private static ChannelAddress evcs1MinimumHardwarePower = new ChannelAddress("evcs1", "MinimumHardwarePower");
	private static ChannelAddress evcs1ChargeState = new ChannelAddress("evcs1", "ChargeState");

	private static ChannelAddress evcs2Status = new ChannelAddress("evcs2", "Status");
	private static ChannelAddress evcs2SetPowerRequest = new ChannelAddress("evcs2", "SetChargePowerRequest");
	private static ChannelAddress evcs2ChargeState = new ChannelAddress("evcs2", "ChargeState");

	private static ChannelAddress evcs3Status = new ChannelAddress("evcs3", "Status");
	private static ChannelAddress evcs3SetPowerRequest = new ChannelAddress("evcs3", "SetChargePowerRequest");
	private static ChannelAddress evcs3ChargeState = new ChannelAddress("evcs3", "ChargeState");

	private static ChannelAddress evcs4Status = new ChannelAddress("evcs4", "Status");
	private static ChannelAddress evcs4SetPowerRequest = new ChannelAddress("evcs4", "SetChargePowerRequest");

	private static ChannelAddress evcs5Status = new ChannelAddress("evcs5", "Status");
	private static ChannelAddress evcs5ChargePower = new ChannelAddress("evcs5", "ChargePower");
	private static ChannelAddress evcs5SetPowerRequest = new ChannelAddress("evcs5", "SetChargePowerRequest");
	private static ChannelAddress evcs5SetChargePowerLimit = new ChannelAddress("evcs5", "SetChargePowerLimit");
	private static ChannelAddress evcs5MaximumHardwarePower = new ChannelAddress("evcs5", "MaximumHardwarePower");
	private static ChannelAddress evcs5ChargeState = new ChannelAddress("evcs5", "ChargeState");

	@Test
	public void clusterMaximum_essActivePowerTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1" };
		EVCS_TARGET = this.getEvcsTarget(evcsIds);

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
						.setEvcsTarget(EVCS_TARGET) //
						.build()) //
				.next(new TestCase() //
						.input(evcs0ChargeState, ChargeState.CHARGING) //
						.input(sumEssActivePower, 0) //
						.input(meterGridActivePower, 0) //
						.input(meterGridActivePowerL1, 0) //
						.input(meterGridActivePowerL2, 0) //
						.input(meterGridActivePowerL3, 0) //
						.input(essAllowedDischargePower, 0) //
						.output(evcsClusterMaximumPowerToDistribute, 21000)) //
				.next(new TestCase() //
						.input(sumEssActivePower, -5000) //
						.output(evcsClusterMaximumPowerToDistribute, 26000)) //
				.next(new TestCase() //
						.input(sumEssActivePower, 6000) //
						.output(evcsClusterMaximumPowerToDistribute, 15000)) //
		;
	}

	@Test
	public void clusterMaximum_symmetricGridPowerTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1" };
		EVCS_TARGET = this.getEvcsTarget(evcsIds);

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
						.setEvcsTarget(EVCS_TARGET) //
						.build()) //
				.next(new TestCase() //
						.input(evcs0ChargeState, ChargeState.CHARGING) //
						.input(meterGridActivePower, -6000) //
						.input(meterGridActivePowerL1, -2000) //
						.input(meterGridActivePowerL2, -2000) //
						.input(meterGridActivePowerL3, -2000) //
						.input(essAllowedDischargePower, 0)) //
				.next(new TestCase() //
						.output(evcsClusterMaximumPowerToDistribute, 27000)) //
				.next(new TestCase() //
						.input(meterGridActivePower, 4500) //
						.input(meterGridActivePowerL1, 1500) //
						.input(meterGridActivePowerL2, 1500) //
						.input(meterGridActivePowerL3, 1500) //
						.input(essAllowedDischargePower, 0) //
						.output(evcsClusterMaximumPowerToDistribute, 16500)) //
		;
	}

	@Test
	public void clusterMaximum_assymmetricGridPowerTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1" };
		EVCS_TARGET = this.getEvcsTarget(evcsIds);

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
						.setEvcsTarget(EVCS_TARGET) //
						.build()) //
				.next(new TestCase() //
						.input(evcs0ChargeState, ChargeState.CHARGING) //
						.input(meterGridActivePower, -4000) //
						.input(meterGridActivePowerL1, -2000) //
						.input(meterGridActivePowerL2, -1000) //
						.input(meterGridActivePowerL3, -1000) //
						.input(essAllowedDischargePower, 0) //
						.output(evcsClusterMaximumPowerToDistribute, 24000)) //
				.next(new TestCase() //
						.input(meterGridActivePower, 4500) //
						.input(meterGridActivePowerL1, 3000) //
						.input(meterGridActivePowerL2, 1500) //
						.input(meterGridActivePowerL3, 500) //
						.input(essAllowedDischargePower, 0) //
						.output(evcsClusterMaximumPowerToDistribute, 12000)) //
		;
	}

	@Test
	public void clusterMaximum_symmetricGridPower_essActivePowerTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1" };
		EVCS_TARGET = this.getEvcsTarget(evcsIds);

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
						.setEvcsTarget(EVCS_TARGET) //
						.build()) //
				.next(new TestCase() //
						.input(evcs0ChargeState, ChargeState.CHARGING) //
						.input(meterGridActivePower, -6000) //
						.input(meterGridActivePowerL1, -2000) //
						.input(meterGridActivePowerL2, -2000) //
						.input(meterGridActivePowerL3, -2000) //
						.input(sumEssActivePower, -6000) //
						.input(essAllowedDischargePower, 0) //
						.output(evcsClusterMaximumPowerToDistribute, 33000)) //
				.next(new TestCase() //
						.input(meterGridActivePower, 4500) //
						.input(meterGridActivePowerL1, 1500) //
						.input(meterGridActivePowerL2, 1500) //
						.input(meterGridActivePowerL3, 1500) //
						.input(sumEssActivePower, 3000) //
						.input(essAllowedDischargePower, 0) //
						.output(evcsClusterMaximumPowerToDistribute, 13500)) //
		;
	}

	@Test
	public void clusterMaximum_essAllowedDischargePowerTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1" };
		EVCS_TARGET = this.getEvcsTarget(evcsIds);

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
						.setEvcsTarget(EVCS_TARGET) //
						.build()); //
		test//
				.next(new TestCase() //
						.input(evcs0ChargeState, ChargeState.CHARGING) //
						.input(meterGridActivePower, -6000) //
						.input(meterGridActivePowerL1, -2000) //
						.input(meterGridActivePowerL2, -2000) //
						.input(meterGridActivePowerL3, -2000) //
						.input(sumEssActivePower, -6000) //
						.input(essAllowedDischargePower, 10000) //
						.onBeforeControllersCallbacks(() -> sut.run()) //
						.output(evcsClusterMaximumPowerToDistribute, 43000)) //
				.next(new TestCase() //
						.input(meterGridActivePower, 4500) //
						.input(meterGridActivePowerL1, 1500) //
						.input(meterGridActivePowerL2, 1500) //
						.input(meterGridActivePowerL3, 1500) //
						.input(sumEssActivePower, 3000) //
						.input(essAllowedDischargePower, 20000) //
						.onBeforeControllersCallbacks(() -> sut.run()) //
						.output(evcsClusterMaximumPowerToDistribute, 33500)) //
		;
	}

	@Test
	public void clusterDistribution_nothingToChargeTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1" };
		EVCS_TARGET = this.getEvcsTarget(evcsIds);

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
						.setEvcsTarget(EVCS_TARGET) //
						.build()) //
				.next(new TestCase() //
						.input(evcs0ChargeState, ChargeState.CHARGING)) //
				.next(new TestCase() //
						.input(sumEssActivePower, 0) //
						.input(meterGridActivePower, 0) //
						.input(meterGridActivePowerL1, 0) //
						.input(meterGridActivePowerL2, 0) //
						.input(meterGridActivePowerL3, 0) //
						.input(essAllowedDischargePower, 0) //
						.input(evcs0SetPowerRequest, 0) //
						.output(evcs0SetChargePowerLimit, 0)) //
				.next(new TestCase() //
						.input(sumEssActivePower, 0) //
						.input(meterGridActivePower, 21000) //
						.input(meterGridActivePowerL1, 7000) //
						.input(meterGridActivePowerL2, 7000) //
						.input(meterGridActivePowerL3, 7000) //
						.input(evcs0SetPowerRequest, 15000) //
						.input(essAllowedDischargePower, 0) //
						.output(evcsClusterMaximumPowerToDistribute, 0) //
						.output(evcs0SetChargePowerLimit, 0)) //
				.next(new TestCase() //
						.input(sumEssActivePower, 0) //
						.input(meterGridActivePower, 15000) //
						.input(meterGridActivePowerL1, 5000) //
						.input(meterGridActivePowerL2, 5000) //
						.input(meterGridActivePowerL3, 5000) //
						.input(evcs0SetPowerRequest, 15000) //
						.input(evcs1SetPowerRequest, 15000) //
						.input(evcs0MaximumPower, 22000) //
						.input(evcs1MaximumPower, 22000) //
						.input(evcs0MinimumHardwarePower, 4500) //
						.input(evcs1MinimumHardwarePower, 4500) //
						.input(evcs0MaximumHardwarePower, 22000) //
						.input(evcs1MaximumHardwarePower, 22000) //
						.input(evcs0Status, Status.CHARGING) //
						.input(evcs1Status, Status.CHARGING) //
						.input(essAllowedDischargePower, 0) //
						.output(evcsClusterMaximumPowerToDistribute, 6000) //
						.output(evcs0SetChargePowerLimit, 6000) //
						.output(evcs1SetChargePowerLimit, 0)) //
		;
	}

	@Test
	public void clusterDistribution_chargeTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1", "evcs2", "evcs3", "evcs4" };
		EVCS_TARGET = this.getEvcsTarget(evcsIds);

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
						.setEvcsTarget(EVCS_TARGET) //
						.build()) //
				.next(new TestCase() //
						.input(evcs0ChargeState, ChargeState.CHARGING) //
						.input(sumEssActivePower, 0) //
						.input(meterGridActivePower, 0) //
						.input(meterGridActivePowerL1, 0) //
						.input(meterGridActivePowerL2, 0) //
						.input(meterGridActivePowerL3, 0) //
						.input(essAllowedDischargePower, 30000) //
						.input(evcs0SetPowerRequest, 15000) //
						.input(evcs1SetPowerRequest, 15000) //
						.input(evcs2SetPowerRequest, 15000) //
						.input(evcs3SetPowerRequest, 15000) //
						.input(evcs4SetPowerRequest, 15000) //
						.input(evcs0Status, Status.CHARGING) //
						.input(evcs1Status, Status.CHARGING) //
						.input(evcs2Status, Status.CHARGING) //
						.input(evcs3Status, Status.CHARGING) //
						.input(evcs4Status, Status.CHARGING) //
						.input(evcs0MaximumPower, null) //
						.input(evcs1MaximumPower, null) //
						.input(evcs0MaximumHardwarePower, 22000) //
						.input(evcs1MaximumHardwarePower, 22000) //
						.input(evcs0ChargePower, 0) //
						.input(evcs1ChargePower, 0)) //
		;
	}

	@Test
	public void clusterDistribution_chargeTest2() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1" };
		EVCS_TARGET = this.getEvcsTarget(evcsIds);

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
						.setEvcsTarget(EVCS_TARGET) //
						.build()) //
				.next(new TestCase() //
						.input(evcs0ChargeState, ChargeState.CHARGING) //
						.input(sumEssActivePower, 0) //
						.input(meterGridActivePower, 0) //
						.input(meterGridActivePowerL1, 0) //
						.input(meterGridActivePowerL2, 0) //
						.input(meterGridActivePowerL3, 0) //
						.input(essAllowedDischargePower, 0) //
						.input(evcs0SetPowerRequest, 15000) //
						.input(evcs1SetPowerRequest, 15000) //
						// TODO: The charge power of an EVCS has to be checked if it really charges
						// this amount)
						.input(evcs0ChargePower, 11000) //
						.input(evcs1ChargePower, 22000) //
						.input(evcs0MaximumPower, 22000) //
						.input(evcs1MaximumPower, 22000) //
						.input(evcs0MaximumHardwarePower, 22000) //
						.input(evcs1MaximumHardwarePower, 22000) //
						.input(evcs0Status, Status.CHARGING) //
						.input(evcs1Status, Status.CHARGING)) //
				.next(new TestCase() //
						.output(evcsClusterMaximumPowerToDistribute, 54000) //
						.output(evcs0SetChargePowerLimit, 15000) //
						.output(evcs1SetChargePowerLimit, 15000)) //
		;
	}

	@Test
	public void clusterDistribution_chargeTest_maximumHardwarePowerTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1" };
		EVCS_TARGET = this.getEvcsTarget(evcsIds);

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
						.setEvcsTarget(EVCS_TARGET) //
						.build()) //
				.next(new TestCase() //
						.input(evcs0ChargeState, ChargeState.CHARGING) //
						.input(sumEssActivePower, 0) //
						.input(meterGridActivePower, 0) //
						.input(meterGridActivePowerL1, 0) //
						.input(meterGridActivePowerL2, 0) //
						.input(meterGridActivePowerL3, 0) //
						.input(essAllowedDischargePower, 0) //
						.input(evcs0SetPowerRequest, 15000) //
						.input(evcs1SetPowerRequest, 15000) //
						.input(evcs0ChargePower, 11000) //
						.input(evcs1ChargePower, 0) //
						.input(evcs0MaximumPower, null) //
						.input(evcs1MaximumPower, null) //
						.input(evcs0MaximumHardwarePower, 11000) //
						.input(evcs1MaximumHardwarePower, 11000) //
						.input(evcs0Status, Status.CHARGING) //
						.input(evcs1Status, Status.CHARGING)) //
				.next(new TestCase() //
						.output(evcsClusterMaximumPowerToDistribute, 32000) //
						.output(evcs0SetChargePowerLimit, 11000) //
						.output(evcs1SetChargePowerLimit, 11000)) //
		;
	}

	@Test
	public void clusterDistribution_chargeTest_maximumPowerTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1" };
		EVCS_TARGET = this.getEvcsTarget(evcsIds);

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
						.setEvcsTarget(EVCS_TARGET) //
						.build()) //
				.next(new TestCase() //
						.input(evcs0ChargeState, ChargeState.CHARGING) //
						.input(sumEssActivePower, 0) //
						.input(meterGridActivePower, 0) //
						.input(meterGridActivePowerL1, 0) //
						.input(meterGridActivePowerL2, 0) //
						.input(meterGridActivePowerL3, 0) //
						.input(essAllowedDischargePower, 0) //
						.input(evcs0SetPowerRequest, 15000) //
						.input(evcs1SetPowerRequest, 15000) //
						.input(evcs0ChargePower, 0) //
						.input(evcs1ChargePower, 0) //
						.input(evcs0MaximumPower, 5000) //
						.input(evcs1MaximumPower, 9000) //
						.input(evcs0MaximumHardwarePower, 22000) //
						.input(evcs1MaximumHardwarePower, 22000) //
						.input(evcs0Status, Status.CHARGING) //
						.input(evcs1Status, Status.CHARGING)) //
				.next(new TestCase() //
						.output(evcsClusterMaximumPowerToDistribute, 21000) //
						.output(evcs0SetChargePowerLimit, 15000) //
						.output(evcs1SetChargePowerLimit, 15000)) //
		;
	}

	@Test
	public void clusterDistribution_chargeTest_minimumPowerTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1" };
		EVCS_TARGET = this.getEvcsTarget(evcsIds);

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
						.setEvcsTarget(EVCS_TARGET) //
						.build()) //
				.next(new TestCase() //
						.input(evcs0ChargeState, ChargeState.CHARGING) //
						.input(sumEssActivePower, 0) //
						.input(meterGridActivePower, 0) //
						.input(meterGridActivePowerL1, 0) //
						.input(meterGridActivePowerL2, 0) //
						.input(meterGridActivePowerL3, 0) //
						.input(essAllowedDischargePower, 0) //
						.input(evcs0SetPowerRequest, 15000) //
						.input(evcs1SetPowerRequest, 15000) //
						.input(evcs0ChargePower, 0) //
						.input(evcs1ChargePower, 0) //
						.input(evcs0MaximumPower, 5000) //
						.input(evcs1MaximumPower, 9000) //
						.input(evcs0MaximumHardwarePower, 22000) //
						.input(evcs1MaximumHardwarePower, 22000) //
						.input(evcs0MinimumHardwarePower, 4500) //
						.input(evcs1MinimumHardwarePower, 4500) //
						.input(evcs0Status, Status.READY_FOR_CHARGING) //
						.input(evcs1Status, Status.READY_FOR_CHARGING)) //
				.next(new TestCase() //
						.output(evcsClusterMaximumPowerToDistribute, 21000) //
						.output(evcs0SetChargePowerLimit, 4500) //
						.output(evcs1SetChargePowerLimit, 4500)) //
				.next(new TestCase() //
						.input(evcs0MinimumHardwarePower, 9000) //
						.input(evcs1MinimumHardwarePower, 6900) //
						.output(evcs0SetChargePowerLimit, 9000) //
						.output(evcs1SetChargePowerLimit, 6900)) //
		;
	}

	@Test
	public void clusterDistribution_filterTest() throws Exception {
		String[] evcsIds = { "evcs5" };
		EVCS_TARGET = this.getEvcsTarget(evcsIds);

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
						.setEvcsTarget(EVCS_TARGET) //
						.build()) //
				.next(new TestCase() //
						.input(sumEssActivePower, 0) //
						.input(meterGridActivePower, 0) //
						.input(meterGridActivePowerL1, 0) //
						.input(meterGridActivePowerL2, 0) //
						.input(meterGridActivePowerL3, 0) //
						.input(essAllowedDischargePower, 0) //
						.input(evcs5SetPowerRequest, 22000) //
						.input(evcs5ChargePower, 0) //
						.input(evcs5ChargeState, ChargeState.NOT_CHARGING) //
						.input(evcs5MaximumHardwarePower, 22080) //
						.input(evcs5Status, Status.READY_FOR_CHARGING) //
						.input(evcsClusterStatus, EvcsClusterStatus.REGULAR)) //
				.next(new TestCase() //
						// Cannot test charge states of evcs because the WriteHandler is not triggered
						// in a Cluster test.
						// Would expect Increasing
						// .output(evcs5ChargeState, ChargeState.INCREASING)) //
						// .output(evcs5ChargeState, ChargeState.INCREASING) //
						// .output(evcsClusterStatus, EvcsClusterStatus.INCREASING)) //
						.output(evcsClusterMaximumPowerToDistribute, 21000) //
						.output(evcs5SetChargePowerLimit, initialPowerFromCluster) //
				);
	}

	@Test
	public void clusterStatusTest() throws Exception {
		String[] evcsIds = { "evcs0", "evcs1", "evcs2", "evcs3" };
		EVCS_TARGET = this.getEvcsTarget(evcsIds);

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
						.setEvcsTarget(EVCS_TARGET) //
						.build()) //
				.next(new TestCase() //
						.input(evcs0ChargeState, ChargeState.UNDEFINED) //
						.input(evcs1ChargeState, ChargeState.UNDEFINED) //
						.input(evcs2ChargeState, ChargeState.UNDEFINED) //
						.input(evcs3ChargeState, ChargeState.UNDEFINED)) //
				.next(new TestCase() //
						.output(evcsClusterStatus, EvcsClusterStatus.UNDEFINED)) //
				.next(new TestCase() //
						.input(evcs0ChargeState, ChargeState.INCREASING) //
						.input(evcs1ChargeState, ChargeState.UNDEFINED) //
						.input(evcs2ChargeState, ChargeState.UNDEFINED) //
						.input(evcs3ChargeState, ChargeState.UNDEFINED)) //
				.next(new TestCase() //
						.output(evcsClusterStatus, EvcsClusterStatus.INCREASING)) //
				.next(new TestCase() //
						.input(evcs0ChargeState, ChargeState.CHARGING) //
						.input(evcs1ChargeState, ChargeState.UNDEFINED) //
						.input(evcs2ChargeState, ChargeState.UNDEFINED) //
						.input(evcs3ChargeState, ChargeState.UNDEFINED)) //
				.next(new TestCase() //
						.output(evcsClusterStatus, EvcsClusterStatus.REGULAR)) //
				.next(new TestCase() //
						.input(evcs0ChargeState, ChargeState.CHARGING) //
						.input(evcs1ChargeState, ChargeState.DECREASING) //
						.input(evcs2ChargeState, ChargeState.INCREASING) //
						.input(evcs3ChargeState, ChargeState.UNDEFINED)) //
				.next(new TestCase() //
						.output(evcsClusterStatus, EvcsClusterStatus.DECREASING)) //
				.next(new TestCase() //
						.input(evcs0ChargeState, ChargeState.CHARGING) //
						.input(evcs1ChargeState, ChargeState.CHARGING) //
						.input(evcs2ChargeState, ChargeState.CHARGING) //
						.input(evcs3ChargeState, ChargeState.CHARGING)) //
				.next(new TestCase() //
						.output(evcsClusterStatus, EvcsClusterStatus.REGULAR)) //
		;
	}

	private String getEvcsTarget(String[] evcsIds) {
		var stringBuilder = new StringBuilder();
		for (String evcsId : evcsIds) {
			stringBuilder.append("(id=" + evcsId + ")");
		}
		return "(&(enabled=true)(!(service.pid=evcsCluster0))(|" + stringBuilder.toString() + "))";
	}
}
