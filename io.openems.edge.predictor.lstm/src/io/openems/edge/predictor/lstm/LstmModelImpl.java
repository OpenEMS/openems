package io.openems.edge.predictor.lstm;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.predictor.api.oneday.AbstractPredictor24Hours;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;
import io.openems.edge.predictor.api.oneday.Predictor24Hours;
import io.openems.edge.predictor.lstm.common.DataModification;
import io.openems.edge.predictor.lstm.common.ReadModels;
import io.openems.edge.predictor.lstm.interpolation.InterpolationManager;
import io.openems.edge.predictor.lstm.train.LstmTrain;
import io.openems.edge.predictor.lstm.train.LstmTrainCallBack;
import io.openems.edge.predictor.lstm.utilities.UtilityConversion;
import io.openems.edge.timedata.api.Timedata;
//CHECKSTYLE:OFF
public class LstmModelImpl extends AbstractPredictor24Hours implements Predictor24Hours, OpenemsComponent {

	public static final Function<List<Integer>, List<Double>> INTEGER_TO_DOUBLE_LIST = UtilityConversion::convertListIntegerToListDouble;
	public static final Function<ArrayList<Double>, Integer[]> Array_List_Double_To_Integer_Array = UtilityConversion::convertDoubleToIntegerArray;

	// private final Logger log = LoggerFactory.getLogger(LstmModelImpl.class);

	private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	@Reference
	private Timedata timedata;

	@Reference
	private ComponentManager componentManager;

