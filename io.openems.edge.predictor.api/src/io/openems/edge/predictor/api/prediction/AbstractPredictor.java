package io.openems.edge.predictor.api.prediction;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.edge.predictor.api.prediction.Prediction.EMPTY_PREDICTION;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.OpenemsComponent;

public abstract class AbstractPredictor extends AbstractOpenemsComponent implements Predictor, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(AbstractPredictor.class);

	private final Map<ChannelAddress, Prediction> predictions = new HashMap<>();

	private LogVerbosity logVerbosity = LogVerbosity.NONE;

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
			String[] channelAddresses, LogVerbosity logVerbosity) throws OpenemsNamedException {
		super.activate(context, id, alias, enabled);
		this.logVerbosity = logVerbosity;

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
		switch (this.logVerbosity) {
		case NONE -> {
		}
		case REQUESTED_PREDICTIONS -> this.logInfo(this.log, "Prediction for [" + channelAddress + "]: " + prediction);
		}
		return prediction;
	}
}
