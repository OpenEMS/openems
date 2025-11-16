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
	private final int minTrainingSamplesRequired;

	public ClassifierTrainingService(//
			ClassifierFitter classifierFitter, //
			int minTrainingSamplesRequired) {
		this.classifierFitter = classifierFitter;
		this.minTrainingSamplesRequired = minTrainingSamplesRequired;
	}

	/**
	 * Trains a new {@link Classifier} using the given data.
	 *
	 * @param dataframe the input data containing both features and labels
	 * @return a trained {@link Classifier}
	 * @throws TrainingException if the number of rows in the data is less than
	 *                           {@code minTrainingSamplesRequired}
	 */
	public Classifier trainClassifier(DataFrame<LocalDate> dataframe) throws TrainingException {
		if (dataframe.rowCount() < this.minTrainingSamplesRequired) {
			throw new TrainingException(TrainingError.INSUFFICIENT_TRAINING_DATA, String.format(//
					"At least %d historical consumption profiles are required, but only %d were available after feature engineering", //
					this.minTrainingSamplesRequired, //
					dataframe.rowCount()));
		}

		var features = dataframe.copy();
		var label = features.getColumn(ColumnNames.LABEL);
		features.removeColumn(ColumnNames.LABEL);

		return this.classifierFitter.fit(features, label);
	}
}
