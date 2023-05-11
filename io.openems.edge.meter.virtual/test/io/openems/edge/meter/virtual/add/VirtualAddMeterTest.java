package io.openems.edge.meter.virtual.add;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class VirtualAddMeterTest {

	private static final String METER_ID = "meter0";
	private static final ChannelAddress METER_POWER = new ChannelAddress(METER_ID, "ActivePower");
	private static final ChannelAddress METER_VOLTAGE = new ChannelAddress(METER_ID, "Voltage");
	private static final ChannelAddress METER_FREQ = new ChannelAddress(METER_ID, "Frequency");

	private static final ChannelAddress METER_POWER_L1 = new ChannelAddress(METER_ID, "ActivePowerL1");
	private static final ChannelAddress METER_POWER_L2 = new ChannelAddress(METER_ID, "ActivePowerL2");
	private static final ChannelAddress METER_POWER_L3 = new ChannelAddress(METER_ID, "ActivePowerL3");

	private static final String METER_ID_1 = "meter1";
	private static final ChannelAddress METER_ID_1_ACTIVEPOWER = new ChannelAddress(METER_ID_1, "ActivePower");
	private static final ChannelAddress METER_ID_1_VOLTAGE = new ChannelAddress(METER_ID_1, "Voltage");
	private static final ChannelAddress METER_ID_1_FREQUENCY = new ChannelAddress(METER_ID_1, "Frequency");

	private static final ChannelAddress METER_ID_1_ACTIVEPOWER_L1 = new ChannelAddress(METER_ID_1, "ActivePowerL1");
	private static final ChannelAddress METER_ID_1_ACTIVEPOWER_L2 = new ChannelAddress(METER_ID_1, "ActivePowerL2");
	private static final ChannelAddress METER_ID_1_ACTIVEPOWER_L3 = new ChannelAddress(METER_ID_1, "ActivePowerL3");

	private static final String METER_ID_2 = "meter2";
	private static final ChannelAddress METER_ID_2_ACTIVEPOWER = new ChannelAddress(METER_ID_2, "ActivePower");
	private static final ChannelAddress METER_ID_2_VOLTAGE = new ChannelAddress(METER_ID_2, "Voltage");
	private static final ChannelAddress METER_ID_2_FREQUENCY = new ChannelAddress(METER_ID_2, "Frequency");

	private static final ChannelAddress METER_ID_2_ACTIVEPOWER_L1 = new ChannelAddress(METER_ID_2, "ActivePowerL1");
	private static final ChannelAddress METER_ID_2_ACTIVEPOWER_L2 = new ChannelAddress(METER_ID_2, "ActivePowerL2");
	private static final ChannelAddress METER_ID_2_ACTIVEPOWER_L3 = new ChannelAddress(METER_ID_2, "ActivePowerL3");

	@Test
	public void test() throws Exception {
		new ComponentTest(new VirtualAddMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("addMeter", new DummyElectricityMeter(METER_ID_1))
				.addReference("addMeter", new DummyElectricityMeter(METER_ID_2)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setMeterIds(METER_ID_1, METER_ID_2) //
						.setType(MeterType.GRID) //
						.build())
				.next(new TestCase("one") //
						.input(METER_ID_1_ACTIVEPOWER, 6_000) //
						.input(METER_ID_1_ACTIVEPOWER_L1, 2_000) //
						.input(METER_ID_1_ACTIVEPOWER_L2, 2_000) //
						.input(METER_ID_1_ACTIVEPOWER_L3, 2_000) //
						.input(METER_ID_2_ACTIVEPOWER, 7_500) //
						.input(METER_ID_2_ACTIVEPOWER_L1, 2_500) //
						.input(METER_ID_2_ACTIVEPOWER_L2, 2_500) //
						.input(METER_ID_2_ACTIVEPOWER_L3, 2_500) //
						.input(METER_ID_1_VOLTAGE, 10) //
						.input(METER_ID_2_VOLTAGE, 20) //
						.input(METER_ID_1_FREQUENCY, 49) //
						.input(METER_ID_2_FREQUENCY, 51)) //
				.next(new TestCase("two") //
						.output(METER_POWER, 13_500) //
						.output(METER_POWER_L1, 4_500) //
						.output(METER_POWER_L2, 4_500) //
						.output(METER_POWER_L3, 4_500) //
						.output(METER_VOLTAGE, 15) //
						.output(METER_FREQ, 50));
	}
}
