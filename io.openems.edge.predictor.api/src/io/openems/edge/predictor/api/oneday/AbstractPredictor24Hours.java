package io.openems.edge.predictor.api.oneday;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.ComponentContext;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.OpenemsComponent;

public abstract class AbstractPredictor24Hours extends AbstractOpenemsComponent
		implements Predictor24Hours, OpenemsComponent {

	protected static class PredictionContainer {
		private Prediction24Hours latestPrediction = null;
		private ZonedDateTime latestPredictionTimestamp = null;
	}

	private final Map<ChannelAddress, PredictionContainer> predictions = new HashMap<>();
	private ChannelAddress[] channelAddresses = {};

	protected abstract ClockProvider getClockProvider();

	protected abstract Prediction24Hours createNewPrediction(ChannelAddress channelAddress);

	protected AbstractPredictor24Hours(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
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
		var channelAddressesArray = new ChannelAddress[channelAddresses.length];
		for (var i = 0; i < channelAddresses.length; i++) {
			channelAddressesArray[i] = ChannelAddress.fromString(channelAddresses[i]);
		}
		this.channelAddresses = channelAddressesArray;
	}

	@Override
	public ChannelAddress[] getChannelAddresses() {
		return this.channelAddresses;
	}

	@Override
	public Prediction24Hours get24HoursPrediction(ChannelAddress channelAddress) {
		var now = roundZonedDateTimeDownTo15Minutes(ZonedDateTime.now(this.getClockProvider().getClock()));
		var container = this.predictions.get(channelAddress);
		if (container == null) {
			container = new PredictionContainer();
			this.predictions.put(channelAddress, container);
		}
		if (container.latestPredictionTimestamp == null || now.isAfter(container.latestPredictionTimestamp)) {
			// Create new prediction
			var prediction = this.createNewPrediction(channelAddress);
			container.latestPrediction = prediction;
			container.latestPredictionTimestamp = now;
		} else {
			// Reuse existing prediction
		}
		return container.latestPrediction;
	}

	/**
	 * Rounds a {@link ZonedDateTime} down to 15 minutes.
	 *
	 * @param d the {@link ZonedDateTime}
	 * @return the rounded result
	 */
	private static ZonedDateTime roundZonedDateTimeDownTo15Minutes(ZonedDateTime d) {
		var minuteOfDay = d.get(ChronoField.MINUTE_OF_DAY);
		return d.with(ChronoField.NANO_OF_DAY, 0).plus(minuteOfDay / 15 * 15, ChronoUnit.MINUTES);
	}

}
