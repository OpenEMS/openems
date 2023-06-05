package io.openems.edge.meter.virtual.symmetric.subtract;

import org.junit.Test;

import com.google.common.collect.Lists;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.test.DummySymmetricMeter;

public class VirtualSubtractMeterTest {

	private static final String METER_ID = "meter0";
	private static final ChannelAddress METER_POWER = new ChannelAddress(METER_ID, "ActivePower");

	private static final String MINUEND_ID = "meter1";
	private static final ChannelAddress MINUEND_POWER = new ChannelAddress(MINUEND_ID, "ActivePower");

	private static final String SUBTRAHEND1_ID = "meter2";
	private static final ChannelAddress SUBTRAHEND1_POWER = new ChannelAddress(SUBTRAHEND1_ID, "ActivePower");

	private static final String SUBTRAHEND2_ID = "ess0";
	private static final ChannelAddress SUBTRAHEND2_POWER = new ChannelAddress(SUBTRAHEND2_ID, "ActivePower");

	@Test
	public void test() throws Exception {
		new ComponentTest(new MeterVirtualSymmetricSubtractImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("minuend", new DummySymmetricMeter(MINUEND_ID)) //
				.addReference("subtrahends", Lists.newArrayList(//
						new DummySymmetricMeter(SUBTRAHEND1_ID), //
						new DummyManagedSymmetricEss(SUBTRAHEND2_ID))) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setType(MeterType.GRID) //
						.setAddToSum(true) //
						.setMinuendId(MINUEND_ID) //
						.setSubtrahendsIds(SUBTRAHEND1_ID, SUBTRAHEND2_ID) //
						.build()) //
				.next(new TestCase() //
						.input(MINUEND_POWER, 5_000) //
						.input(SUBTRAHEND1_POWER, 2_000) //
						.input(SUBTRAHEND2_POWER, 4_000) //
						.output(METER_POWER, -1000)) //
				.next(new TestCase() //
						.input(MINUEND_POWER, null) //
						.input(SUBTRAHEND1_POWER, 2_000) //
						.input(SUBTRAHEND2_POWER, 4_000) //
						.output(METER_POWER, null)) //
				.next(new TestCase() //
						.input(MINUEND_POWER, 5_000) //
						.input(SUBTRAHEND1_POWER, null) //
						.input(SUBTRAHEND2_POWER, 4_000) //
						.output(METER_POWER, 1000));
	}
}