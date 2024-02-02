package io.openems.edge.predictor.api.test;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.predictor.api.prediction.AbstractPredictor;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.prediction.Predictor;

public class DummyPredictor extends AbstractPredictor implements Predictor {

	private final ClockProvider clockProvider;
	private Prediction prediction;

	public DummyPredictor(String id, ClockProvider clockProvider, Prediction prediction,
			ChannelAddress... channelAddresses) throws OpenemsNamedException {
		this(id, clockProvider, prediction, Stream.of(channelAddresses) //
				.map(ChannelAddress::toString) //
				.toArray(String[]::new));
	}

	public DummyPredictor(String id, ClockProvider clockProvider, Prediction prediction, String... channelAddresses)
			throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true, channelAddresses);
		this.clockProvider = clockProvider;
		this.prediction = prediction;
	}

	public void setPrediction(Prediction prediction) {
		this.prediction = prediction;
	}

	@Override
	protected ClockProvider getClockProvider() {
		return this.clockProvider;
	}

	protected Prediction createNewPrediction(ChannelAddress channelAddress) {
		return Prediction.from(ZonedDateTime.now(this.clockProvider.getClock()), this.prediction);
	}
}
