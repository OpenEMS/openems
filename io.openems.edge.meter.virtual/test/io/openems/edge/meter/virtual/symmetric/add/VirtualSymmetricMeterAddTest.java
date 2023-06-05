package io.openems.edge.meter.virtual.symmetric.add;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.test.DummySymmetricMeter;

public class VirtualSymmetricMeterAddTest {

	private static final String METER_ID = "meter0";
	private static final ChannelAddress METER_POWER = new ChannelAddress(METER_ID, "ActivePower");
	private static final ChannelAddress METER_FREQ = new ChannelAddress(METER_ID, "Frequency");

	private static final String METER_ID_1 = "meter1";
	private static final ChannelAddress METER_ID_1_ACTIVEPOWER = new ChannelAddress(METER_ID_1, "ActivePower");
	private static final ChannelAddress METER_ID_1_FREQUENCY = new ChannelAddress(METER_ID_1, "Frequency");

	private static final String METER_ID_2 = "meter2";
	private static final ChannelAddress METER_ID_2_ACTIVEPOWER = new ChannelAddress(METER_ID_2, "ActivePower");
	private static final ChannelAddress METER_ID_2_FREQUENCY = new ChannelAddress(METER_ID_2, "Frequency");

	private static final String METER_ID_3 = "meter3";
	private static final ChannelAddress METER_ID_3_ACTIVEPOWER = new ChannelAddress(METER_ID_3, "ActivePower");
	private static final ChannelAddress METER_ID_3_FREQUENCY = new ChannelAddress(METER_ID_3, "Frequency");

	private static final String METER_ID_4 = "meter4";
	private static final ChannelAddress METER_ID_4_ACTIVEPOWER = new ChannelAddress(METER_ID_4, "ActivePower");
	private static final ChannelAddress METER_ID_4_FREQUENCY = new ChannelAddress(METER_ID_4, "Frequency");

	@Test
	public void test() throws Exception {
		new ComponentTest(new MeterVirtualSymmetricAddImpl()) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("addMeter", new DummySymmetricMeter(METER_ID_1)) //
				.addReference("addMeter", new DummySymmetricMeter(METER_ID_2)) //
				.addReference("addMeter", new DummySymmetricMeter(METER_ID_3)) //
				.addReference("addMeter", new DummySymmetricMeter(METER_ID_4)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setMeterIds(METER_ID_1, METER_ID_2, METER_ID_3, METER_ID_4) //
						.setType(MeterType.GRID) //
						.build())
				.next(new TestCase("one") //
						.input(METER_ID_1_ACTIVEPOWER, 2_000) //
						.input(METER_ID_2_ACTIVEPOWER, 2_000) //
						.input(METER_ID_3_ACTIVEPOWER, 2_000) //
						.input(METER_ID_4_ACTIVEPOWER, 2_000) //
						.input(METER_ID_1_FREQUENCY, 49) //
						.input(METER_ID_2_FREQUENCY, 51) //
						.input(METER_ID_3_FREQUENCY, 49) //
						.input(METER_ID_4_FREQUENCY, 51)) //
				.next(new TestCase() //
						.output(METER_POWER, 8_000) //
						.output(METER_FREQ, 51));
	}
}
