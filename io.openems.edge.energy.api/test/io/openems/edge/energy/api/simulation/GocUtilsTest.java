package io.openems.edge.energy.api.simulation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GocUtilsTest {

	@Test
	public void testNormalizeModePreferenceRanks_ShouldReturnEmptyMapIfInputIsEmpty() {
		final var result = GocUtils.normalizeModePreferenceRanks(Map.of());

		assertTrue(result.isEmpty());
	}

	@Test
	public void testNormalizeModePreferenceRanks_ShouldNormalizeSingleEntryToZero() {
		final var input = Map.of(1, 42);

		final var result = GocUtils.normalizeModePreferenceRanks(input);

		assertEquals(1, result.size());
		assertEquals(0.0, result.get(1), 0.);
	}

	@Test
	public void testNormalizeModePreferenceRanks_ShouldNormalizeMultipleDistinctRanks() {
		final var input = Map.of(//
				1, 10, //
				2, 20, //
				3, 30);

		final var result = GocUtils.normalizeModePreferenceRanks(input);

		assertEquals(0.0, result.get(1), 0.);
		assertEquals(0.5, result.get(2), 0.);
		assertEquals(1.0, result.get(3), 0.);
	}

	@Test
	public void testNormalizeModePreferenceRanks_ShouldUseDenseRankingForDuplicateRanks() {
		final var input = Map.of(//
				1, 10, //
				2, 10, //
				3, 30);

		final var result = GocUtils.normalizeModePreferenceRanks(input);

		assertEquals(0.0, result.get(1), 0.);
		assertEquals(0.0, result.get(2), 0.);
		assertEquals(1.0, result.get(3), 0.);
	}

	@Test
	public void testNormalizeModePreferenceRanks_ShouldTreatNullAsHighestRank() {
		final var input = new HashMap<Integer, Integer>();
		input.put(1, 5);
		input.put(2, null);

		final var result = GocUtils.normalizeModePreferenceRanks(input);

		assertEquals(0.0, result.get(1), 0.);
		assertEquals(1.0, result.get(2), 0.);
	}
}
