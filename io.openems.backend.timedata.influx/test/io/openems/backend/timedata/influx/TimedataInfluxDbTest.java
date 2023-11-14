package io.openems.backend.timedata.influx;

import static io.openems.backend.timedata.influx.TimedataInfluxDb.isAllowed;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TimedataInfluxDbTest {

	@Test
	public void testIsAllowed() {
		assertFalse(isAllowed(null));
		assertFalse(isAllowed("invalid"));
		assertFalse(isAllowed("in/va/lid"));

		// Channel-ID
		assertTrue(isAllowed("ess0/ActivePower"));
		assertTrue(isAllowed("_sum/EssActivePower"));
		assertTrue(isAllowed("_cycle/MeasuredCycleTime"));

		assertFalse(isAllowed("ess/ActivePower"));
		assertFalse(isAllowed("Ess1/ActivePower"));
		assertFalse(isAllowed("cycle/MeasuredCycleTime"));
		assertFalse(isAllowed("_cycle1/MeasuredCycleTime"));
		assertFalse(isAllowed("My Heat-Pump/Status"));
		assertFalse(isAllowed("äöü/Status"));

		// SunSpec
		assertFalse(isAllowed("pvInverter0/S1Evt"));
		assertFalse(isAllowed("pvInverter0/S111A"));
	}

}
