package io.openems.edge.predictor.api.test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.predictor.api.oneday.AbstractPredictor24Hours;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;
import io.openems.edge.predictor.api.oneday.Predictor24Hours;

public class DummyPredictor24Hours extends AbstractPredictor24Hours implements Predictor24Hours {

	private final ClockProvider clockProvider;
	private final Prediction24Hours prediction24Hours;

	public DummyPredictor24Hours(String id, ClockProvider clockProvider, Prediction24Hours prediction24Hours,
			String... channelAddresses) throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true, channelAddresses);
		this.clockProvider = clockProvider;
		this.prediction24Hours = prediction24Hours;
	}

	@Override
	protected ClockProvider getClockProvider() {
		return this.clockProvider;
	}

	@Override
	protected Prediction24Hours createNewPrediction(ChannelAddress channelAddress) {
		return this.prediction24Hours;
	}

}
