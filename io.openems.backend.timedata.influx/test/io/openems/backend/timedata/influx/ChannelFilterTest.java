package io.openems.backend.timedata.influx;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
	public void testIsValidComponentId() {
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

	@ParameterizedTest
	@ValueSource(strings = { "a1", "_a", "_sumS" })
	public void testValidComponentIds(String componentId) {
		assertTrue(ChannelFilter.isValidComponentId(componentId));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "a", "_", "_1", "_Sum", "_sum0", "1pv0", "pv0_", "pv_0", "p-v0", "pv0a" })
	public void testInvalidComponentIds(String componentId) {
		assertFalse(ChannelFilter.isValidComponentId(componentId));
	}

	@Test
	@Disabled("Performance test, disabled by default")
	public void testSimplePerformanceComponentIdValidation() {
		final var pattern = Pattern.compile("^([a-z][a-zA-Z0-9]+[0-9]+|_[a-z][a-zA-Z0-9]+[a-zA-Z])$").asPredicate();
		final int runs = 1_000_000;
		final int uniqueComponents = 100;

		final var componentIds = new ArrayList<String>(uniqueComponents);
		final var fakeComponentIds = new ArrayList<String>(uniqueComponents);

		for (int i = 0; i < uniqueComponents; i++) {
			componentIds.add("component" + i);
			fakeComponentIds.add("component" + i + "-fake0");
		}

		// Pattern validation
		final var startPattern = System.currentTimeMillis();
		for (int i = 0; i < runs; i++) {
			final int index = i % uniqueComponents;
			if (!pattern.test(componentIds.get(index))) {
				throw new IllegalStateException("Invalid component ID: " + componentIds.get(index));
			}
			if (pattern.test(fakeComponentIds.get(index))) {
				throw new IllegalStateException("Valid component ID: " + fakeComponentIds.get(index));
			}
		}
		final var endPattern = System.currentTimeMillis();

		// Manually parsing
		final var startManually = System.currentTimeMillis();
		for (int i = 0; i < runs; i++) {
			final int index = i % uniqueComponents;
			if (!ChannelFilter.isValidComponentId(componentIds.get(index))) {
				throw new IllegalStateException("Invalid component ID: " + componentIds.get(index));
			}
			if (ChannelFilter.isValidComponentId(fakeComponentIds.get(index))) {
				throw new IllegalStateException("Valid component ID: " + fakeComponentIds.get(index));
			}
		}
		final var endManually = System.currentTimeMillis();

		System.out.println("Pattern took: " + (endPattern - startPattern) + "ms");
		System.out.println("Manual took: " + (endManually - startManually) + "ms");
	}

}
