package io.openems.edge.predictor.lstmmodel;

import static io.openems.edge.predictor.lstmmodel.utilities.DataUtility.combine;
import static io.openems.edge.predictor.lstmmodel.utilities.DataUtility.getData;
import static io.openems.edge.predictor.lstmmodel.utilities.DataUtility.getDate;
import static io.openems.edge.predictor.lstmmodel.utilities.DataUtility.getMinute;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.osgi.service.metatype.annotations.Designate;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Role;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.EdgeGuards;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.predictor.api.prediction.AbstractPredictor;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.prediction.Prediction.Interval;
import io.openems.edge.predictor.api.prediction.Predictor;
import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.common.ReadAndSaveModels;
import io.openems.edge.predictor.lstmmodel.jsonrpc.GetPredictionRequest;
import io.openems.edge.predictor.lstmmodel.jsonrpc.PredictionRequestHandler;
import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;
import io.openems.edge.predictor.lstmmodel.train.LstmTrain;
import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Predictor.LstmModel", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class LstmModelImpl extends AbstractPredictor
		implements Predictor, OpenemsComponent, ComponentJsonApi, LstmModel {

	private static final Function<ArrayList<Double>, Integer[]> DOUBLELIST_TO_INTARRAY = UtilityConversion::toInteger1DArray;
	private static final long PERIOD = 45 * 24 * 60; /* 45 days in minutes */

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

	public LstmModelImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				LstmModel.ChannelId.values()//
		);
	}

	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ChannelAddress channelForPrediction;

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled(), //
				config.channelAddresses(), config.logVerbosity());

		this.channelForPrediction = ChannelAddress.fromString(config.channelAddresses());

		/*
		 * Avoid training for the new FEMs due to lack of data. Set a fixed 45-day
		 * period: 30 days for training and 15 days for validation.
		 */
		this.scheduler.scheduleAtFixedRate(//
				new LstmTrain(this.timedata, //
						config.channelAddresses(), this),
				0, PERIOD, TimeUnit.MINUTES);

	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.scheduler.shutdown();
		ThreadPoolUtils.shutdownAndAwaitTermination(this.scheduler, 0);
		super.deactivate();
	}

	@Override
	protected Prediction createNewPrediction(ChannelAddress channelAddress) {

		var hyperParameters = ReadAndSaveModels.read(channelAddress.toString().split("/")[1]);

		var nowDate = ZonedDateTime.now();

		var seasonalityPrediction = this.predictSeasonality(channelAddress, nowDate, hyperParameters);
		var trendPrediction = this.predictTrend(channelAddress, nowDate, hyperParameters);
		var predicted = combine(trendPrediction, seasonalityPrediction);
		var till = nowDate.withMinute(getMinute(nowDate, hyperParameters)).withSecond(0).withNano(0);

		return Prediction.from(//
				Prediction.getValueRange(this.sum, channelAddress), //
				Interval.DUODCIMUS, //
				till, //
				DOUBLELIST_TO_INTARRAY.apply(predicted));
	}

	/**
	 * Queries historic data for a specified time range and channel address with
	 * given hyperparameters.
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
	private SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(//
			ZonedDateTime from, //
			ZonedDateTime until, //
			ChannelAddress channelAddress, //
			HyperParameters hyperParameters) {
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
	public ArrayList<Double> predictTrend(//
			ChannelAddress channelAddress, //
			ZonedDateTime nowDate, //
			HyperParameters hyperParameters) {

		var till = nowDate//
				.withMinute(getMinute(nowDate, hyperParameters))//
				.withSecond(0)//
				.withNano(0);

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
	 * Predicts seasonality values for a specified channel at the current date using
	 * LSTM models.
	 *
	 * @param channelAddress  The address of the channel for which seasonality
	 *                        values are predicted.
	 * @param nowDate         The current date and time for which seasonality values
	 *                        are predicted.
	 * @param hyperParameters The hyperparameters for the prediction model.
	 * @return A list of predicted seasonality values for the specified channel at
	 *         the current date.
	 * @throws SomeException If there's any specific exception that might be thrown
	 *                       during the process.
	 */
	public ArrayList<Double> predictSeasonality(ChannelAddress channelAddress, ZonedDateTime nowDate,
			HyperParameters hyperParameters) {

		var till = nowDate//
				.withMinute(getMinute(nowDate, hyperParameters))//
				.withSecond(0)//
				.withNano(0);

		var temp = till.minusDays(hyperParameters.getWindowSizeSeasonality() - 1);

		var from = temp//
				.withMinute(getMinute(nowDate, hyperParameters))//
				.withSecond(0)//
				.withNano(0);

		var targetFrom = till.plusMinutes(hyperParameters.getInterval());

		var queryResult = this.queryHistoricData(from, till, channelAddress, hyperParameters);

		var predicted = LstmPredictor.getArranged(
				LstmPredictor.getIndex(targetFrom.getHour(), targetFrom.getMinute(), hyperParameters), //
				LstmPredictor.predictSeasonality(DataModification.removeNegatives(getData(queryResult)),
						getDate(queryResult), //
						hyperParameters));

		return predicted;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(GetPredictionRequest.METHOD, endpoint -> {
			endpoint.setGuards(EdgeGuards.roleIsAtleast(Role.OWNER));
		}, call -> {
			return PredictionRequestHandler.handlerGetPredictionRequest(call.getRequest().id, this.predictorManager,
					this.channelForPrediction);
		});
	}

}