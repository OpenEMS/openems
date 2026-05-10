package io.openems.edge.predictor.profileclusteringmodel.training;

import java.time.Clock;
import java.util.function.Supplier;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.meta.types.SubdivisionCode;
import io.openems.edge.predictor.profileclusteringmodel.PredictorConfig.ClassifierFitter;
import io.openems.edge.predictor.profileclusteringmodel.PredictorConfig.ClustererFitter;
import io.openems.edge.predictor.profileclusteringmodel.TrainingCallback;
import io.openems.edge.timedata.api.Timedata;

public record TrainingContext(//
		TrainingCallback callback, //
		Supplier<Clock> clockSupplier, //
		Timedata timedata, //
		ChannelAddress channelAddress, //
		int trainingWindowInDays, //
		int maxGapSizeInterpolation, //
		int minTrainingSamples, //
		int maxTrainingSamples, //
		ClustererFitter clustererFitter, //
		ClassifierFitter classifierFitter, //
		Supplier<SubdivisionCode> subdivisionCodeSupplier) {
}
