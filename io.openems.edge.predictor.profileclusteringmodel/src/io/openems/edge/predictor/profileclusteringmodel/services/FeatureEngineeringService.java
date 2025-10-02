package io.openems.edge.predictor.profileclusteringmodel.services;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import io.openems.edge.common.meta.types.SubdivisionCode;
import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.transformer.DataFrameTransformerPipeline;
import io.openems.edge.predictor.api.mlcore.transformer.DayOfWeekFeatureTransformer;
import io.openems.edge.predictor.api.mlcore.transformer.DropColumnsTransformer;
import io.openems.edge.predictor.api.mlcore.transformer.DropNaTransformer;
import io.openems.edge.predictor.api.mlcore.transformer.IsWorkingDayFeatureTransformer;
import io.openems.edge.predictor.api.mlcore.transformer.LagFeatureTransformer;
import io.openems.edge.predictor.api.mlcore.transformer.LagValidationTransformer;
import io.openems.edge.predictor.api.mlcore.transformer.OneHotEncoder;
import io.openems.edge.predictor.profileclusteringmodel.ColumnNames;

public class FeatureEngineeringService {

	private final Supplier<SubdivisionCode> subdivisionCodeSupplier;

	public FeatureEngineeringService(Supplier<SubdivisionCode> subdivisionCodeSupplier) {
		this.subdivisionCodeSupplier = subdivisionCodeSupplier;
	}

	/**
	 * Transforms the raw feature-label matrix into a processed form for training.
	 * 
	 * <p>
	 * This applies transformations such as day-of-week extraction, working day
	 * encoding, lag feature creation and validation, handling of missing values,
	 * and one-hot encoding of categorical variables.
	 *
	 * @param rawFeatureLabelMatrix the raw input {@link DataFrame} containing
	 *                              features and labels
	 * @return a {@link FeatureEngineeringTrainingResult} holding the transformed
	 *         feature-label matrix and the fitted {@link OneHotEncoder}
	 */
	public FeatureEngineeringTrainingResult transformForTraining(DataFrame<LocalDate> rawFeatureLabelMatrix) {
		var oneHotEncoder = new OneHotEncoder<LocalDate>(//
				List.of(ColumnNames.DAY_OF_WEEK, ColumnNames.LABEL_LAG_1_DAY), //
				true);

		var pipeline = new DataFrameTransformerPipeline<LocalDate>(List.of(//
				new DayOfWeekFeatureTransformer<LocalDate>(ColumnNames.DAY_OF_WEEK, LocalDate::getDayOfWeek), //
				new IsWorkingDayFeatureTransformer<LocalDate>(//
						ColumnNames.IS_WORKING_DAY, //
						this.subdivisionCodeSupplier.get(), //
						Function.identity()), //
				new LagFeatureTransformer<LocalDate>(ColumnNames.LABEL, 1, ColumnNames.LABEL_LAG_1_DAY), //
				new LagValidationTransformer<LocalDate>(ColumnNames.LABEL_LAG_1_DAY, 1, ChronoUnit.DAYS), //
				new DropNaTransformer<LocalDate>(), //
				oneHotEncoder));

		var transformedFeatureLabelMatrix = pipeline.transform(rawFeatureLabelMatrix);

		return new FeatureEngineeringTrainingResult(transformedFeatureLabelMatrix, oneHotEncoder);
	}

	/**
	 * Transforms the base feature matrix for prediction by applying the same
	 * transformations as during training, except label removal and one-hot
	 * encoding.
	 * 
	 * <p>
	 * This step prepares the base matrix so that lag features and calendar features
	 * are aligned with the training setup.
	 *
	 * @param baseFeatureMatrix the raw feature matrix without labels
	 * @return the transformed base feature matrix
	 */
	public DataFrame<LocalDate> transformBaseFeatureMatrixForPrediction(DataFrame<LocalDate> baseFeatureMatrix) {
		var pipeline = new DataFrameTransformerPipeline<LocalDate>(List.of(//
				new DayOfWeekFeatureTransformer<LocalDate>(ColumnNames.DAY_OF_WEEK, LocalDate::getDayOfWeek), //
				new IsWorkingDayFeatureTransformer<LocalDate>(//
						ColumnNames.IS_WORKING_DAY, //
						this.subdivisionCodeSupplier.get(), //
						Function.identity()), //
				new LagFeatureTransformer<LocalDate>(ColumnNames.LABEL, 1, ColumnNames.LABEL_LAG_1_DAY), //
				new LagValidationTransformer<LocalDate>(ColumnNames.LABEL_LAG_1_DAY, 1, ChronoUnit.DAYS)));

		var trasformedBaseFeatureMatrix = pipeline.transform(baseFeatureMatrix);

		return trasformedBaseFeatureMatrix;
	}

	/**
	 * Prepares the feature matrix for prediction by dropping the label column,
	 * removing rows with missing data, and applying one-hot encoding.
	 *
	 * @param baseFeatureMatrix the DataFrame with base features
	 * @param oneHotEncoder     the encoder fitted during training
	 * @return the final feature matrix ready for prediction
	 */
	public DataFrame<LocalDate> transformForPrediction(//
			DataFrame<LocalDate> baseFeatureMatrix, //
			OneHotEncoder<LocalDate> oneHotEncoder) {
		var pipeline = new DataFrameTransformerPipeline<LocalDate>(List.of(//
				new DropColumnsTransformer<LocalDate>(List.of(ColumnNames.LABEL)), //
				new DropNaTransformer<LocalDate>(), //
				oneHotEncoder));

		var predictionFeatureMatrix = pipeline.transform(baseFeatureMatrix);

		return predictionFeatureMatrix;
	}

	public record FeatureEngineeringTrainingResult(//
			DataFrame<LocalDate> featureLabelMatrix, //
			OneHotEncoder<LocalDate> oneHotEncoder//
	) {
	}
}
