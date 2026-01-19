package io.openems.edge.predictor.profileclusteringmodel;

import java.time.Instant;
import java.time.LocalDate;

import io.openems.edge.predictor.api.common.TrainingError;
import io.openems.edge.predictor.api.mlcore.classification.Classifier;
import io.openems.edge.predictor.api.mlcore.clustering.Clusterer;
import io.openems.edge.predictor.api.mlcore.transformer.OneHotEncoder;

public interface TrainingCallback {

	/**
	 * Called when training completes successfully.
	 * 
	 * @param bundle the trained model
	 */
	public void onTrainingSuccess(ModelBundle bundle);

	/**
	 * Called when training fails.
	 *
	 * @param error   the training error type
	 * @param message additional detail or cause
	 */
	public void onTrainingError(TrainingError error, String message);

	public record ModelBundle(//
			Clusterer clusterer, //
			Classifier classifier, //
			OneHotEncoder<LocalDate> oneHotEncoder, //
			Instant createdAt//
	) {
	}
}
