package io.openems.edge.predictor.api.mlcore.clustering;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;

public class AutoKMeansClustererTest {

	@Test
	public void testFit_ShouldThrowException_WhenMinKLessThanOne() {
		var df = toDataframe(List.of(//
				List.of(1.0, 1.0), //
				List.of(1.2, 1.1), //
				List.of(5.0, 5.0), //
				List.of(5.2, 5.1)));
		assertThrows(IllegalArgumentException.class, () -> {
			AutoKMeansClusterer.fit(df, 0, 3);
		});
	}

	@Test
	public void testFit_ShouldThrowException_WhenMinKGreaterThanMaxK() {
		var df = toDataframe(List.of(//
				List.of(1.0, 1.0), //
				List.of(1.2, 1.1), //
				List.of(5.0, 5.0), //
				List.of(5.2, 5.1)));
		assertThrows(IllegalArgumentException.class, () -> {
			AutoKMeansClusterer.fit(df, 5, 3);
		});
	}

	@Test
	public void testFit_ShouldCalculateCentroids() {
		var df = toDataframe(List.of(//
				List.of(1.0, 1.0), //
				List.of(1.2, 1.1), //
				List.of(5.0, 5.0), //
				List.of(5.2, 5.1)));
		var clusterer = AutoKMeansClusterer.fit(df, 1, 3);

		var centroids = clusterer.getCentroids();
		assertNotNull(centroids);
		assertTrue(centroids.size() >= 1);
	}

	@Test
	public void testFit_ShouldThrowException_WhenNullDataFrame() {
		assertThrows(IllegalArgumentException.class, () -> {
			AutoKMeansClusterer.fit(null, 1, 3);
		});
	}

	@Test
	public void testFit_ShouldThrowException_WhenEmptyDataFrame() {
		var df = toDataframe(new ArrayList<>());
		assertThrows(IllegalArgumentException.class, () -> {
			AutoKMeansClusterer.fit(df, 1, 3);
		});
	}

	@Test
	public void testFit_ShouldThrowException_WhenNaNValue() {
		var df = toDataframe(List.of(//
				List.of(1.0, Double.NaN), //
				List.of(2.0, 2.0)));
		assertThrows(IllegalArgumentException.class, () -> {
			AutoKMeansClusterer.fit(df, 1, 3);
		});
	}

	@Test
	public void testFit_ShouldThrowException_WhenNullValue() {
		var df = toDataframe(List.of(//
				Arrays.asList(1.0, null), //
				Arrays.asList(2.0, 2.0)));
		assertThrows(IllegalArgumentException.class, () -> {
			AutoKMeansClusterer.fit(df, 1, 3);
		});
	}

	@Test
	public void testFromCentroids_ShouldWork() {
		var centroids = List.of(//
				new double[] { 1.0, 2.0 }, //
				new double[] { 3.0, 4.0 });
		var clusterer = AutoKMeansClusterer.from(centroids);

		var input = toDataframe(List.of(//
				List.of(1.1, 2.1), //
				List.of(2.9, 3.8)));

		var labels = clusterer.predict(input);
		assertEquals(2, labels.size());
		for (int label : labels) {
			assertTrue(label == 0 || label == 1);
		}
	}

	@Test
	public void testFromCentroids_ShouldThrowException_WhenNullList() {
		assertThrows(IllegalArgumentException.class, () -> {
			AutoKMeansClusterer.from(null);
		});
	}

	@Test
	public void testFromCentroids_ShouldThrowException_WhenEmptyList() {
		assertThrows(IllegalArgumentException.class, () -> {
			AutoKMeansClusterer.from(new ArrayList<>());
		});
	}

	@Test
	public void testFromCentroids_ShouldThrowException_WhenDifferentLengths() {
		var centroids = List.of(//
				new double[] { 1.0, 2.0 }, //
				new double[] { 3.0 });
		assertThrows(IllegalArgumentException.class, () -> {
			AutoKMeansClusterer.from(centroids);
		});
	}

	@Test
	public void testPredict_ShouldReturnClusterLabels() {
		var df = toDataframe(List.of(//
				List.of(1.0, 1.0), //
				List.of(1.1, 1.1), //
				List.of(5.0, 5.0), //
				List.of(5.1, 5.1)));

		var clusterer = AutoKMeansClusterer.fit(df, 1, 3);
		var labels = clusterer.predict(df);

		assertEquals(4, labels.size());
		for (int label : labels) {
			assertTrue(label >= 0 && label < clusterer.getCentroids().size());
		}
	}

	@Test
	public void testGetCentroids_ShouldBeImmutableCopy() {
		var df = toDataframe(List.of(//
				List.of(1.0, 1.0), //
				List.of(5.0, 5.0), //
				List.of(10.0, 10.0)));
		var clusterer = AutoKMeansClusterer.fit(df, 1, 3);
		var centroids = clusterer.getCentroids();

		assertThrows(UnsupportedOperationException.class, () -> {
			centroids.add(new double[] { 0.0, 0.0 });
		});
	}

	private static DataFrame<Integer> toDataframe(List<List<Double>> values) {
		var index = new ArrayList<Integer>();
		var columnNames = new ArrayList<String>();

		if (!values.isEmpty()) {
			int columnCount = values.get(0).size();
			for (int i = 0; i < columnCount; i++) {
				columnNames.add("c" + i);
			}
		}

		for (int i = 0; i < values.size(); i++) {
			index.add(i);
		}

		return new DataFrame<>(index, columnNames, values);
	}
}
