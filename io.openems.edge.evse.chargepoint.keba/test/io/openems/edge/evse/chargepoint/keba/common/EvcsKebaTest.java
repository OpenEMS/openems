package io.openems.edge.evse.chargepoint.keba.common;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.evse.chargepoint.keba.common.enums.CableState;

public class EvcsKebaTest {

	/**
	 * Use the given {@link TestCase} to test all expected Channels from
	 * {@link EvcsKeba}.
	 * 
	 * @param tc The {@link TestCase}
	 * @throws Exception on error
	 */
	public static void testEvcsKebaChannels(TestCase tc) throws Exception {
		tc //
				.output(EvcsKeba.ChannelId.PLUG, CableState.PLUGGED_AND_LOCKED) //
		;
	}
}
