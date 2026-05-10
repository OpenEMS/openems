package io.openems.edge.predictor.production.linearmodel.training;

import java.time.Clock;
import java.util.function.Supplier;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.predictor.production.linearmodel.PredictorConfig.RegressorFitter;
import io.openems.edge.predictor.production.linearmodel.TrainingCallback;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.weather.api.Weather;

public record TrainingContext(//
		TrainingCallback callback, //
		Supplier<Clock> clockSupplier, //
		Timedata timedata, //
		Weather weather, //
		ChannelAddress productionChannelAddress, //
		int trainingWindowInQuarters, //
		RegressorFitter regressorFitter, //
		int minTrainingSamples, //
		int maxTrainingSamples) {
}