	protected LstmModelImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				LstmModel.ChannelId.values());

		LstmTrainCallBack callback = new LstmTrain();
		scheduler.scheduleAtFixedRate(() -> callback.onCallback(), 0, 15L * 24 * 60 * 60 * 1000, TimeUnit.MILLISECONDS);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.channelAddresses());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		scheduler.shutdown();
		super.deactivate();
	}

	@Override
	protected Prediction24Hours createNewPrediction(ChannelAddress channelAddress) {

		/*
		 * ZonedDateTime nowDate = ZonedDateTime.of( 2023, 6, 11, 2, 0, 0, 0,
		 * ZonedDateTime.now().getZone());
		 */

		ZonedDateTime nowDate = ZonedDateTime.now();

		ZonedDateTime until = ZonedDateTime.of(//
				nowDate.getYear(), //
				nowDate.getMonthValue(), //
				nowDate.getDayOfMonth(), //
				nowDate.getHour(), //
				getMinute(nowDate), //
				0, //
				0, //
				nowDate.getZone());

		ZonedDateTime temp = until.minusDays(6);
		ZonedDateTime fromDate = ZonedDateTime.of(//
				temp.getYear(), //
				temp.getMonthValue(), //
				temp.getDayOfMonth(), //
				nowDate.getHour(), //
				getMinute(nowDate), //
				0, //
				0, //
				temp.getZone());

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryResult = new TreeMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>();

		try {
			queryResult = this.timedata.queryHistoricData(null, fromDate, until, Sets.newHashSet(channelAddress),
					new Resolution(15, ChronoUnit.MINUTES));
		} catch (OpenemsNamedException e) {

			e.printStackTrace();
		}

		ArrayList<Double> target = new ArrayList<Double>();
		//ArrayList<Double> sameDayLastWeek = new ArrayList<Double>();
		ZonedDateTime targetFrom = until.plusMinutes(15);

		ZonedDateTime targetTo = targetFrom.plusHours(24);

		ArrayList<Double> allTarget = new ArrayList<Double>();
		try {
			target = this.getData(this.timedata.queryHistoricData(null, targetFrom, targetTo,
					Sets.newHashSet(channelAddress), new Resolution(15, ChronoUnit.MINUTES)));
			allTarget.addAll(target);
		} catch (OpenemsNamedException e) {
			e.printStackTrace();

		}

		ArrayList<Double> forTrendPrediction = new ArrayList<Double>();
		try {
			forTrendPrediction = this.getData(this.timedata.queryHistoricData(null, until.minusMinutes(105), until,
					Sets.newHashSet(channelAddress), new Resolution(15, ChronoUnit.MINUTES)));

		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}

		ArrayList<Double> data = this.getData(queryResult);
		ArrayList<OffsetDateTime> date = this.getDate(queryResult);

		Prediction prediction = new Prediction(data, date, Collections.min(forTrendPrediction),
				Collections.max(forTrendPrediction));
		double onePointPrediction = this.predictTrend(forTrendPrediction, until, Collections.min(data),
				Collections.max(data));

		ArrayList<Double> predicted = new ArrayList<Double>();
		predicted = getArranged(getIndex(targetFrom.getHour(), targetFrom.getMinute()),
				prediction.predictedAndScaledBack);
		ArrayList<Double> allPredicted = new ArrayList<Double>();
		allPredicted.addAll(predicted);

		predicted.set(0, onePointPrediction);
		return new Prediction24Hours(Array_List_Double_To_Integer_Array.apply(predicted));

	}

	@Override
	protected ClockProvider getClockProvider() {
		return this.componentManager;
	}

	/**
	 * get minute.
	 * 
	 * @param fromDate ZonedDateTime fromDate
	 * @return minute
	 */
	public static int getMinute(ZonedDateTime fromDate) {
		int nowMinute = fromDate.getMinute();
		return (nowMinute / 15) * 15;
	}

	public ArrayList<Double> getData(SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> querryResult) {

		ArrayList<Double> data = (ArrayList<Double>) querryResult.values().stream() //
				.map(SortedMap::values) //
				.flatMap(Collection::stream) //
				.map(v -> {
					if (v.isJsonNull()) {
						return null;
					}
					return v.getAsDouble();
				}).collect(Collectors.toList());

		if (isAllNulls(data)) {
			System.out.println("Data is all null, use different predictor");
		}
		return data;
	}

	public ArrayList<OffsetDateTime> getDate(
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> querryResult) {

		ArrayList<OffsetDateTime> date = new ArrayList<OffsetDateTime>();

		querryResult.keySet()//
				.stream()//
				.forEach(zonedDateTime -> {
					date.add(zonedDateTime.toOffsetDateTime());
				});
		return date;
	}

	private boolean isAllNulls(ArrayList<Double> array) {
		return StreamSupport //
				.stream(array.spliterator(), true) //
				.allMatch(o -> o == null);
	}

	public double predictTrend(ArrayList<Double> data, ZonedDateTime until, double min, double max) {
		// read Model
		// String pathTrend =
		// "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\Trend.txt";

		String pathTrend = this.getClass().getResource("Trend.txt").getFile();
		double pred = 0;
		// ArrayList<Double>prediction = new ArrayList<Double>();
		ReadModels models = new ReadModels(pathTrend);
		ArrayList<ArrayList<Double>> val = models.dataList.get(0);
		InterpolationManager interpolationManager = new InterpolationManager(data);
		data = interpolationManager.interpolated;
		ArrayList<Double> scaledData = DataModification.scale(data, min, max);
		// readData

		pred = Predictor.predict(scaledData, val.get(0), val.get(1), val.get(2), val.get(3), val.get(4), val.get(5),
				val.get(7), val.get(6));

		pred = DataModification.scaleBack(pred, min, max);
		// prediction.add(pred);

		return pred;

	}

	public static ArrayList<Double> getArranged(int splitIndex, ArrayList<Double> singleArray) {
		ArrayList<Double> arranged = new ArrayList<Double>();
		ArrayList<Double> firstGroup = new ArrayList<Double>();
		ArrayList<Double> secondGroup = new ArrayList<Double>();

		for (int i = 0; i < singleArray.size(); i++) {
			if (i < splitIndex) {
				firstGroup.add(singleArray.get(i));
			} else {
				secondGroup.add(singleArray.get(i));
			}
		}

		arranged.addAll(secondGroup);
		arranged.addAll(firstGroup);

		return arranged;
	}

	public static Integer getIndex(Integer hour, Integer minute) {

		int k = 0;
		for (int i = 0; i < 24; i++) {
			for (int j = 0; j < 4; j++) {
				int h = i;
				int m = j * 15;
				if (hour == h && minute == m) {
					System.out.println(k);

					return k;
				} else {
					k = k + 1;
				}
			}
		}

		return k;

	}
	//CHECKSTYLE:ON
}
