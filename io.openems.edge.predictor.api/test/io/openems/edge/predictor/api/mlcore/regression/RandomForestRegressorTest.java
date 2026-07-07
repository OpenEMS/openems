package io.openems.edge.predictor.api.mlcore.regression;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.tribuo.regression.rtree.impurity.MeanSquaredError;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;

public class RandomForestRegressorTest {

	@Test
	public void testFitAndPredict_ShouldReturnPredictionsMatchingTargetCount() {
		var featureNames = List.of("feature1");
		var features = List.of(//
				List.of(1.0), //
				List.of(1.0), //
				List.of(1.0), //
				List.of(1.0));
		var targets = List.of(2.0, 4.0, 6.0, 8.0);

		var index = List.of(0, 1, 2, 3);
		var x = new DataFrame<>(index, featureNames, features);
		var y = new Series<>(index, targets);

		var config = new RandomForestRegressor.Config(//
				25, // numTrees
				12, // maxDepth
				1.0f, // minChildWeight
				1e-8f, // minImpurityDecrease
				0.3f, // fractionFeaturesInSplit
				false, // useRandomSplitPoints
				new MeanSquaredError(), // impurity
				42L // seed
		);

		var model = RandomForestRegressor.fit(x, y, config);
		var preds = model.predict(x);

		assertEquals(targets.size(), preds.size());
	}
}
