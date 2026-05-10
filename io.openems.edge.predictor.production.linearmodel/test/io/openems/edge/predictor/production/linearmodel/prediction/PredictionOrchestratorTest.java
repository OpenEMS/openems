package io.openems.edge.predictor.production.linearmodel.prediction;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.regression.Regressor;
import io.openems.edge.predictor.production.linearmodel.services.FeatureEngineeringService;
import io.openems.edge.predictor.production.linearmodel.services.PredictionDataService;
import io.openems.edge.weather.api.Weather;

@RunWith(MockitoJUnitRunner.class)
public class PredictionOrchestratorTest {

	@Mock
	private PredictionDataService predictionDataService;

	@Mock
	private FeatureEngineeringService featureEngineeringService;

	@Mock
	private Regressor regressor;

	private ZonedDateTime now = ZonedDateTime.of(2025, 9, 10, 15, 30, 0, 0, ZoneId.of("Europe/Berlin"));

	@SuppressWarnings("unchecked")
	@Test
	public void testRunPrediction_ShouldPredictExpectedValues() throws Exception {
		var predictionContext = new PredictionContext(//
				mock(Weather.class), //
				mock(Clock.class), //
				1, //
				this.regressor);

		var sut = new PredictionOrchestrator(//
				predictionContext, //
				this.predictionDataService, //
				this.featureEngineeringService);

		var rawFeatureMatrix = mock(DataFrame.class);
		var transformedFeatureMatrix = new DataFrame<>(//
				List.of(this.now, this.now.plusMinutes(15)), //
				List.of("column1"), //
				List.of(//
						List.of(1.0), //
						List.of(2.0)));

		when(this.predictionDataService.prepareFeatureMatrix(1)).thenReturn(rawFeatureMatrix);
		when(this.featureEngineeringService.transformForPrediction(rawFeatureMatrix))
				.thenReturn(transformedFeatureMatrix);
		when(this.regressor.predict(transformedFeatureMatrix)).thenReturn(List.of(100.0, 200.0));

		final var result = sut.runPrediction();

		verify(this.predictionDataService).prepareFeatureMatrix(1);
		verify(this.featureEngineeringService).transformForPrediction(eq(rawFeatureMatrix));
		verify(this.regressor).predict(eq(transformedFeatureMatrix));
		assertEquals(List.of(this.now, this.now.plusMinutes(15)), result.getIndex());
		assertEquals(List.of(100.0, 200.0), result.getValues());
	}
}
