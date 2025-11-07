package io.openems.edge.predictor.profileclusteringmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.stream.IntStream;

import org.junit.Test;

import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public class ProfileTest {

	@Test
	public void testProfile_ShouldCreateProfile() {
		var index = IntStream.range(0, Profile.LENGTH)//
				.boxed()//
				.toList();
		var values = IntStream.range(0, Profile.LENGTH)//
				.asDoubleStream()//
				.boxed()//
				.toList();

		var series = new Series<>(index, values);
		var profile = new Profile(1, series);

		assertEquals(1, profile.clusterIndex());
		assertEquals(Profile.LENGTH, profile.values().size());
	}

	@Test
	public void testProfile_ShouldThrowException_WhenSeriesLengthIsInvalid() {
		var index = IntStream.range(0, 50)//
				.boxed()//
				.toList();
		var values = IntStream.range(0, 50)//
				.asDoubleStream()//
				.boxed()//
				.toList();

		var series = new Series<>(index, values);

		assertThrows(IllegalArgumentException.class, () -> {
			new Profile(1, series);
		});
	}

	@Test
	public void testFromArray_ShouldCreateProfile() {
		double[] values = IntStream.range(0, Profile.LENGTH)//
				.asDoubleStream()//
				.toArray();
		var profile = Profile.fromArray(2, values);

		assertEquals(2, profile.clusterIndex());
		assertEquals(Profile.LENGTH, profile.values().size());
	}

	@Test
	public void testFromArray_ShouldThrowException_WhenArrayLengthIsInvalid() {
		double[] values = new double[50];

		assertThrows(IllegalArgumentException.class, () -> {
			Profile.fromArray(2, values);
		});
	}
}
