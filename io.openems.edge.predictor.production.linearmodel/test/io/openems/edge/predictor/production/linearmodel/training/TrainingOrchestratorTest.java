package io.openems.edge.predictor.production.linearmodel.training;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.regression.Regressor;
import io.openems.edge.predictor.production.linearmodel.PredictorConfig.RegressorFitter;
import io.openems.edge.predictor.production.linearmodel.TrainingCallback;
import io.openems.edge.predictor.production.linearmodel.TrainingCallback.ModelBundle;
import io.openems.edge.predictor.production.linearmodel.services.FeatureEngineeringService;
import io.openems.edge.predictor.production.linearmodel.services.ModelTrainingService;
import io.openems.edge.predictor.production.linearmodel.services.TrainingDataService;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.weather.api.Weather;

@RunWith(MockitoJUnitRunner.class)
public class TrainingOrchestratorTest {

	@Mock
	private TrainingDataService trainingDataService;

	@Mock
	private FeatureEngineeringService featureEngineeringService;

	@Mock
	private ModelTrainingService modelTrainingService;

	@Mock
	private Regressor regressor;

	private ZonedDateTime now = ZonedDateTime.of(2025, 9, 10, 15, 30, 0, 0, ZoneId.of("Europe/Berlin"));
	private Clock clock = Clock.fixed(this.now.toInstant(), this.now.getZone());

	@SuppressWarnings("unchecked")
	@Test
	public void testRunTraining_ShouldCallServicesAndTriggerCallback() throws Exception {
		var trainingCallback = mock(TrainingCallback.class);
		var trainingContext = new TrainingContext(//
				trainingCallback, //
				() -> this.clock, //
				mock(Timedata.class), //
				mock(Weather.class), //
				mock(ChannelAddress.class), //
				2, //
				mock(RegressorFitter.class), //
				2, //
				2);

		var sut = new TrainingOrchestrator(//
				trainingContext, //
				this.trainingDataService, //
				this.featureEngineeringService, //
				this.modelTrainingService);

		var rawFeatureTargetMatrix = mock(DataFrame.class);
		var transformedFeatureTargetMatrix = mock(DataFrame.class);

		when(this.trainingDataService.prepareFeatureTargetMatrix(any(), any())).thenReturn(rawFeatureTargetMatrix);
		when(this.featureEngineeringService.transformForTraining(rawFeatureTargetMatrix))
				.thenReturn(transformedFeatureTargetMatrix);
		when(this.modelTrainingService.trainRegressor(transformedFeatureTargetMatrix)).thenReturn(this.regressor);

		final var result = sut.runTraining();

		verify(this.trainingDataService).prepareFeatureTargetMatrix(//
				eq(this.now.minusMinutes(2 * 15)), //
				eq(this.now));
		verify(this.featureEngineeringService).transformForTraining(eq(rawFeatureTargetMatrix));
		verify(this.modelTrainingService).trainRegressor(eq(transformedFeatureTargetMatrix));
		assertEquals(new ModelBundle(this.regressor, this.clock.instant()), result);
	}
}
