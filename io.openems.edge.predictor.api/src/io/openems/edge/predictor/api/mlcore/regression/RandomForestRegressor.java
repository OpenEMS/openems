package io.openems.edge.predictor.api.mlcore.regression;

import java.util.List;
import java.util.stream.IntStream;

import org.tribuo.Example;
import org.tribuo.Feature;
import org.tribuo.MutableDataset;
import org.tribuo.common.tree.RandomForestTrainer;
import org.tribuo.datasource.ListDataSource;
import org.tribuo.ensemble.EnsembleModel;
import org.tribuo.impl.ArrayExample;
import org.tribuo.provenance.SimpleDataSourceProvenance;
import org.tribuo.regression.RegressionFactory;
import org.tribuo.regression.ensemble.AveragingCombiner;
import org.tribuo.regression.rtree.CARTRegressionTrainer;
import org.tribuo.regression.rtree.impurity.RegressorImpurity;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public class RandomForestRegressor implements Regressor {

	private static final String TARGET = "target";

	private final EnsembleModel<org.tribuo.regression.Regressor> model;

	private RandomForestRegressor(EnsembleModel<org.tribuo.regression.Regressor> model) {
		this.model = model;
	}

	/**
	 * Fits an {@link RandomForestRegressor} model to the given feature matrix and
	 * target series.
	 *
	 * @param featureMatrix the input feature matrix
	 * @param targetSeries  the target values corresponding to the feature matrix
	 * @param config        the configuration parameters for the training
	 * @return a trained {@link RandomForestRegressor} model
	 */
	public static RandomForestRegressor fit(DataFrame<?> featureMatrix, Series<?> targetSeries, Config config) {
		var examples = toExamples(featureMatrix, targetSeries);

		var source = new ListDataSource<>(//
				examples, //
				new RegressionFactory(), //
				new SimpleDataSourceProvenance("random-forest-external-data", new RegressionFactory()));
		var dataset = new MutableDataset<>(source);

		var trainer = new RandomForestTrainer<>(//
				new CARTRegressionTrainer(//
						config.maxDepth(), //
						config.minChildWeight(), //
						config.minImpurityDecrease(), //
						config.fractionFeaturesInSplit(), //
						config.useRandomSplitPoints(), //
						config.impurity(), //
						config.seed()), //
				new AveragingCombiner(), //
				config.numTrees(), //
				config.seed());

		var model = trainer.train(dataset);

		return new RandomForestRegressor(model);
	}

	@Override
	public List<Double> predict(DataFrame<?> featureMatrix) {
		var examples = toExamples(featureMatrix);

		var prediction = this.model.predict(examples);

		return prediction.stream()//
				.map(p -> p.getOutput().getValues()[0])//
				.toList();
	}

	private static List<Example<org.tribuo.regression.Regressor>> toExamples(DataFrame<?> featureMatrix,
			Series<?> targetSeries) {
		return IntStream.range(0, featureMatrix.rowCount())//
				.mapToObj(i -> {
					var targetValue = new org.tribuo.regression.Regressor(TARGET, targetSeries.getAt(i));
					var features = IntStream.range(0, featureMatrix.columnCount())//
							.mapToObj(j -> new Feature(//
									featureMatrix.getColumnNames().get(j), //
									featureMatrix.getValueAt(i, j)))//
							.toList();
					return (Example<org.tribuo.regression.Regressor>) new ArrayExample<>(targetValue, features);
				})//
				.toList();
	}

	private static List<Example<org.tribuo.regression.Regressor>> toExamples(DataFrame<?> featureMatrix) {
		return IntStream.range(0, featureMatrix.rowCount())//
				.mapToObj(i -> {
					var targetValue = new org.tribuo.regression.Regressor(TARGET, Double.NaN);
					var features = IntStream.range(0, featureMatrix.columnCount())//
							.mapToObj(j -> new Feature(//
									featureMatrix.getColumnNames().get(j), //
									featureMatrix.getValueAt(i, j)))//
							.toList();
					return (Example<org.tribuo.regression.Regressor>) new ArrayExample<>(targetValue, features);
				})//
				.toList();
	}

	public record Config(//
			int numTrees, //
			int maxDepth, //
			float minChildWeight, //
			float minImpurityDecrease, //
			float fractionFeaturesInSplit, //
			boolean useRandomSplitPoints, //
			RegressorImpurity impurity, //
			long seed) {
	}
}
