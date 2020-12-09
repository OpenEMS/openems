package io.openems.edge.predictor.holtwinters;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;
import io.openems.edge.predictor.api.oneday.Predictor24Hours;
import io.openems.edge.predictor.holtwinters.lib.HoltWinters;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.test.DummyTimedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Predictor.HoltWinters", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class HoltWintersPredictorImpl extends AbstractOpenemsComponent implements Predictor24Hours, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(HoltWintersPredictorImpl.class);

	private final static double ALPHA = 0.01;
	private final static double BETA = 0.01;
	private final static double GAMMA = 0.01;

	// private final static double ALPHA = 0.2; // just some reasonable
	// private final static double BETA = 0.2; // default parameter values.
	// private final static double GAMMA = 0.1; // source:
	// https://robjhyndman.com/hyndsight/hw-initialization/#comment-358754180
	private final static int ENTRIES_PER_PERIOD = 96; // 96 x 15 minutes values
	private final static int NUMBER_OF_INPUT_SEASONS = 7; // days for input data

//	@Reference
	private Timedata timedata;

	@Reference
	private ComponentManager componentManager;

	private ChannelAddress[] channelAddresses = null;

	private static class PredictionContainer {
		private Prediction24Hours latestPrediction = null;
		private ZonedDateTime latestPredictionTimestamp = null;
	}

	private final Map<ChannelAddress, PredictionContainer> predictions = new HashMap<>();

	public HoltWintersPredictorImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				HoltWintersPredictor.ChannelId.values() //
		);

		int[] values = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 9, 146, 348, 636, 1192, 2092,
				2882, 3181, 3850, 5169, 6005, 6710, 7372, 8138, 8918, 9736, 10615, 11281, 11898, 12435, 11982, 14287,
				15568, 16747, 16934, 17221, 17573, 15065, 16726, 16670, 16696, 16477, 16750, 16991, 17132, 17567, 17003,
				17686, 17753, 17773, 17381, 17059, 17110, 16395, 15803, 15044, 14413, 13075, 12975, 6748, 7845, 10781,
				8605, 6202, 3049, 1697, 1184, 1142, 1015, 568, 1093, 414, 121, 110, 17, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
				0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3, 6, 146, 297, 489, 1111, 1953, 3825, 2346,
				3356, 3407, 3482, 4238, 7179, 11642, 5486, 4265, 5488, 5559, 6589, 7608, 9285, 7668, 6077, 3918, 4498,
				7221, 9628, 11962, 9483, 11746, 10401, 8875, 8825, 13945, 16488, 13038, 17702, 16772, 7319, 228, 477,
				501, 547, 589, 1067, 13304, 17367, 14825, 13654, 12545, 8371, 10468, 9810, 8537, 6228, 3758, 4131, 3572,
				1698, 1017, 569, 188, 14, 2, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

		DummyTimedata timedata = new DummyTimedata("timedata0");
		ZonedDateTime start = ZonedDateTime.of(2019, 12, 30, 0, 0, 0, 0, ZoneId.of("UTC"));
		for (int i = 0; i < values.length; i++) {
			timedata.add(start.plusMinutes(i * 15), ChannelAddress.fromString("meter1/ActivePower"), values[i]);
		}
		this.timedata = timedata;
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		ChannelAddress[] channelAddresses = new ChannelAddress[config.channelAddresses().length];
		for (int i = 0; i < config.channelAddresses().length; i++) {
			channelAddresses[i] = ChannelAddress.fromString(config.channelAddresses()[i]);
		}
		this.channelAddresses = channelAddresses;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public ChannelAddress[] getChannelAddresses() {
		return this.channelAddresses;
	}

	@Override
	public Prediction24Hours get24HoursPrediction(ChannelAddress channelAddress) {
		ZonedDateTime now = roundZonedDateTimeDownTo15Minutes(ZonedDateTime.now(this.componentManager.getClock()));
		PredictionContainer container = this.predictions.get(channelAddress);
		if (container == null) {
			container = new PredictionContainer();
			this.predictions.put(channelAddress, container);
		}
		if (container.latestPredictionTimestamp == null || now.isAfter(container.latestPredictionTimestamp)) {
			// Create new prediction
			Prediction24Hours prediction = this.createNewPrediction(channelAddress);
			container.latestPrediction = prediction;
			container.latestPredictionTimestamp = now;
		} else {
			// Reuse existing prediction
		}
		return container.latestPrediction;
	}

	private Prediction24Hours createNewPrediction(ChannelAddress channelAddress) {
		ZonedDateTime now = ZonedDateTime.now(this.componentManager.getClock());
		ZonedDateTime fromDate = now.minus(NUMBER_OF_INPUT_SEASONS, ChronoUnit.DAYS);

		// Query database
		final SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryResult;
		try {
			queryResult = this.timedata.queryHistoricData(null, fromDate, now, Sets.newHashSet(channelAddress),
					900 /* seconds per 15 minutes */);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, e.getMessage());
			e.printStackTrace();
			return Prediction24Hours.EMPTY;
		}

		// Extract input data for prediction
		long[] input = queryResult.values().stream() //
				.map(m -> m.values()) //
				// extract JsonElement values as flat stream
				.flatMap(Collection::stream) //
				// remove 'null's
				.filter(v -> !v.isJsonNull()) //
				// convert JsonElement to long
				.mapToLong(v -> v.getAsLong()) //
				// replace 0 with 1 - HoltWinters does not work with 0 inputs
				.map(v -> v == 0L ? 1L : v) //
				// convert to Array
				.toArray();

		Integer[] result = new Integer[Prediction24Hours.NUMBER_OF_VALUES];

		// Calculate prediction
		if (input.length < Prediction24Hours.NUMBER_OF_VALUES * 2) {
			this.logWarn(this.log,
					"Only [" + input.length + "] values available. Not sufficient for a prediction - at least ["
							+ Prediction24Hours.NUMBER_OF_VALUES * 2 + "] are required.");

		} else {
			double[] prediction = HoltWinters.forecast(input, ALPHA, BETA, GAMMA, ENTRIES_PER_PERIOD,
					Prediction24Hours.NUMBER_OF_VALUES, false);

			// Create result
			for (int i = 0; i < Prediction24Hours.NUMBER_OF_VALUES; i++) {
				result[i] = (int) Math.round(prediction[prediction.length - Prediction24Hours.NUMBER_OF_VALUES + i]);
			}
		}
		return new Prediction24Hours(result);
	}

	/**
	 * Rounds a {@link ZonedDateTime} down to 15 minutes.
	 * 
	 * @param d the {@link ZonedDateTime}
	 * @return the rounded result
	 */
	private static ZonedDateTime roundZonedDateTimeDownTo15Minutes(ZonedDateTime d) {
		int minuteOfDay = d.get(ChronoField.MINUTE_OF_DAY);
		return d.with(ChronoField.NANO_OF_DAY, 0).plus(minuteOfDay / 15 * minuteOfDay, ChronoUnit.MINUTES);
	}
}
