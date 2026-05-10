package io.openems.edge.predictor.profileclusteringmodel.services;

import java.time.LocalDate;

import io.openems.edge.predictor.api.common.TrainingException;
import io.openems.edge.predictor.api.mlcore.classification.Classifier;
import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.profileclusteringmodel.ColumnNames;
import io.openems.edge.predictor.profileclusteringmodel.PredictorConfig.ClassifierFitter;
import io.openems.edge.predictor.profileclusteringmodel.training.TrainingError;

public class ClassifierTrainingService {

	private final ClassifierFitter classifierFitter;
	private final int minTrainingSamples;
	private final int maxTrainingSamples;

	public ClassifierTrainingService(//
			ClassifierFitter classifierFitter, //
			int minTrainingSamples, //
			int maxTrainingSamples) {
		this.classifierFitter = classifierFitter;
		this.minTrainingSamples = minTrainingSamples;
		this.maxTrainingSamples = maxTrainingSamples;
	}

	/**
	 * Trains a new {@link Classifier} using the given data.
	 *
	 * @param featureLabelMatrix the {@link DataFrame} containing features and the
	 *                           label column
	 * @return a trained {@link Classifier}
	 * @throws TrainingException if the number of rows in the data is less than
	 *                           {@code minTrainingSamples}
	 */
	public Classifier trainClassifier(DataFrame<LocalDate> featureLabelMatrix) throws TrainingException {
		int sampleCount = featureLabelMatrix.rowCount();

		if (sampleCount < this.minTrainingSamples) {
			throw new TrainingException(TrainingError.INSUFFICIENT_TRAINING_DATA, String.format(//
					"At least %d training samples are required, but only %d were available after feature engineering", //
					this.minTrainingSamples, //
					sampleCount));
		}

		if (sampleCount > this.maxTrainingSamples) {
			featureLabelMatrix = featureLabelMatrix.tail(this.maxTrainingSamples);
		}

		var features = featureLabelMatrix.copy();
		var label = features.getColumn(ColumnNames.LABEL);
		features.removeColumn(ColumnNames.LABEL);

		return this.classifierFitter.fit(features, label);
	}
}
