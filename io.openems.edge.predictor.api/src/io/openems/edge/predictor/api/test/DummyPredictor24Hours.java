package io.openems.edge.predictor.api.test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

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
	private DummyPrediction24Hours prediction24Hours;

	public DummyPredictor24Hours(String id, ClockProvider clockProvider, DummyPrediction24Hours prediction24Hours,
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

	public void setPrediction24Hours(DummyPrediction24Hours prediction24Hours) {
		this.prediction24Hours = prediction24Hours;
	}

	@Override
	protected ClockProvider getClockProvider() {
		return this.clockProvider;
	}

	@Override
	protected Prediction24Hours createNewPrediction(ChannelAddress channelAddress) {

		var now = ZonedDateTime.now(this.clockProvider.getClock()).withZoneSameInstant(ZoneOffset.UTC);
		now = roundZonedDateTimeDownTo15Minutes(now);

		var quarterHourIndex = now.get(ChronoField.MINUTE_OF_DAY) / 15;
		var values = this.prediction24Hours.getValues();
		var adjustedValues = new Integer[Prediction24Hours.NUMBER_OF_VALUES];

		for (int i = quarterHourIndex, y = 0; i < quarterHourIndex + Prediction24Hours.NUMBER_OF_VALUES
				&& i < values.length; i++, y++) {
			adjustedValues[y] = values[i];
		}

		return new Prediction24Hours(adjustedValues);
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
