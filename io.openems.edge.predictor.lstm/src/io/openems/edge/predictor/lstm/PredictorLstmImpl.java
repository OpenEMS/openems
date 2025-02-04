package io.openems.edge.predictor.lstm;

import static io.openems.common.utils.ThreadPoolUtils.shutdownAndAwaitTermination;
import static io.openems.edge.common.jsonapi.EdgeGuards.roleIsAtleast;
import static io.openems.edge.predictor.lstm.jsonrpc.PredictionRequestHandler.handlerGetPredictionRequest;
import static io.openems.edge.predictor.lstm.preprocessing.DataModification.removeNegatives;
import static io.openems.edge.predictor.lstm.utilities.DataUtility.combine;
import static io.openems.edge.predictor.lstm.utilities.DataUtility.concatenateList;
import static io.openems.edge.predictor.lstm.utilities.DataUtility.getData;
import static io.openems.edge.predictor.lstm.utilities.DataUtility.getDate;
import static io.openems.edge.predictor.lstm.utilities.DataUtility.getMinute;
import static java.lang.Math.min;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Role;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.predictor.api.prediction.AbstractPredictor;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.prediction.Predictor;
import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.common.LstmPredictor;
import io.openems.edge.predictor.lstm.common.ReadAndSaveModels;
import io.openems.edge.predictor.lstm.jsonrpc.GetPredictionRequest;
import io.openems.edge.predictor.lstm.train.LstmTrain;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Predictor.LSTM", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class PredictorLstmImpl extends AbstractPredictor
		implements Predictor, OpenemsComponent, ComponentJsonApi, PredictorLstm {

	/** 45 days. */
	private static final long DAYS_45 = 45;

	/** 45 days in minutes. */
	private static final long PERIOD = DAYS_45 * 24 * 60;

	@Reference
	private Sum sum;

	@Reference
	private Timedata timedata;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private PredictorManager predictorManager;

	@Override
	protected ClockProvider getClockProvider() {
		return this.componentManager;
	}

	public PredictorLstmImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				PredictorLstm.ChannelId.values()//
		);
	}

	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ChannelAddress channelForPrediction;

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled(), //
				new String[] { config.channelAddress() }, config.logVerbosity());

		var channelAddress = ChannelAddress.fromString(config.channelAddress());
		this.channelForPrediction = channelAddress;

		/*
		 * Avoid training for the newly setup Edges due to lack of data. Set a fixed
		 * 45-day period: 30 days for training and 15 days for validation.
		 */
		this.scheduler.scheduleAtFixedRate(//
				new LstmTrain(this.timedata, channelAddress, this, DAYS_45), //
				0, //
				PERIOD, //
				TimeUnit.MINUTES//
		);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		shutdownAndAwaitTermination(this.scheduler, 0);
		super.deactivate();
	}

	@Override
	protected Prediction createNewPrediction(ChannelAddress channelAddress) {
		var hyperParameters = ReadAndSaveModels.read(channelAddress.getChannelId());
		var now = ZonedDateTime.now();

		var seasonalityFuture = CompletableFuture
				.supplyAsync(() -> this.predictSeasonality(channelAddress, now, hyperParameters));

		var trendFuture = CompletableFuture.supplyAsync(() -> this.predictTrend(channelAddress, now, hyperParameters));

		var dayPlus1SeasonalityFuture = CompletableFuture
				.supplyAsync(() -> this.predictSeasonality(channelAddress, now.plusDays(1), hyperParameters));

		// var combinePrerequisites = CompletableFuture.allOf(seasonalityFuture,
		// trendFuture);

		try {
			// TODO combinePrerequisites.get();

			// Current day prediction
			var currentDayPredicted = combine(trendFuture.get(), seasonalityFuture.get());

			// Next Day prediction
			var plus1DaySeasonalityPrediction = dayPlus1SeasonalityFuture.get();

			// Concat current and Nextday
			var actualPredicted = concatenateList(currentDayPredicted, plus1DaySeasonalityPrediction);

			var baseTimeOfPrediction = now.withMinute(getMinute(now, hyperParameters)).withSecond(0).withNano(0);

			return Prediction.from(this.sum, channelAddress, //
					baseTimeOfPrediction, //
					averageInChunks(actualPredicted));

		} catch (Exception e) {
			throw new RuntimeException("Error in getting prediction execution", e);
		}
	}

	/**
	 * Averages the elements of an integer array in chunks of a specified size.
	 *
	 * <p>
	 * This method takes an input array of integers and divides it into chunks of a
	 * fixed size. For each chunk, it calculates the average of the integers and
	 * stores the result in a new array. The size of the result array is determined
	 * by the total number of elements in the input array divided by the chunk size.
	 * </p>
	 *
	 * @param inputList an arrayList of Doubles to be processed. The array length
	 *                  must be a multiple of the chunk size for correct processing.
	 * @return an array of integers containing the averages of each chunk.
	 * 
	 */
	private static Integer[] averageInChunks(ArrayList<Double> inputList) {
		final int chunkSize = 3;
		var resultSize = inputList.size() / chunkSize;
		var result = new Integer[resultSize];

		for (int i = 0; i < inputList.size(); i += chunkSize) {
			var sum = IntStream.range(i, min(i + chunkSize, inputList.size())) //
					.mapToDouble(j -> inputList.get(j))//
					.sum();
			result[i / chunkSize] = (int) (sum / chunkSize);
		}
		return result;
	}

	/**
	 * Queries historic data for a specified time range and channel address with
	 * given {@link ChannelAddress}.
	 *
	 * @param from            the start of the time range
	 * @param until           the end of the time range
	 * @param channelAddress  the {@link ChannelAddress} for the query
	 * @param hyperParameters the {@link HyperParameters} that include the interval
	 *                        for data resolution
	 * @return a SortedMap where the key is a ZonedDateTime representing the
	 *         timestamp of the data point, and the value is another SortedMap where
	 *         the key is the ChannelAddress and the value is the data point as a
	 *         JsonElement. and null if error
	 */
	private SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(ZonedDateTime from,
			ZonedDateTime until, ChannelAddress channelAddress, HyperParameters hyperParameters) {
		try {
			return this.timedata.queryHistoricData(null, from, until, Sets.newHashSet(channelAddress),
					new Resolution(hyperParameters.getInterval(), ChronoUnit.MINUTES));
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Predicts trend values for a specified channel at the current date using LSTM
	 * models.
	 *
	 * @param channelAddress  The {@link ChannelAddress} for which trend values are
	 *                        predicted.
	 * @param nowDate         The current date and time for which trend values are
	 *                        predicted.
	 * @param hyperParameters The {@link HyperParameters} for the prediction model.
	 * @return A list of predicted trend values for the specified channel at the
	 *         current date.
	 * @throws SomeException If there's any specific exception that might be thrown
	 *                       during the process.
	 */
	public ArrayList<Double> predictTrend(ChannelAddress channelAddress, ZonedDateTime nowDate,
			HyperParameters hyperParameters) {
		var till = nowDate.withMinute(getMinute(nowDate, hyperParameters)).withSecond(0).withNano(0);
		var from = till.minusMinutes(hyperParameters.getInterval() * hyperParameters.getWindowSizeTrend());

		var trendQueryResult = this.queryHistoricData(//
				from, //
				till, //
				channelAddress, //
				hyperParameters);

		return LstmPredictor.predictTrend(//
				getData(trendQueryResult), //
				getDate(trendQueryResult), //
				till, //
				hyperParameters);
	}

	/**
	 * Predicts Seasonality values for a specified channel at the current date using
	 * LSTM models.
	 *
	 * @param channelAddress  The address of the channel for which seasonality
	 *                        values are predicted.
	 * @param nowDate         The current date and time for which seasonality values
	 *                        are predicted.
	 * @param hyperParameters The {@link ChannelAddress} for the prediction model.
	 * @return A list of predicted seasonality values for the specified channel at
	 *         the current date.
	 * @throws SomeException If there's any specific exception that might be thrown
	 *                       during the process.
	 */
	public ArrayList<Double> predictSeasonality(ChannelAddress channelAddress, ZonedDateTime nowDate,
			HyperParameters hyperParameters) {
		var till = nowDate.withMinute(getMinute(nowDate, hyperParameters)).withSecond(0).withNano(0);
		var temp = till.minusDays(hyperParameters.getWindowSizeSeasonality() - 1);

		var from = temp//
				.withMinute(getMinute(nowDate, hyperParameters))//
				.withSecond(0)//
				.withNano(0);

		var targetFrom = till.plusMinutes(hyperParameters.getInterval());
		var queryResult = this.queryHistoricData(from, till, channelAddress, hyperParameters);

		return LstmPredictor.getArranged(
				LstmPredictor.getIndex(targetFrom.getHour(), targetFrom.getMinute(), hyperParameters), //
				LstmPredictor.predictSeasonality(removeNegatives(getData(queryResult)), getDate(queryResult), //
						hyperParameters));
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(GetPredictionRequest.METHOD, endpoint -> {
			endpoint.setGuards(roleIsAtleast(Role.OWNER));
		}, call -> {
			return handlerGetPredictionRequest(call.getRequest().id, this.predictorManager, this.channelForPrediction);
		});
	}
}