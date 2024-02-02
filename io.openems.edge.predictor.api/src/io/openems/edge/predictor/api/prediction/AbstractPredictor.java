package io.openems.edge.predictor.api.prediction;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.edge.predictor.api.prediction.Prediction.EMPTY_PREDICTION;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.ComponentContext;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.OpenemsComponent;

public abstract class AbstractPredictor extends AbstractOpenemsComponent implements Predictor, OpenemsComponent {

	private final Map<ChannelAddress, Prediction> predictions = new HashMap<>();

	protected abstract ClockProvider getClockProvider();

	protected abstract Prediction createNewPrediction(ChannelAddress channelAddress);

	protected AbstractPredictor(//
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Override
	protected final void activate(ComponentContext context, String id, String alias, boolean enabled) {
		throw new IllegalArgumentException("use the other activate method!");
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled,
			String[] channelAddresses) throws OpenemsNamedException {
		super.activate(context, id, alias, enabled);

		for (var i = 0; i < channelAddresses.length; i++) {
			this.predictions.put(ChannelAddress.fromString(channelAddresses[i]), EMPTY_PREDICTION);
		}
	}

	@Override
	public ChannelAddress[] getChannelAddresses() {
		return this.predictions.keySet().toArray(ChannelAddress[]::new);
	}

	@Override
	public Prediction getPrediction(ChannelAddress channelAddress) {
		var now = roundDownToQuarter(ZonedDateTime.now(this.getClockProvider().getClock()));
		var prediction = this.predictions.get(channelAddress);
		if (prediction == null || prediction.isEmpty() || now.isAfter(prediction.valuePerQuarter.firstKey())) {
			// Create new prediction
			prediction = this.createNewPrediction(channelAddress);
			this.predictions.put(channelAddress, prediction);
		} else {
			// Reuse existing prediction
		}
		return prediction;
	}
}
