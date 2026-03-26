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
		var upperQuantileValues = IntStream.range(100, Profile.LENGTH + 100)//
				.asDoubleStream()//
				.boxed()//
				.toList();

		var valuesSeries = new Series<>(index, values);
		var upperQuantileValuesSeries = new Series<>(index, upperQuantileValues);
		var profile = new Profile(1, valuesSeries, upperQuantileValuesSeries);

		assertEquals(1, profile.clusterIndex());
		assertEquals(Profile.LENGTH, profile.values().size());
	}

	@Test
	public void testProfile_ShouldThrowException_WhenSeriesLengthIsInvalid() {
		var indexShort = IntStream.range(0, 50)//
				.boxed()//
				.toList();
		var valuesShort = IntStream.range(0, 50)//
				.asDoubleStream()//
				.boxed()//
				.toList();
		var indexLong = IntStream.range(0, Profile.LENGTH)//
				.boxed()//
				.toList();
		var valuesLong = IntStream.range(0, Profile.LENGTH)//
				.asDoubleStream()//
				.boxed()//
				.toList();

		var seriesShort = new Series<>(indexShort, valuesShort);
		var seriesLong = new Series<>(indexLong, valuesLong);

		assertThrows(IllegalArgumentException.class, () -> {
			new Profile(1, seriesShort, seriesLong);
		});
		assertThrows(IllegalArgumentException.class, () -> {
			new Profile(1, seriesLong, seriesShort);
		});
	}

	@Test
	public void testFromArray_ShouldCreateProfile() {
		double[] values = IntStream.range(0, Profile.LENGTH)//
				.asDoubleStream()//
				.toArray();
		double[] upperQuantileValues = IntStream.range(100, Profile.LENGTH + 100)//
				.asDoubleStream()//
				.toArray();
		var profile = Profile.fromArray(2, values, upperQuantileValues);

		assertEquals(2, profile.clusterIndex());
		assertEquals(Profile.LENGTH, profile.values().size());
		assertEquals(Profile.LENGTH, profile.upperQuantileValues().size());
	}

	@Test
	public void testFromArray_ShouldThrowException_WhenArrayLengthIsInvalid() {
		double[] valuesShort = new double[50];
		double[] valuesLong = new double[Profile.LENGTH];

		assertThrows(IllegalArgumentException.class, () -> {
			Profile.fromArray(2, valuesShort, valuesLong);
		});
		assertThrows(IllegalArgumentException.class, () -> {
			Profile.fromArray(2, valuesLong, valuesShort);
		});
	}
}
