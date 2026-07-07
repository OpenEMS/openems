package io.openems.edge.predictor.api.mlcore.classification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public class BernoulliNaiveBayesClassifier implements Classifier {

	private static final double ALPHA = 1.0;

	private final Set<Integer> classes;
	private final int numFeatures;
	private final Map<Integer, Double> classPriors;
	private final Map<Integer, double[]> featureProbs;

	private BernoulliNaiveBayesClassifier(//
			Set<Integer> classes, //
			int numFeatures, //
			Map<Integer, Double> classPriors, //
			Map<Integer, double[]> featureProbs) {
		this.classes = classes;
		this.numFeatures = numFeatures;
		this.classPriors = classPriors;
		this.featureProbs = featureProbs;
	}

	/**
	 * Trains a Bernoulli Naive Bayes classifier using the given binary feature
	 * matrix and corresponding class labels.
	 *
	 * @param features A DataFrame containing binary feature values (0 or 1).
	 * @param labels   A Series containing discrete class labels for each sample.
	 * @return A trained BernoulliNaiveBayesClassifier instance.
	 * @throws IllegalArgumentException if feature and label indices do not match,
	 *                                  or if feature values are not binary, or if
	 *                                  any label is null or NaN.
	 */
	public static BernoulliNaiveBayesClassifier fit(DataFrame<?> features, Series<?> labels) {
		if (!features.getIndex().equals(labels.getIndex())) {
			throw new IllegalArgumentException("Feature and label indices do not match");
		}

		validateBinaryFeatures(features);
		validateLabels(labels);

		var numSamples = features.rowCount();
		var numFeatures = features.columnCount();

		int[][] featuresArray = features.getValues().stream()//
				.map(row -> row.stream()//
						.mapToInt(d -> (int) Math.round(d))//
						.toArray())//
				.toArray(int[][]::new);

		int[] labelsArray = labels.getValues().stream()//
				.mapToInt(d -> (int) Math.round(d))//
				.toArray();

		var classCounts = computeClassCounts(labelsArray);
		var classes = new HashSet<>(classCounts.keySet());

		var featureCountsPerClass = computeFeatureCountsPerClass(featuresArray, labelsArray, numFeatures);

		var classPriors = computeClassPriors(classes, classCounts, numSamples);
		var featureProbs = computeFeatureProbabilities(classes, classCounts, featureCountsPerClass, numFeatures);

		return new BernoulliNaiveBayesClassifier(classes, numFeatures, classPriors, featureProbs);
	}

	private static Map<Integer, Integer> computeClassCounts(int[] labels) {
		return Arrays.stream(labels)//
				.boxed()//
				.collect(Collectors.groupingBy(//
						Function.identity(), //
						Collectors.collectingAndThen(//
								Collectors.counting(), //
								Long::intValue)));
	}

	private static Map<Integer, double[]> computeFeatureCountsPerClass(//
			int[][] features, //
			int[] labels, //
			int numFeatures) {
		var featureCountsPerClass = new HashMap<Integer, double[]>();
		for (int i = 0; i < labels.length; i++) {
			int label = labels[i];

			var counts = featureCountsPerClass.computeIfAbsent(label, t -> new double[numFeatures]);

			for (int j = 0; j < numFeatures; j++) {
				counts[j] += features[i][j];
			}
		}
		return featureCountsPerClass;
	}

	private static Map<Integer, Double> computeClassPriors(//
			Set<Integer> classes, //
			Map<Integer, Integer> classCounts, //
			int numSamples) {
		var classPriors = new HashMap<Integer, Double>();
		for (int classLabel : classes) {
			classPriors.put(classLabel, classCounts.get(classLabel) / (double) numSamples);
		}
		return classPriors;
	}

	private static Map<Integer, double[]> computeFeatureProbabilities(//
			Set<Integer> classes, //
			Map<Integer, Integer> classCounts, //
			Map<Integer, double[]> featureCountsPerClass, //
			int numFeatures) {
		var featureProbs = new HashMap<Integer, double[]>();
		for (int classLabel : classes) {
			double[] featureCounts = featureCountsPerClass.get(classLabel);
			double[] conditionalProbs = new double[numFeatures];
			int classCount = classCounts.get(classLabel);

			for (int featureIndex = 0; featureIndex < numFeatures; featureIndex++) {
				conditionalProbs[featureIndex] = (featureCounts[featureIndex] + ALPHA) / (classCount + 2 * ALPHA);
			}

			featureProbs.put(classLabel, conditionalProbs);
		}
		return featureProbs;
	}

	@Override
	public List<Integer> predict(DataFrame<?> features) {
		if (features.columnCount() != this.numFeatures) {
			throw new IllegalArgumentException("Input feature count does not match trained model");
		}

		var predictions = new ArrayList<Integer>(features.rowCount());

		for (var sample : features.getValues()) {
			int predictedClass = this.predict(sample);
			predictions.add(predictedClass);
		}

		return predictions;
	}

	@Override
	public int predict(List<Double> features) {
		if (features.size() != this.numFeatures) {
			throw new IllegalArgumentException("Input feature count does not match trained model");
		}

		validateBinaryFeatures(features);

		double maxLogLikelihood = Double.NEGATIVE_INFINITY;
		int bestClass = -1;

		for (int classLabel : this.classes) {
			double logLikelihood = this.computeLogLikelihood(features, classLabel);
			if (logLikelihood > maxLogLikelihood) {
				maxLogLikelihood = logLikelihood;
				bestClass = classLabel;
			}
		}

		return bestClass;
	}

	private double computeLogLikelihood(List<Double> sample, int classLabel) {
		double logLikelihood = Math.log(this.classPriors.get(classLabel));
		double[] probs = this.featureProbs.get(classLabel);

		for (int j = 0; j < this.numFeatures; j++) {
			int x = (int) Math.round(sample.get(j));
			double p = probs[j];
			logLikelihood += x * Math.log(p) + (1 - x) * Math.log(1 - p);
		}

		return logLikelihood;
	}

	private static void validateBinaryFeatures(DataFrame<?> features) {
		for (var row : features.getValues()) {
			validateBinaryFeatures(row);
		}
	}

	private static void validateBinaryFeatures(List<Double> features) {
		for (Double value : features) {
			if (value == null || Double.isNaN(value)) {
				throw new IllegalArgumentException("Feature value is null or NaN");
			}
			int rounded = (int) Math.round(value);
			if (rounded != 0 && rounded != 1) {
				throw new IllegalArgumentException("Feature value is not binary (0 or 1)");
			}
		}
	}

	private static void validateLabels(Series<?> labels) {
		for (var label : labels.getValues()) {
			if (label == null || Double.isNaN(label)) {
				throw new IllegalArgumentException("Label is null or NaN");
			}
		}
	}
}
