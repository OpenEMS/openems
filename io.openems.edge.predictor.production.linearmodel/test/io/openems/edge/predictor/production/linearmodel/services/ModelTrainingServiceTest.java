package io.openems.edge.predictor.production.linearmodel.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.openems.edge.predictor.api.common.TrainingException;
import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;
import io.openems.edge.predictor.api.mlcore.regression.Regressor;
import io.openems.edge.predictor.production.linearmodel.ColumnNames;
import io.openems.edge.predictor.production.linearmodel.PredictorConfig.RegressorFitter;
import io.openems.edge.predictor.production.linearmodel.training.TrainingError;

@RunWith(MockitoJUnitRunner.class)
public class ModelTrainingServiceTest {

	@Mock
	private RegressorFitter regressorFitter;

	@Mock
	private Regressor regressor;

	private ZonedDateTime now = ZonedDateTime.of(2025, 9, 10, 15, 30, 0, 0, ZoneId.of("Europe/Berlin"));

	private DataFrame<ZonedDateTime> featureMatrix;
	private Series<ZonedDateTime> targetSeries;
	private DataFrame<ZonedDateTime> featureTargetMatrix;

	@Before
	public void setUp() {
		when(this.regressorFitter.fit(any(), any())).thenReturn(this.regressor);

		var index = List.of(//
				this.now.minusMinutes(30), //
				this.now.minusMinutes(15));
		var columnNames = List.of("column1");
		var values = List.of(//
				List.of(1.0), //
				List.of(2.0));
		this.featureMatrix = new DataFrame<>(index, columnNames, values);
		this.targetSeries = new Series<>(index, List.of(100.0, 200.0));
		this.featureTargetMatrix = this.featureMatrix.copy();
		this.featureTargetMatrix.setColumn(ColumnNames.TARGET, this.targetSeries);
	}

	@Test
	public void testTrainRegressor_ShouldReturnFittedModel() throws Exception {
		var sut = new ModelTrainingService(this.regressorFitter, 2, 2);
		var result = sut.trainRegressor(this.featureTargetMatrix);

		verify(this.regressorFitter).fit(eq(this.featureMatrix), eq(this.targetSeries));
		assertEquals(this.regressor, result);
	}

	@Test
	public void testTrainRegressor_ShouldThrowException_WhenNotEnoughData() {
		var sut = new ModelTrainingService(this.regressorFitter, 3, 3);
		var exception = assertThrows(TrainingException.class, () -> {
			sut.trainRegressor(this.featureTargetMatrix);
		});
		assertEquals(TrainingError.INSUFFICIENT_TRAINING_DATA, exception.getError());
		assertEquals("Insufficient data points for training", exception.getMessage());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTrainRegressor_ShouldOnlyUseMaxTrainingSamples() throws Exception {
		var sut = new ModelTrainingService(this.regressorFitter, 1, 1);
		sut.trainRegressor(this.featureTargetMatrix);

		var featuresCaptor = ArgumentCaptor.forClass(DataFrame.class);
		var targetCaptor = ArgumentCaptor.forClass(Series.class);
		verify(this.regressorFitter).fit(featuresCaptor.capture(), targetCaptor.capture());

		assertEquals(List.of(this.now.minusMinutes(15)), featuresCaptor.getValue().getIndex());
		assertEquals(List.of(this.now.minusMinutes(15)), targetCaptor.getValue().getIndex());
	}
}
