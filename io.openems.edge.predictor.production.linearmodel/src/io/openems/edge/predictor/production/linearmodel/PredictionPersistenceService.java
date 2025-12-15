package io.openems.edge.predictor.production.linearmodel;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.timedata.api.Timedata;

public class PredictionPersistenceService {

	private static final long NO_INITIAL_DELAY = 0L;
	private static final long PERIOD_15_MINUTES = 15L;

	private final Logger log = LoggerFactory.getLogger(PredictionPersistenceService.class);

	private final Timedata timedata;
	private final Supplier<Clock> clockSupplier;
	private final List<ChannelMapping> channelMappings;

	private ScheduledExecutorService scheduler;

	public PredictionPersistenceService(//
			PredictorProductionLinearModel parent, //
			Timedata timedata, //
			Supplier<Clock> clockSupplier) {
		this(timedata, clockSupplier, defaultChannelMappings(parent));
	}

	public PredictionPersistenceService(//
			Timedata timedata, //
			Supplier<Clock> clockSupplier, //
			List<ChannelMapping> channelMappings) {
		this.timedata = timedata;
		this.clockSupplier = clockSupplier;
		this.channelMappings = List.copyOf(channelMappings);
	}

	/**
	 * Starts the scheduled job that shifts predicted values to their realized
	 * channels.
	 */
	public void startShiftingJob() {
		if (this.scheduler != null) {
			this.deactivateShiftingJob();
		}

		this.scheduler = Executors.newSingleThreadScheduledExecutor();
		this.scheduler.scheduleAtFixedRate(//
				this::shiftPredictionToRealizedChannel, //
				NO_INITIAL_DELAY, //
				PERIOD_15_MINUTES, //
				TimeUnit.MINUTES);
	}

	/**
	 * Stops the scheduled shifting job if it is currently running.
	 */
	public void deactivateShiftingJob() {
		if (this.scheduler != null) {
			ThreadPoolUtils.shutdownAndAwaitTermination(this.scheduler, 5);
			this.scheduler = null;
		}
	}

	/**
	 * Updates the "prediction ahead" channels with values from a given
	 * {@link Prediction}.
	 *
	 * @param prediction the {@link Prediction} object containing predicted values
	 *                   for different time horizons
	 */
	public void updatePredictionAheadChannels(Prediction prediction) {
		var now = roundDownToQuarter(ZonedDateTime.now(this.clockSupplier.get()));
		for (var channelMapping : this.channelMappings) {
			var value = prediction.getAt(now.plusHours(channelMapping.hoursAhead()));
			channelMapping.predictionAheadSetter.accept(value);
		}
	}

	@VisibleForTesting
	void shiftPredictionToRealizedChannel() {
		var now = roundDownToQuarter(ZonedDateTime.now(this.clockSupplier.get()));

		for (var channelMapping : this.channelMappings) {
			try {
				channelMapping.predictionRealizedSetter()//
						.accept(this.queryHistoricValue(//
								now.minusHours(channelMapping.hoursAhead()), //
								channelMapping.channelAheadAddress())//
								.orElse(null));
			} catch (OpenemsNamedException e) {
				this.log.error("Failed to shift prediction for channel {} ({}h ahead)", //
						channelMapping.channelAheadAddress(), //
						channelMapping.hoursAhead(), //
						e);
			}
		}
	}

	private Optional<Integer> queryHistoricValue(ZonedDateTime timestamp, ChannelAddress channelAddress)
			throws OpenemsNamedException {
		var data = this.timedata.queryHistoricData(//
				null, //
				timestamp, //
				timestamp.plusMinutes(15), //
				Sets.newHashSet(channelAddress), //
				new Resolution(15, ChronoUnit.MINUTES));

		return Optional.ofNullable(data)//
				.map(d -> d.get(timestamp))//
				.map(v -> v.get(channelAddress))//
				.filter(e -> e != null && !e.isJsonNull())//
				.map(JsonElement::getAsInt);
	}

	public record ChannelMapping(//
			int hoursAhead, //
			ChannelAddress channelAheadAddress, //
			Consumer<Integer> predictionAheadSetter, //
			Consumer<Integer> predictionRealizedSetter) {
	}

	/**
	 * Creates the default list of {@link ChannelMapping} instances for a given
	 * {@link PredictorProductionLinearModel}.
	 *
	 * <p>
	 * By default, mappings are provided for 1h, 6h, 12h, 24h, and 36h ahead
	 * predictions.
	 *
	 * @param parent the parent {@link PredictorProductionLinearModel} providing the
	 *               prediction channels and setter methods
	 * @return a list of default {@link ChannelMapping} objects
	 */
	public static List<ChannelMapping> defaultChannelMappings(PredictorProductionLinearModel parent) {
		return List.of(//
				new ChannelMapping(//
						1, //
						parent.getPrediction1hAheadChannel().address(), //
						parent::_setPrediction1hAhead, //
						parent::_setPrediction1hRealized), //
				new ChannelMapping(//
						6, //
						parent.getPrediction6hAheadChannel().address(), //
						parent::_setPrediction6hAhead, //
						parent::_setPrediction6hRealized), //
				new ChannelMapping(//
						12, //
						parent.getPrediction12hAheadChannel().address(), //
						parent::_setPrediction12hAhead, //
						parent::_setPrediction12hRealized), //
				new ChannelMapping(//
						24, //
						parent.getPrediction24hAheadChannel().address(), //
						parent::_setPrediction24hAhead, //
						parent::_setPrediction24hRealized), //
				new ChannelMapping(//
						36, //
						parent.getPrediction36hAheadChannel().address(), //
						parent::_setPrediction36hAhead, //
						parent::_setPrediction36hRealized));
	}
}
