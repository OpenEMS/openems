package io.openems.edge.timeofusetariff.manual.eeg2025.gridsell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import java.util.Arrays;

import org.junit.Test;

import io.openems.common.timedata.DurationUnit;
import io.openems.common.utils.TimeRangeValues;
import io.openems.common.utils.TimeSpan;

public class UtilsTest {

	@Test
	public void testProcessPrices() {
		final var start = Instant.parse("2026-01-01T00:00:00Z");
		final var end = start.plusSeconds(2 * 3600);
		final var timeSpan = TimeSpan.between(start, end);

		final var resolution = DurationUnit.ofHours(1);

		final var marketPrices = TimeRangeValues.builder(timeSpan, resolution, Double.class)//
				.setByTime(start, -5.0)//
				.setByTime(start.plusSeconds(3600), 10.0)//
				.build();

		final var result = Utils.processPrices(20.0, marketPrices);

		assertEquals(2, Arrays.stream(result.entries()).toArray().length);
		assertEquals(0.0, result.getAt(start), 0.0);
		assertEquals(20.0, result.getAt(start.plusSeconds(3600)), 0.0);
		assertNull(result.getAt(start.plusSeconds(2 * 3600)));
	}
}
