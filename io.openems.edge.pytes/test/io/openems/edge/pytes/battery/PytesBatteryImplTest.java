package io.openems.edge.pytes.battery;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class PytesBatteryImplTest {

	private static final String BATTERY_ID = "battery0";

	private static final ChannelAddress BMS_CURRENT = new ChannelAddress(BATTERY_ID, "BmsBatteryCurrent");
	private static final ChannelAddress BMS_VOLTAGE = new ChannelAddress(BATTERY_ID, "BmsBatteryVoltage");
	private static final ChannelAddress DIRECTION = new ChannelAddress(BATTERY_ID, "BatteryCurrentDirection");
	private static final ChannelAddress DC_DISCHARGE_POWER = new ChannelAddress(BATTERY_ID, "DcDischargePower");
	private static final ChannelAddress CURRENT = new ChannelAddress(BATTERY_ID, "Current");
	private static final ChannelAddress VOLTAGE = new ChannelAddress(BATTERY_ID, "Voltage");

	private static ComponentTest activate(DummyPytesJs3 ess) throws Exception {
		return new ComponentTest(new PytesBatteryImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ess) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setEssId(ess.id()) //
						.build());
	}

	@Test
	public void registersAtEss() throws Exception {
		var ess = new DummyPytesJs3("ess0");
		var test = activate(ess);
		assertSame(test.getSut(), ess.getBattery());
		test.deactivate();
		assertNull("battery must unregister from the ESS on deactivate", ess.getBattery());
	}

	@Test
	public void calculatesPowerFromBmsValues() throws Exception {
		var ess = new DummyPytesJs3("ess0");
		activate(ess) //
				// 10 A discharge at 53 V
				.next(new TestCase("discharge 10 A") //
						.input(BMS_CURRENT, 10_000) // mA
						.input(BMS_VOLTAGE, 53_000) // mV
						.input(DIRECTION, 1) // != 0 -> discharge
						.output(DC_DISCHARGE_POWER, 530) //
						.output(CURRENT, 10) //
						.output(VOLTAGE, 53)) //
				// 8 A charge
				.next(new TestCase("charge 8 A") //
						.input(BMS_CURRENT, 8_000) //
						.input(BMS_VOLTAGE, 53_000) //
						.input(DIRECTION, 0) // 0 -> charge
						.output(DC_DISCHARGE_POWER, -424) //
						.output(CURRENT, -8)) //
				// 56 A charge: mA * mV exceeds Integer.MAX_VALUE. Before the fix this
				// overflowed to +1330 W (wrong sign and magnitude), seen live on
				// 2026-09-16 right after a restart.
				.next(new TestCase("charge 56 A, no int overflow") //
						.input(BMS_CURRENT, 56_000) //
						.input(BMS_VOLTAGE, 53_000) //
						.input(DIRECTION, 0) //
						.output(DC_DISCHARGE_POWER, -2968) //
						.output(CURRENT, -56)) //
				.deactivate();
	}

	@Test
	public void keepsLastPowerWhenValuesMissing() throws Exception {
		var ess = new DummyPytesJs3("ess0");
		activate(ess) //
				.next(new TestCase() //
						.input(BMS_CURRENT, 10_000) //
						.input(BMS_VOLTAGE, 53_000) //
						.input(DIRECTION, 1) //
						.output(DC_DISCHARGE_POWER, 530)) //
				.next(new TestCase("voltage missing") //
						.input(BMS_VOLTAGE, null) //
						.output(DC_DISCHARGE_POWER, 530)) //
				.deactivate();
	}
}
