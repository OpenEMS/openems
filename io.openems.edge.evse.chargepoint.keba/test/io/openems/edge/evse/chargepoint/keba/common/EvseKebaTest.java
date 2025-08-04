package io.openems.edge.evse.chargepoint.keba.common;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.meter.api.ElectricityMeter;

public class EvseKebaTest {

	/**
	 * Use the given {@link TestCase} to test all expected Channels from
	 * {@link EvseKeba}.
	 * 
	 * @param tc The {@link TestCase}
	 * @throws Exception on error
	 */
	public static void testEvseKebaChannels(TestCase tc) throws Exception {
		tc //
				.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
				// Not Deprecated EVCS

				.output(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, true) //

				.output(EvseKeba.ChannelId.ENERGY_SESSION, 6530) //
				.output(EvseKeba.ChannelId.SET_ENERGY_LIMIT, null) //
		;
	}
}
