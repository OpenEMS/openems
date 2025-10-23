package io.openems.edge.predictor.api.mlcore.clustering;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.junit.Test;

public class KMeansUtilsTest {

	@Test
	public void testComputeSquaredDistance_ShouldReturnCorrectDistance() {
		double[] a = { 1.0, 2.0, 3.0 };
		double[] b = { 4.0, 6.0, 8.0 };
		double expected = 9 + 16 + 25;
		double actual = KMeansUtils.computeSquaredDistance(a, b);
		assertEquals(expected, actual, 1e-12);
	}

	@Test
	public void testComputeSquaredDistance_ShouldThrow_WhenDifferentLengths() {
		double[] a = { 1.0, 2.0 };
		double[] b = { 1.0, 2.0, 3.0 };
		assertThrows(//
				IllegalArgumentException.class, //
				() -> KMeansUtils.computeSquaredDistance(a, b));
	}

	@Test
	public void testComputeSquaredDistance_ShouldReturnZero_WhenEmptyVectors() {
		double[] a = {};
		double[] b = {};
		double result = KMeansUtils.computeSquaredDistance(a, b);
		assertEquals(0.0, result, 1e-12);
	}

	@Test
	public void testComputeInertia_ShouldReturnCorrectValue() {
		final var clusters = new ArrayList<CentroidCluster<DataPoint>>();

		var center1 = new DataPoint(new double[] { 0.0, 0.0 });
		var cluster1 = new CentroidCluster<DataPoint>(center1);
		cluster1.addPoint(new DataPoint(new double[] { 0.0, 0.0 }));
		cluster1.addPoint(new DataPoint(new double[] { 1.0, 0.0 }));

		var center2 = new DataPoint(new double[] { 2.0, 2.0 });
		var cluster2 = new CentroidCluster<DataPoint>(center2);
		cluster2.addPoint(new DataPoint(new double[] { 2.0, 2.0 }));
		cluster2.addPoint(new DataPoint(new double[] { 3.0, 2.0 }));

		clusters.add(cluster1);
		clusters.add(cluster2);

		double expected = 2.0;
		double actual = KMeansUtils.computeInertia(clusters);
		assertEquals(expected, actual, 1e-12);
	}

	@Test
	public void testComputeInertia_ShouldReturnZeroForEmptyClusters() {
		var clusters = new ArrayList<CentroidCluster<DataPoint>>();
		double inertia = KMeansUtils.computeInertia(clusters);
		assertEquals(0.0, inertia, 1e-12);
	}

	@Test
	public void testDetectKnee_ShouldReturnCorrectKForKneePoint() {
		var inertias = List.of(100.0, 50.0, 30.0, 25.0, 23.0);
		int minK = 1;
		int bestK = KMeansUtils.detectKnee(inertias, minK);
		assertEquals(3, bestK);
	}

	@Test
	public void testDetectKnee_ShouldReturnMinKForSingleValue() {
		var inertias = List.of(100.0);
		int minK = 1;
		int bestK = KMeansUtils.detectKnee(inertias, minK);
		assertEquals(1, bestK);
	}

	@Test
	public void testDetectKnee_ShouldReturnMinKForEqualValues() {
		var inertias = List.of(50.0, 50.0, 50.0, 50.0);
		int minK = 1;
		int bestK = KMeansUtils.detectKnee(inertias, minK);
		assertEquals(1, bestK);
	}

	@Test
	public void testPointToLineDistance_ShouldReturnCorrectDistance() throws Exception {
		var method = KMeansUtils.class.getDeclaredMethod("pointToLineDistance", double.class, double.class,
				double.class, double.class, double.class, double.class);
		method.setAccessible(true);

		double dist = (double) method.invoke(null, 1.0, 1.0, 0.0, 0.0, 0.0, 2.0);
		assertEquals(1.0, dist, 1e-12);

		dist = (double) method.invoke(null, 3.0, 0.0, 0.0, 0.0, 6.0, 0.0);
		assertEquals(0.0, dist, 1e-12);

		dist = (double) method.invoke(null, 3.0, 4.0, 0.0, 0.0, 6.0, 0.0);
		assertEquals(4.0, dist, 1e-12);
	}

	@Test
	public void testDetectKnee_ShouldThrow_WhenNullList() {
		assertThrows(//
				IllegalArgumentException.class, //
				() -> KMeansUtils.detectKnee(null, 1));
	}

	@Test
	public void testDetectKnee_ShouldThrow_WhenEmptyList() {
		assertThrows(//
				IllegalArgumentException.class, //
				() -> KMeansUtils.detectKnee(new ArrayList<>(), 1));
	}

	@Test
	public void testDetectKnee_ShouldThrow_WhenMinKIsNegative() {
		assertThrows(//
				IllegalArgumentException.class, //
				() -> KMeansUtils.detectKnee(List.of(1.0, 2.0, 3.0), -1));
	}
}
