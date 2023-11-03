package io.openems.edge.predictor.api.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;
import io.openems.edge.predictor.api.oneday.Predictor24Hours;

public class DummyPredictorManager extends AbstractDummyOpenemsComponent<DummyPredictorManager>
		implements PredictorManager, OpenemsComponent {

	private final List<Predictor24Hours> predictors = new ArrayList<>();

	public DummyPredictorManager(Predictor24Hours... predictors) {
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
	 * Add a {@link Predictor24Hours}.
	 * 
	 * @param predictor the {@link Predictor24Hours}
	 */
	public void addPredictor(Predictor24Hours predictor) {
		this.predictors.add(predictor);
	}

	@Override
	public Prediction24Hours get24HoursPrediction(ChannelAddress channelAddress) {
		for (Predictor24Hours predictor : this.predictors) {
			for (ChannelAddress pattern : predictor.getChannelAddresses()) {
				if (ChannelAddress.match(channelAddress, pattern) < 0) {
					// Predictor does not work for this ChannelAddress
					continue;
				}
				return predictor.get24HoursPrediction(channelAddress);
			}
		}
		// No matching Predictor found
		return Prediction24Hours.EMPTY;
	}
}
