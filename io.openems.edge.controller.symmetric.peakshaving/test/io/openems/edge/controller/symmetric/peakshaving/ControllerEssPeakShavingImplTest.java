package io.openems.edge.controller.symmetric.peakshaving;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class ControllerEssPeakShavingImplTest {

	private static final String CTRL_ID = "ctrl0";

	private static final String ESS_ID = "ess0";
	private static final ChannelAddress ESS_ACTIVE_POWER = new ChannelAddress(ESS_ID, "ActivePower");
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS = new ChannelAddress(ESS_ID,
			"SetActivePowerEqualsWithPid");

	private static final String METER_ID = "meter0";
	private static final ChannelAddress METER_ACTIVE_POWER = new ChannelAddress(METER_ID, "ActivePower");

	@Test
	public void default_settings() throws Exception {
		new ControllerTest(new ControllerEssPeakShavingImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyManagedSymmetricEss(ESS_ID) //
						.setPower(new DummyPower(0.3, 0.3, 0.1))) //
				.addComponent(new DummyElectricityMeter(METER_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMeterId(METER_ID) //
						.setIsStandalone(true) //
						.setEnableRecharge(true) //
						.setPeakShavingPower(100_000) //
						.setRechargePower(50_000) //
						.build())
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, 120_000) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20_000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, 40_000) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, -10_000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, 120_000) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20_000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 3793) //
						.input(METER_ACTIVE_POWER, 120_000 - 3793) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20_000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 8981) //
						.input(METER_ACTIVE_POWER, 120_000 - 8981) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20_000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 13_723) //
						.input(METER_ACTIVE_POWER, 120_000 - 13_723) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20_000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 17_469) //
						.input(METER_ACTIVE_POWER, 120_000 - 17_469) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20_000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 20_066) //
						.input(METER_ACTIVE_POWER, 120_000 - 20_066) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20_000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 21_564) //
						.input(METER_ACTIVE_POWER, 120_000 - 21_564) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20_000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 22_175) //
						.input(METER_ACTIVE_POWER, 120_000 - 22_175) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20_000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 22_173) //
						.input(METER_ACTIVE_POWER, 120_000 - 22_173) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20_000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 21_816) //
						.input(METER_ACTIVE_POWER, 120000 - 21_816) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20_000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 21_311) //
						.input(METER_ACTIVE_POWER, 120_000 - 21_311) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20_000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 20_803) //
						.input(METER_ACTIVE_POWER, 120_000 - 20_803) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20_000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 20_377) //
						.input(METER_ACTIVE_POWER, 120_000 - 20_377) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20_000)); //
	}
	
	@Test
	public void no_recharge() throws Exception {
		new ControllerTest(new ControllerEssPeakShavingImpl()) //
		.addReference("componentManager", new DummyComponentManager()) //
		.addComponent(new DummyManagedSymmetricEss(ESS_ID) //
				.setPower(new DummyPower(0.3, 0.3, 0.1))) //
		.addComponent(new DummyElectricityMeter(METER_ID)) //
		.activate(MyConfig.create() //
				.setId(CTRL_ID) //
				.setEssId(ESS_ID) //
				.setMeterId(METER_ID) //
				.setIsStandalone(true) //
				.setEnableRecharge(false) //
				.setPeakShavingPower(100_000) //
				.build())
		.next(new TestCase() //
				.input(ESS_ACTIVE_POWER, 0) //
				.input(METER_ACTIVE_POWER, 120_000) //
				.output(ESS_SET_ACTIVE_POWER_EQUALS, 20_000)) //
		.next(new TestCase() //
				.input(ESS_ACTIVE_POWER, 0) //
				.input(METER_ACTIVE_POWER, 40_000) //
				.output(ESS_SET_ACTIVE_POWER_EQUALS, 0)) //
		.next(new TestCase() //
				.input(ESS_ACTIVE_POWER, 13_723) //
				.input(METER_ACTIVE_POWER, 115_000 - 13_723) //
				.output(ESS_SET_ACTIVE_POWER_EQUALS, 15_000)) //
		.next(new TestCase() //
				.input(ESS_ACTIVE_POWER, 20_377) //
				.input(METER_ACTIVE_POWER, 112_000 - 20_377) //
				.output(ESS_SET_ACTIVE_POWER_EQUALS, 12_000)); //
	}
	
	@Test
	public void not_standalone() throws Exception {
		new ControllerTest(new ControllerEssPeakShavingImpl()) //
		.addReference("componentManager", new DummyComponentManager()) //
		.addComponent(new DummyManagedSymmetricEss(ESS_ID) //
				.setPower(new DummyPower(0.3, 0.3, 0.1))) //
		.addComponent(new DummyElectricityMeter(METER_ID)) //
		.activate(MyConfig.create() //
				.setId(CTRL_ID) //
				.setEssId(ESS_ID) //
				.setMeterId(METER_ID) //
				.setIsStandalone(false) //
				.setEnableRecharge(false) //
				.setPeakShavingPower(100_000) //
				.build())
		.next(new TestCase() //
				.input(ESS_ACTIVE_POWER, 0) //
				.input(METER_ACTIVE_POWER, 120_000) //
				.output(ESS_SET_ACTIVE_POWER_EQUALS, 20_000)) //
		.next(new TestCase() //
				.input(ESS_ACTIVE_POWER, 0) //
				.input(METER_ACTIVE_POWER, 99_000) //
				.output(ESS_SET_ACTIVE_POWER_EQUALS, null)) //
		.next(new TestCase() //
				.input(ESS_ACTIVE_POWER, 0) //
				.input(METER_ACTIVE_POWER, 40_000) //
				.output(ESS_SET_ACTIVE_POWER_EQUALS, null)) //
		.next(new TestCase() //
				.input(ESS_ACTIVE_POWER, 13_723) //
				.input(METER_ACTIVE_POWER, 120_000 - 13_723) //
				.output(ESS_SET_ACTIVE_POWER_EQUALS, 20_000)) //
		.next(new TestCase() //
				.input(ESS_ACTIVE_POWER, 20_377) //
				.input(METER_ACTIVE_POWER, 120_000 - 20_377) //
				.output(ESS_SET_ACTIVE_POWER_EQUALS, 20_000)); //
	}

}
