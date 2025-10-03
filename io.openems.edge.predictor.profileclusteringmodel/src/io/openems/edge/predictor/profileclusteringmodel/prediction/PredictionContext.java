package io.openems.edge.predictor.profileclusteringmodel.prediction;

import java.time.Clock;
import java.time.LocalDate;
import java.util.function.Supplier;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.meta.types.SubdivisionCode;
import io.openems.edge.predictor.api.mlcore.classification.Classifier;
import io.openems.edge.predictor.api.mlcore.clustering.Clusterer;
import io.openems.edge.predictor.api.mlcore.transformer.OneHotEncoder;
import io.openems.edge.predictor.profileclusteringmodel.CurrentProfile;
import io.openems.edge.predictor.profileclusteringmodel.PredictorConfig.ProfileSwitcherFactory;
import io.openems.edge.timedata.api.Timedata;

public record PredictionContext(//
		Supplier<Clock> clockSupplier, //
		Timedata timedata, //
		ChannelAddress channelAddress, //
		int maxGapSizeInterpolation, //
		Clusterer clusterer, //
		Classifier classifier, //
		OneHotEncoder<LocalDate> oneHotEncoder, //
		Supplier<SubdivisionCode> subdivisionCodeSupplier, //
		ProfileSwitcherFactory profileSwitcherFactory, //
		CurrentProfile currentProfile) {
}
