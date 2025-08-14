package io.openems.edge.predictor.api.mlcore.classification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public class BernoulliNaiveBayesClassifierTest {

	private DataFrame<Integer> features;
	private Series<Integer> labels;

	@Before
	public void setUp() {
		var index = Arrays.asList(0, 1, 2);
		var columnNames = Arrays.asList("feature1", "feature2");
		var values = Arrays.asList(//
				Arrays.asList(1.0, 0.0), //
				Arrays.asList(0.0, 1.0), //
				Arrays.asList(1.0, 1.0));
		this.features = new DataFrame<>(index, columnNames, values);

		var labelValues = Arrays.asList(//
				0.0, //
				0.0, //
				1.0);
		this.labels = new Series<>(index, labelValues);
	}

	@Test
	public void testFitAndPredict_ShouldPredictCorrectly() {
		var index = List.of(0, 1, 2, 3, 4, 5, 6, 7);
		var columnNames = List.of("feature1", "feature2");
		var values = List.of(//
				List.of(0.0, 0.0), // Class 0
				List.of(0.0, 0.0), // Class 0
				List.of(0.0, 1.0), // Class 1
				List.of(0.0, 1.0), // Class 1
				List.of(1.0, 0.0), // Class 2
				List.of(1.0, 0.0), // Class 2
				List.of(1.0, 1.0), // Class 3
				List.of(1.0, 1.0) // Class 3
		);
		var labels = new Series<>(index, List.of(//
				0.0, // Class 0
				0.0, // Class 0
				1.0, // Class 1
				1.0, // Class 1
				2.0, // Class 2
				2.0, // Class 2
				3.0, // Class 3
				3.0 // Class 3
		));
		var features = new DataFrame<>(index, columnNames, values);

		var classifier = BernoulliNaiveBayesClassifier.fit(features, labels);

		var testValues = List.of(//
				List.of(0.0, 0.0), // Should be 0
				List.of(0.0, 1.0), // Should be 1
				List.of(1.0, 0.0), // Should be 2
				List.of(1.0, 1.0) // Should be 3
		);
		var testData = new DataFrame<>(List.of(10, 11, 12, 13), columnNames, testValues);

		var expected = List.of(0, 1, 2, 3);
		var actual = classifier.predict(testData);

		assertEquals(expected, actual);
	}

	@Test
	public void testFit_ShouldThrowException_WhenMismatchedIndices() {
		var wrongIndex = List.of(1, 2, 3);
		var wrongLabels = new Series<>(wrongIndex, List.of(0.0, 0.0, 1.0));

		assertThrows(IllegalArgumentException.class, () -> {
			BernoulliNaiveBayesClassifier.fit(this.features, wrongLabels);
		});
	}

	@Test
	public void testFit_ShouldThrowException_WhenNonBinaryFeatures() {
		var invalidValues = Arrays.asList(//
				Arrays.asList(1.0, 2.0), // 2.0 is not binary
				Arrays.asList(0.0, 1.0), //
				Arrays.asList(1.0, 1.0));
		var invalidFeatures = new DataFrame<>(List.of(0, 1, 2), List.of("feature1", "feature2"), invalidValues);

		assertThrows(IllegalArgumentException.class, () -> {
			BernoulliNaiveBayesClassifier.fit(invalidFeatures, this.labels);
		});
	}

	@Test
	public void testFit_ShouldThrowException_WhenNullLabels() {
		var nullLabelValues = Arrays.asList(0.0, null, 1.0);
		var nullLabels = new Series<>(List.of(0, 1, 2), nullLabelValues);

		assertThrows(IllegalArgumentException.class, () -> {
			BernoulliNaiveBayesClassifier.fit(this.features, nullLabels);
		});
	}

	@Test
	public void testPredict_ShouldThrowException_WhenIncorrectFeatureCount() {
		var classifier = BernoulliNaiveBayesClassifier.fit(this.features, this.labels);

		var invalidTestValues = List.of(List.of(1.0, 0.0, 1.0)); // 3 features instead of 2
		var invalidTestData = new DataFrame<>(//
				List.of(0), //
				List.of("feature1", "feature2", "feature3"), invalidTestValues);

		assertThrows(IllegalArgumentException.class, () -> {
			classifier.predict(invalidTestData);
		});
	}

	@Test
	public void testPredict_ShouldHandleSingleSample() {
		var classifier = BernoulliNaiveBayesClassifier.fit(this.features, this.labels);

		var sample = List.of(1.0, 0.0);
		int actual = classifier.predict(sample);

		assertEquals(0, actual);
	}

	@Test
	public void testPredict_ShouldHandleUnseenFeatureCombination() {
		var classifier = BernoulliNaiveBayesClassifier.fit(this.features, this.labels);

		var unseen = List.of(List.of(0.0, 0.0));
		var testData = new DataFrame<>(List.of(0), List.of("feature1", "feature2"), unseen);

		var prediction = classifier.predict(testData);

		assertEquals(1, prediction.size());
		assertTrue(prediction.get(0) == 0 || prediction.get(0) == 1);
	}

	@Test
	public void testPredict_ShouldNotFailDueToLaplaceSmoothing_WhenZeroFeatureCount() {
		var index = List.of(0, 1);
		var columnNames = List.of("feature1");
		var values = List.of(List.of(0.0), List.of(1.0));
		var labels = new Series<>(index, List.of(0.0, 1.0));
		var features = new DataFrame<>(index, columnNames, values);

		var classifier = BernoulliNaiveBayesClassifier.fit(features, labels);

		var testSample = List.of(1.0);

		var prediction = classifier.predict(testSample);
		assertTrue(prediction == 0 || prediction == 1);
	}
}
