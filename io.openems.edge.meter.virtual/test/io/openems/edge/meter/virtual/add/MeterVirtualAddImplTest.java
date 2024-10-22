package io.openems.edge.meter.virtual.add;

import static io.openems.common.types.MeterType.GRID;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L1;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L2;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L3;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.FREQUENCY;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.VOLTAGE;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class MeterVirtualAddImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new MeterVirtualAddImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("addMeter", new DummyElectricityMeter("meter1"))
				.addReference("addMeter", new DummyElectricityMeter("meter2")) //
				.addReference("addMeter", new DummyElectricityMeter("meter3")) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setMeterIds("meter1", "meter2", "meter3") //
						.setType(GRID) //
						.build())
				.next(new TestCase("one") //
						.input("meter1", ACTIVE_POWER, 6_000) //
						.input("meter1", ACTIVE_POWER_L1, 2_000) //
						.input("meter1", ACTIVE_POWER_L2, 2_000) //
						.input("meter1", ACTIVE_POWER_L3, 2_000) //
						.input("meter2", ACTIVE_POWER, 7_500) //
						.input("meter2", ACTIVE_POWER_L1, 2_500) //
						.input("meter2", ACTIVE_POWER_L2, 2_500) //
						.input("meter2", ACTIVE_POWER_L3, 2_500) //
						.input("meter3", ACTIVE_POWER, 9_000) //
						.input("meter3", ACTIVE_POWER_L1, 3_000) //
						.input("meter3", ACTIVE_POWER_L2, 3_000) //
						.input("meter3", ACTIVE_POWER_L3, 3_000) //
						.input("meter1", VOLTAGE, 10) //
						.input("meter2", VOLTAGE, 20) //
						.input("meter3", VOLTAGE, 30) //
						.input("meter1", FREQUENCY, 49) //
						.input("meter2", FREQUENCY, 51) //
						.input("meter3", FREQUENCY, 56)) //
				.next(new TestCase("two") //
						.output(ACTIVE_POWER, 22_500) //
						.output(ACTIVE_POWER_L1, 7_500) //
						.output(ACTIVE_POWER_L2, 7_500) //
						.output(ACTIVE_POWER_L3, 7_500) //
						.output(VOLTAGE, 20) //
						.output(FREQUENCY, 52));
	}
}
