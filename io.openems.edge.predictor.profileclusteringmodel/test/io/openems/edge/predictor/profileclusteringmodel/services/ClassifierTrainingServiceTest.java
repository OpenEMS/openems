package io.openems.edge.predictor.profileclusteringmodel.services;

import static io.openems.edge.predictor.profileclusteringmodel.ColumnNames.LABEL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import io.openems.edge.predictor.api.mlcore.classification.Classifier;
import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.profileclusteringmodel.PredictorConfig.ClassifierFitter;

@RunWith(MockitoJUnitRunner.class)
public class ClassifierTrainingServiceTest {

	private static final String COLUMN_FEATURE = "feature";

	@Test
	public void testTrainClassifier_ShouldCallFitWithCorrectFeaturesAndLabels() {
		int minTrainingSamplesRequired = 3;

		var classifierFitter = mock(ClassifierFitter.class);
		var classifier = mock(Classifier.class);

		var index = List.of(//
				LocalDate.of(2025, 7, 8), //
				LocalDate.of(2025, 7, 9), //
				LocalDate.of(2025, 7, 10));
		var columnNames = List.of(//
				LABEL, //
				COLUMN_FEATURE);
		var values = List.of(//
				List.of(1.0, 1.0), //
				List.of(2.0, 1.0), //
				List.of(3.0, 0.0));
		var featureMatrix = new DataFrame<>(index, columnNames, values);

		var expectedFeatures = featureMatrix.copy();
		var expectedLabels = featureMatrix.getColumn(LABEL);
		expectedFeatures.removeColumn(LABEL);

		when(classifierFitter.fit(eq(expectedFeatures), eq(expectedLabels))).thenReturn(classifier);

		var service = new ClassifierTrainingService(classifierFitter, minTrainingSamplesRequired);

		var result = service.trainClassifier(featureMatrix);

		assertEquals(classifier, result);
		verify(classifierFitter).fit(eq(expectedFeatures), eq(expectedLabels));
	}

	@Test
	public void testTrainClassifier_SchouldThrowException_WhenNotEnoughData() {
		int minTrainingSamplesRequired = 3;

		ClassifierFitter classifierFitter = mock(ClassifierFitter.class);

		var index = List.of(//
				LocalDate.of(2025, 7, 8));
		var columnNames = List.of(//
				LABEL, //
				COLUMN_FEATURE);
		var values = List.of(//
				List.of(1.0, 1.0));
		var featureMatrix = new DataFrame<>(index, columnNames, values);

		var service = new ClassifierTrainingService(classifierFitter, minTrainingSamplesRequired);

		assertThrows(IllegalStateException.class, () -> {
			service.trainClassifier(featureMatrix);
		});
	}
}
