package io.openems.edge.predictor.api.test;

import static io.openems.edge.predictor.api.prediction.Prediction.EMPTY_PREDICTION;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.prediction.Predictor;

public class DummyPredictorManager extends AbstractDummyOpenemsComponent<DummyPredictorManager>
		implements PredictorManager, OpenemsComponent {

	private final List<Predictor> predictors = new ArrayList<>();

	public DummyPredictorManager(Predictor... predictors) {
		super(PredictorManager.SINGLETON_COMPONENT_ID, //
				OpenemsComponent.ChannelId.values(), //
				PredictorManager.ChannelId.values() //
		);
		Collections.addAll(this.predictors, predictors);
	}

	@Override
	protected DummyPredictorManager self() {
		return this;
	}

	/**
	 * Add a {@link Predictor}.
	 * 
	 * @param predictor the {@link Predictor}
	 */
	public void addPredictor(Predictor predictor) {
		this.predictors.add(predictor);
	}

	@Override
	public Prediction getPrediction(ChannelAddress channelAddress) {
		for (var predictor : this.predictors) {
			for (ChannelAddress pattern : predictor.getChannelAddresses()) {
				if (ChannelAddress.match(channelAddress, pattern) < 0) {
					// Predictor does not work for this ChannelAddress
					continue;
				}
				return predictor.getPrediction(channelAddress);
			}
		}
		// No matching Predictor found
		return EMPTY_PREDICTION;
	}
}
