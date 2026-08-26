package io.openems.backend.timedata.influx;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.junit.Ignore;
import org.junit.Test;

public class ChannelFilterTest {

	@Test
	public void testFromDuplicatedChannel() {
		final var filter = ChannelFilter.from(new String[] { //
				"component0/Channel", //
				"component0/Channel", //
		}, new String[0]);

		assertNotNull(filter);
	}

	@Test
	public void testIsValid() {
		final var filter = ChannelFilter.from(//
				new String[] { //
						"component0/Channel", //
				}, //
				new String[] { //
						"_PropertyAlias", //
				});

		assertFalse(filter.isValid("/Channel"));
		assertFalse(filter.isValid("component0/"));
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
		assertTrue(filter.isValid("pvInverter0/S1Evt"));
		assertTrue(filter.isValid("pvInverter0/S111A"));

		assertFalse(filter.isValid("foo0/_PropertyAlias"));
		assertFalse(filter.isValid("bar0/_PropertyAlias"));

		assertTrue(filter.isValid("bar0/PropertyAlias"));
		for (int i = 0; i < 20; i++) {
			assertTrue(filter.isValid("pvInverter" + i + "/ActivePower"));
		}
	}

	@Test
	public void testIsValidComponentId() throws Exception {
		assertFalse(ChannelFilter.isValidComponentId(null));
		assertTrue(ChannelFilter.isValidComponentId("_sum"));
		assertFalse(ChannelFilter.isValidComponentId("pvInverter"));
		assertTrue(ChannelFilter.isValidComponentId("pvInverter0"));
		assertTrue(ChannelFilter.isValidComponentId("pvInverter10"));
		assertTrue(ChannelFilter.isValidComponentId("pvInverter9"));
		assertTrue(ChannelFilter.isValidComponentId("pvInverter99"));
		assertFalse(ChannelFilter.isValidComponentId("pvInverte.r0"));
		assertFalse(ChannelFilter.isValidComponentId("pvInve rter0"));
		assertFalse(ChannelFilter.isValidComponentId("PvInverter0"));
		assertFalse(ChannelFilter.isValidComponentId("pvInverter0a"));
		assertFalse(ChannelFilter.isValidComponentId("pvInvärter0"));
	}

	@Test
	@Ignore
	public void testSimplePerformanceComponentIdValidation() throws Exception {
		final var pattern = Pattern.compile("^([a-z][a-zA-Z0-9]+[0-9]+|_[a-z][a-zA-Z0-9]+[a-zA-Z])$").asPredicate();

		// Pattern validation
		final var startPattern = System.currentTimeMillis();
		for (int i = 0; i < 1_000_000; i++) {
			pattern.test("component" + i + "/Channel");
		}
		final var endPattern = System.currentTimeMillis();

		// Manually parsing
		final var startManually = System.currentTimeMillis();
		for (int i = 0; i < 1_000_000; i++) {
			ChannelFilter.isValidComponentId("component" + i + "/Channel");
		}
		final var endManually = System.currentTimeMillis();

		System.out.println("Pattern took: " + (endPattern - startPattern) + "ms");
		System.out.println("Manual took: " + (endManually - startManually) + "ms");
	}

}
