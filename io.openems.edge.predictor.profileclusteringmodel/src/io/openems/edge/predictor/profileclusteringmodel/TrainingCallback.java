package io.openems.edge.predictor.profileclusteringmodel;

import java.time.Instant;
import java.time.LocalDate;

import io.openems.edge.predictor.api.mlcore.classification.Classifier;
import io.openems.edge.predictor.api.mlcore.clustering.Clusterer;
import io.openems.edge.predictor.api.mlcore.transformer.OneHotEncoder;

public interface TrainingCallback {

	/**
	 * Called when new models have been trained.
	 *
	 * @param bundle the newly trained models
	 */
	public void onModelsTrained(ModelBundle bundle);

	public record ModelBundle(//
			Clusterer clusterer, //
			Classifier classifier, //
			OneHotEncoder<LocalDate> oneHotEncoder, //
			Instant createdAt//
	) {
	}
}
