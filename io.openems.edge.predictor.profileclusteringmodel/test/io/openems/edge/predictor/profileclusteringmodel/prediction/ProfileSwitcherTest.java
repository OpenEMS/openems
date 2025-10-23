package io.openems.edge.predictor.profileclusteringmodel.prediction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.Test;

import io.openems.edge.predictor.api.mlcore.datastructures.Series;
import io.openems.edge.predictor.profileclusteringmodel.Profile;

public class ProfileSwitcherTest {

	@Test
	public void testFindBetterProfile_ShouldReturnEmpty_WhenTodaysValuesEmpty() {
		var profiles = List.of(//
				createConstantProfile(0, 10), //
				createConstantProfile(1, 20));
		var currentProfile = profiles.get(0);
		var todaysValues = createConstantSeries(Double.NaN);

		var switcher = new ProfileSwitcher(profiles, currentProfile, todaysValues);
		var result = switcher.findBetterProfile();

		assertTrue(result.isEmpty());
	}

	@Test
	public void testFindBetterProfile_ShouldReturnEmpty_WhenNoProfileIsBetter() {
		var profiles = List.of(//
				createConstantProfile(0, 10), //
				createConstantProfile(1, 20));
		var currentProfile = profiles.get(0);
		var todaysValues = createConstantSeries(10.0);

		var switcher = new ProfileSwitcher(profiles, currentProfile, todaysValues);
		var result = switcher.findBetterProfile();

		assertTrue(result.isEmpty());
	}

	@Test
	public void testFindBetterProfile_ShouldReturnBetterProfile_WhenBetterExists() {
		var profiles = List.of(//
				createConstantProfile(0, 10), //
				createConstantProfile(1, 50));
		var currentProfile = profiles.get(0);
		var todaysValues = createConstantSeries(50.0);

		var switcher = new ProfileSwitcher(profiles, currentProfile, todaysValues);
		var result = switcher.findBetterProfile();

		assertTrue(result.isPresent());
		assertEquals(1, result.get().clusterIndex());
	}

	@Test
	public void testFindBetterProfile_ShouldNotSwitchTooEarly_WithPartialObservations() {
		var profiles = List.of(//
				createConstantProfile(0, 10), //
				createConstantProfile(1, 50));
		var currentProfile = profiles.get(0);

		double[] values = new double[Profile.LENGTH];
		for (int i = 0; i < Profile.LENGTH; i++) {
			values[i] = (i < 12) ? 50.0 : Double.NaN;
		}

		var index = IntStream.range(0, Profile.LENGTH)//
				.boxed()//
				.toList();
		var valueList = IntStream.range(0, Profile.LENGTH)//
				.mapToObj(i -> values[i])//
				.toList();
		var todaysValues = new Series<>(index, valueList);

		var switcher = new ProfileSwitcher(profiles, currentProfile, todaysValues);
		var result = switcher.findBetterProfile();

		assertTrue(result.isEmpty());
	}

	private static Profile createConstantProfile(int clusterIndex, double value) {
		double[] values = new double[Profile.LENGTH];
		for (int i = 0; i < Profile.LENGTH; i++) {
			values[i] = value;
		}
		return Profile.fromArray(clusterIndex, values);
	}

	private static Series<Integer> createConstantSeries(double value) {
		var index = IntStream.range(0, Profile.LENGTH)//
				.boxed()//
				.toList();
		var values = IntStream.range(0, Profile.LENGTH)//
				.mapToObj(i -> value)//
				.toList();
		return new Series<>(index, values);
	}
}
