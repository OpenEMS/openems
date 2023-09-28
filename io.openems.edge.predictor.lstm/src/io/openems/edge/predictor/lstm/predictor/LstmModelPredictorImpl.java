package io.openems.edge.predictor.lstm.predictor;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.predictor.api.oneday.AbstractPredictor24Hours;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;
import io.openems.edge.predictor.api.oneday.Predictor24Hours;
import io.openems.edge.predictor.lstm.utilities.UtilityConversion;
import io.openems.edge.predictor.lstm.common.DataModification;
import io.openems.edge.predictor.lstm.common.ReadModels;
import io.openems.edge.predictor.lstm.interpolation.InterpolationManager;
import io.openems.edge.predictor.lstm.performance.PerformanceMatrix;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Lstm.Model.predictor", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class LstmModelPredictorImpl extends AbstractPredictor24Hours
		implements Predictor24Hours, OpenemsComponent /* , org.osgi.service.event.EventHandler */ {

	// private final Logger log = LoggerFactory.getLogger(LstmPredictorImpl.class);

	public static final Function<List<Integer>, List<Double>> INTEGER_TO_DOUBLE_LIST = UtilityConversion::convertListIntegerToListDouble;
	public static final Function<ArrayList<Double>, Integer[]> Array_List_Double_To_Integer_Array = UtilityConversion::convertDoubleToIntegerArray;

	@Reference
	private Timedata timedata;

	protected Config config;

	@Reference
	private ComponentManager componentManager;

	public LstmModelPredictorImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				LstmModelPredictor.ChannelId.values() //

		);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		super.activate(context, this.config.id(), this.config.alias(), this.config.enabled(),
				this.config.channelAddresses());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ClockProvider getClockProvider() {
		return this.componentManager;
	}

	@Override
	protected Prediction24Hours createNewPrediction(ChannelAddress channelAddress) {
		ArrayList<Double> predicted = new ArrayList<Double>();
		ArrayList<Double> allPredicted = new ArrayList<Double>();
		ArrayList<Double> allTarget = new ArrayList<Double>();
		ArrayList<Double> forTrendPrediction = new ArrayList<Double>();

		ZonedDateTime nowDate = ZonedDateTime.of(2023, 6, 11, 2, 0, 0, 0, ZonedDateTime.now().getZone());
		// for (int i =0;i<1000;i++) {

		ZonedDateTime until = ZonedDateTime.of(nowDate.getYear(), nowDate.getMonthValue(), nowDate.getDayOfMonth(),
				nowDate.getHour(), getMinute(nowDate), 0, 0, nowDate.getZone());

		ZonedDateTime temp = until.minusDays(6);
		ZonedDateTime fromDate = ZonedDateTime.of(temp.getYear(), temp.getMonthValue(), temp.getDayOfMonth(),
				nowDate.getHour(), getMinute(nowDate), 0, 0, temp.getZone());

		System.out.println("From : " + fromDate);
		System.out.println("Till : " + until);

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> querryResult = new TreeMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>();

		try {
			querryResult = timedata.queryHistoricData(null, fromDate, until, Sets.newHashSet(channelAddress),
					new Resolution(15, ChronoUnit.MINUTES));
		} catch (OpenemsNamedException e) {

			e.printStackTrace();
		}

		ArrayList<Double> data = this.getData(querryResult);
		ArrayList<OffsetDateTime> date = this.getDate(querryResult);
		ArrayList<Double> target = new ArrayList<Double>();
		ArrayList<Double> sameDayLastWeek = new ArrayList<Double>();
		ZonedDateTime targetFrom = until.plusMinutes(15);

		ZonedDateTime targetTo = targetFrom.plusHours(24);

		try {
			target = getData(timedata.queryHistoricData(null, targetFrom, targetTo, Sets.newHashSet(channelAddress),
					new Resolution(15, ChronoUnit.MINUTES)));
			allTarget.addAll(target);
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
		try {
			sameDayLastWeek = getData(timedata.queryHistoricData(null, fromDate.plusMinutes(15),
					ZonedDateTime.of(fromDate.getYear(), fromDate.getMonthValue(), fromDate.getDayOfMonth() + 1, 0, 0,
							0, 0, nowDate.getZone()),
					Sets.newHashSet(channelAddress), new Resolution(15, ChronoUnit.MINUTES)));
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
		try {
			forTrendPrediction = getData(timedata.queryHistoricData(null, until.minusMinutes(105), until,
					Sets.newHashSet(channelAddress), new Resolution(15, ChronoUnit.MINUTES)));

		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Prediction prediction = new Prediction(data, date,Collections.min(forTrendPrediction),Collections.max(forTrendPrediction));
		double onePointPrediction = predictTrend(forTrendPrediction, until, Collections.min(data),
				Collections.max(data));
		predicted = getArranged(getIndex(targetFrom.getHour(), targetFrom.getMinute()),
				prediction.predictedAndScaledBack);
		allPredicted.addAll(predicted);
		// System.out.println(prediction.predicted);
		System.out.println("");
		System.out.println("Target From = " + targetFrom);
		System.out.println("Target to= " + targetTo);
		System.out.println("Target = " + target);
		System.out.println("");
		System.out.println("Last week same day: " + sameDayLastWeek);
		System.out.println("");
		System.out.println("Predicted = " + prediction.predictedAndScaledBack);
		System.out.println("");
		System.out.println("forTrendfrom =" + until.minusMinutes(105));
		System.out.println("forTrendto =" + until);
		System.out.println("forTrendPrediction =" + forTrendPrediction);
		System.out.println("forTrendPredictionr result  =" + onePointPrediction);
		System.out.println("");
		System.out.println("Predicted Arranged= " + predicted);

		System.out.println("Predicted size= " + prediction.predictedAndScaledBack.size());
		System.out.println("Target size = " + target.size());
		System.out.println("");

		// nowDate = nowDate.plusMinutes(15);

		// }

		// Integer[] x =
		// Array_List_Double_To_Integer_Array.apply(prediction.predictedAndScaledBack);
		System.out.println("all Predicted size= " + allPredicted.size());
		System.out.println("all Target size= " + allTarget.size());

		PerformanceMatrix pm = new PerformanceMatrix(allTarget, allPredicted, 0.2);
		pm.statusReport();

		return new Prediction24Hours(Array_List_Double_To_Integer_Array.apply(predicted));
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

	public static Integer getMinute(ZonedDateTime fromDate) {

		int nowMinute = fromDate.getMinute();
		if (nowMinute >= 0 && nowMinute < 15) {

			return 0;
		} else if (nowMinute >= 15 && nowMinute < 30) {
			return 15;
		} else if (nowMinute >= 30 && nowMinute < 45) {
			return 30;
		}
		return 45;
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

	public double predictTrend(ArrayList<Double> data, ZonedDateTime until, double min, double max) {
		// read Model
		String pathTrend = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\Trend.txt";
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

}