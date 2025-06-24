package io.openems.edge.meter.test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.meter.api.ElectricityMeter;

public class InvertTest {

	/**
	 * Creates a {@link TestCase} for testing the functionality of a
	 * {@link ElectricityMeter}.
	 * 
	 * @param invert meter config parameter
	 * @return the {@link TestCase}
	 */
	public static TestCase testInvert(boolean invert) throws Exception {
		var signum = invert ? -1 : 1;
		return new TestCase()//
				.output(ElectricityMeter.ChannelId.CURRENT, 3000)//
				.output(ElectricityMeter.ChannelId.CURRENT_L1, 1000)//
				.output(ElectricityMeter.ChannelId.CURRENT_L2, 1000)//
				.output(ElectricityMeter.ChannelId.CURRENT_L3, 1000)//
				.output(ElectricityMeter.ChannelId.VOLTAGE, 1000)//
				.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 1000)//
				.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 1000)//
				.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 1000)//
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 10000 * signum)//
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 10000 * signum)//
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 10000 * signum)//
				.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 13000 * signum)//
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER, 7000 * signum)//
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, 7000 * signum)//
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, 7000 * signum)//
				.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, 7000 * signum)//
				.output(ElectricityMeter.ChannelId.FREQUENCY, 5000)//

		;
	}

	/**
	 * Creates a {@link TestCase} for testing the energy functionality of a
	 * {@link ElectricityMeter}.
	 * 
	 * @param invert meter config parameter
	 * @return the {@link TestCase}
	 */
	public static TestCase testEnergyInvert(boolean invert) {
		return new TestCase()//
				.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, invert ? 0L : 13000L)//
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, invert ? 13000L : 0L);//
	}
}
