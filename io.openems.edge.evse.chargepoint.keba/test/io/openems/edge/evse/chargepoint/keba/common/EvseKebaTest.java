package io.openems.edge.evse.chargepoint.keba.common;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;

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
				.output(EvseKeba.ChannelId.ENERGY_SESSION, 6530) //
				.output(EvseKeba.ChannelId.SET_ENERGY_LIMIT, null) //
		;
	}
}
