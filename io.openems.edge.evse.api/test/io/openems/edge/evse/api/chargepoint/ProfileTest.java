package io.openems.edge.evse.api.chargepoint;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.SINGLE_PHASE;
import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;
import static io.openems.edge.evse.api.common.ApplySetPoint.convertAmpereToWatt;
import static io.openems.edge.evse.api.common.ApplySetPoint.convertMilliAmpereToWatt;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ProfileTest {

	@Test
	public void test() {
		assertEquals(1380, convertAmpereToWatt(SINGLE_PHASE, 6));
		assertEquals(1380, convertMilliAmpereToWatt(SINGLE_PHASE, 6000));

		assertEquals(4140, convertAmpereToWatt(THREE_PHASE, 6));
		assertEquals(4140, convertMilliAmpereToWatt(THREE_PHASE, 6000));

		assertEquals(3680, convertAmpereToWatt(SINGLE_PHASE, 16));
		assertEquals(3680, convertMilliAmpereToWatt(SINGLE_PHASE, 16000));

		assertEquals(11040, convertAmpereToWatt(THREE_PHASE, 16));
		assertEquals(11040, convertMilliAmpereToWatt(THREE_PHASE, 16000));

		assertEquals(7360, convertAmpereToWatt(SINGLE_PHASE, 32));
		assertEquals(7360, convertMilliAmpereToWatt(SINGLE_PHASE, 32000));

		assertEquals(22080, convertAmpereToWatt(THREE_PHASE, 32));
		assertEquals(22080, convertMilliAmpereToWatt(THREE_PHASE, 32000));
	}

}
