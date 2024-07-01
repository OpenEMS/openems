package io.openems.backend.timedata.influx;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ChannelFilterTest {

	@Test
	public void testFromDuplicatedChannel() {
		final var filter = ChannelFilter.from(new String[] { //
				"component0/Channel", //
				"component0/Channel", //
		});

		assertNotNull(filter);
	}

	@Test
	public void testIsValid() {
		final var filter = ChannelFilter.from(new String[] { //
				"component0/Channel", //
		});

		assertFalse(filter.isValid("component0/Channel"));
		assertTrue(filter.isValid("component0/SomeOtherChannel"));

		assertFalse(filter.isValid(null));
		assertFalse(filter.isValid("invalid"));
		assertFalse(filter.isValid("in/va/lid"));

		// Channel-ID
		assertTrue(filter.isValid("ess0/ActivePower"));
		assertTrue(filter.isValid("_sum/EssActivePower"));
		assertTrue(filter.isValid("_cycle/MeasuredCycleTime"));

		assertFalse(filter.isValid("ess/ActivePower"));
		assertFalse(filter.isValid("Ess1/ActivePower"));
		assertFalse(filter.isValid("cycle/MeasuredCycleTime"));
		assertFalse(filter.isValid("_cycle1/MeasuredCycleTime"));
		assertFalse(filter.isValid("My Heat-Pump/Status"));
		assertFalse(filter.isValid("äöü/Status"));
		assertFalse(filter.isValid("äöü0/Status"));

		// SunSpec
		assertFalse(filter.isValid("pvInverter0/S1Evt"));
		assertFalse(filter.isValid("pvInverter0/S111A"));
	}

}
