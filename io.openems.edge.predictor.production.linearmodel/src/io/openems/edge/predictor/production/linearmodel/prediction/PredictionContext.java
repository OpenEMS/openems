package io.openems.edge.predictor.production.linearmodel.prediction;

import java.time.Clock;

import io.openems.edge.predictor.api.mlcore.regression.Regressor;
import io.openems.edge.weather.api.Weather;

public record PredictionContext(//
		Weather weather, //
		Clock clock, //
		int forecastQuarters, //
		Regressor regressor) {
}
