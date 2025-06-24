package io.openems.edge.controller.evse.single;

import static org.junit.Assert.assertEquals;

import java.time.Instant;

import org.junit.Test;

import io.openems.common.test.TestUtils;
import io.openems.edge.controller.evse.single.Types.History;
import io.openems.edge.controller.evse.single.Types.Hysteresis;

public class TypesTest {

	@Test
	public void testHysteresis() {
		// TODO full tests
		var now = Instant.now(TestUtils.createDummyClock());
		var h = new History();
		h.addEntry(now.minusSeconds(310), null, 7000);
		h.addEntry(now.minusSeconds(300), null, 8000);
		h.addEntry(now.minusSeconds(290), null, 9000);
		assertEquals(Hysteresis.INACTIVE, Hysteresis.from(h));
	}

}
