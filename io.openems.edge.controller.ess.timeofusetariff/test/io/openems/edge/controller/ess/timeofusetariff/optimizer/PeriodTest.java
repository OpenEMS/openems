package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.DELAY_DISCHARGE;
import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

public class PeriodTest {

	@Test
	public void testToString() {
		var time = ZonedDateTime.of(2000, 1, 1, 0, 15, 0, 0, ZoneId.of("UTC"));
		var p = new Period(time, 123, 234, 8000, DELAY_DISCHARGE, 345, 456, 567F, 678.);

		assertEquals(
				"00:15        123         234       8000 DELAY_DISCHARGE                  345   456 567.00 678.0000",
				p.toString());

		Period.fromLog(p.toString());
	}

}
