package io.openems.edge.predictor.lstm.predictor;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
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
import io.openems.edge.predictor.lstm.common.DataModification;
import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.common.ReadModels;
import io.openems.edge.predictor.lstm.interpolation.InterpolationManager;
import io.openems.edge.predictor.lstm.performance.PerformanceMatrix;
import io.openems.edge.predictor.lstm.utilities.UtilityConversion;
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
		final ArrayList<Double> allPredicted = new ArrayList<Double>();
		ArrayList<Double> allTarget = new ArrayList<Double>();
		ArrayList<Double> forTrendPrediction = new ArrayList<Double>();
		final ArrayList<ArrayList<Double>> allPredictionFro20Days = new ArrayList<ArrayList<Double>>();
		final ArrayList<ArrayList<Double>> allTargetFro20Days = new ArrayList<ArrayList<Double>>();
		HyperParameters hyperParameters = new HyperParameters();
		int windowSizeSeasonality = hyperParameters.getWindowSizeSeasonality();

		ZonedDateTime nowDate = ZonedDateTime.of(2023, 7, 14, 18, 0, 0, 0, ZonedDateTime.now().getZone());

		ZonedDateTime until = ZonedDateTime.of(nowDate.getYear(), nowDate.getMonthValue(), nowDate.getDayOfMonth(),
				nowDate.getHour(), getMinute(nowDate, hyperParameters), 0, 0, nowDate.getZone());

		ZonedDateTime temp = until.minusDays(windowSizeSeasonality - 1);
		ZonedDateTime fromDate = ZonedDateTime.of(temp.getYear(), temp.getMonthValue(), temp.getDayOfMonth(),
				nowDate.getHour(), getMinute(nowDate, hyperParameters), 0, 0, temp.getZone());

		System.out.println("From : " + fromDate);
		System.out.println("Till : " + until);

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> querryResult = new TreeMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>();

		try {
			querryResult = this.timedata.queryHistoricData(null, fromDate, until, Sets.newHashSet(channelAddress),
					new Resolution(hyperParameters.getInterval(), ChronoUnit.MINUTES));
		} catch (OpenemsNamedException e) {

			e.printStackTrace();
		}

		final ArrayList<Double> data = this.getData(querryResult);
		final ArrayList<OffsetDateTime> date = this.getDate(querryResult);
		ArrayList<Double> target = new ArrayList<Double>();
		ArrayList<OffsetDateTime> dateTrend = new ArrayList<OffsetDateTime>();
		// ArrayList<Double> sameDayLastWeek = new ArrayList<Double>();
		ZonedDateTime targetFrom = until.plusMinutes(15);

		ZonedDateTime targetTo = targetFrom.plusHours(24);

		try {
			target = this.getData(
					this.timedata.queryHistoricData(null, targetFrom, targetTo, Sets.newHashSet(channelAddress),
							new Resolution(hyperParameters.getInterval(), ChronoUnit.MINUTES)));
			allTarget.addAll(target);
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}

		try {
			forTrendPrediction = this.getData(this.timedata.queryHistoricData(null, until.minusMinutes(105), until,
					Sets.newHashSet(channelAddress), new Resolution(15, ChronoUnit.MINUTES)));
			dateTrend = this.getDate(this.timedata.queryHistoricData(null, until.minusMinutes(105), until,
					Sets.newHashSet(channelAddress), new Resolution(15, ChronoUnit.MINUTES)));

		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String path = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\SavedModel.txt";

		Prediction prediction = new Prediction(data, date, path, hyperParameters);
		final ArrayList<Double> trendPrediction = this.predictTrend(forTrendPrediction, dateTrend, until,
				hyperParameters);
		var predicted = getArranged(getIndex(targetFrom.getHour(), targetFrom.getMinute(), hyperParameters),
				prediction.getPredictedValues());

		// combining the prediction from trend and seasonlaity without any smoothing
		// method

		for (int l = 0; l < trendPrediction.size(); l++) {
			predicted.add(l, trendPrediction.get(l));

		}

		// allPredicted.addAll(predicted);
		// System.out.println(prediction.predicted);
		// System.out.println("data size" + data.size());
		// System.out.println("Target From = " + targetFrom);
		// System.out.println("Target to= " + targetTo);
		// System.out.println("Target = " + target);
		// System.out.println("");
		// System.out.println("Last week same day: " + sameDayLastWeek);
		// System.out.println("");
		// System.out.println("Predicted = " + prediction.getPredictedValues());
		// System.out.println("");
		// System.out.println("forTrendfrom =" + until.minusMinutes(windowSizeTrend *
		// 15));
		// System.out.println("forTrendto =" + until);
		// System.out.println("forTrendPrediction =" + forTrendPrediction);
		// System.out.println("forTrendPredictionr result =" + onePointPrediction);
		// System.out.println("");
		// System.out.println("Predicted Arranged= " + predicted);
		//
		// System.out.println("Predicted size= " +
		// prediction.getPredictedValues().size());
		// System.out.println("Target size = " + target.size());
		// System.out.println("");

		allTargetFro20Days.add(target);
		allPredictionFro20Days.add(predicted);

		// nowDate = nowDate.plusMinutes(15);

		// }

		// Integer[] x =
		// Array_List_Double_To_Integer_Array.apply(prediction.predictedAndScaledBack);
		System.out.println("all Predicted size= " + allPredicted.size());
		System.out.println("all Target size= " + allTarget.size());
		System.out.println("Target=" + allTargetFro20Days);
		System.out.println("Predicted =" + allPredictionFro20Days);

		PerformanceMatrix pm = new PerformanceMatrix(allTarget, allPredicted, 0.2);
		pm.statusReport();

		return new Prediction24Hours(Array_List_Double_To_Integer_Array.apply(predicted));
	}

	/**
	 * Extract and convert data values from a SortedMap of query results. This
	 * method extracts and converts data values from a SortedMap of query results,
	 * where each result contains a timestamped map of channel addresses and JSON
	 * elements. The method processes the JSON elements, converting them to Double
	 * values, and stores them in an ArrayList. If all data values are null, a
	 * warning message is printed.
	 *
	 * @param querryResult A SortedMap of ZonedDateTime and SortedMap of
	 *                     ChannelAddress and JsonElement representing query
	 *                     results.
	 * @return An ArrayList of Double values containing the extracted data.
	 */

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

		if (this.isAllNulls(data)) {
			System.out.println("Data is all null, use different predictor");
		}
		return data;
	}

	/**
	 * Extract and convert data values from a SortedMap of query results. This
	 * method extracts data values from a SortedMap of query results, which is a
	 * collection of timestamped data points. It converts the extracted values into
	 * an ArrayList of Double values.
	 *
	 * @param querryResult A SortedMap of ZonedDateTime and SortedMap of
	 *                     ChannelAddress and JsonElement representing query
	 *                     results.
	 * @return An ArrayList of Double values containing the extracted data.
	 */

	public ArrayList<OffsetDateTime> getDate(
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> querryResult) {

		ArrayList<OffsetDateTime> date = new ArrayList<OffsetDateTime>();

		querryResult.keySet()//
				.stream()// offs
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

	/**
	 * Get the nearest 15-minute interval for a given ZonedDateTime. This method
	 * calculates and returns the nearest 15-minute interval (0, 15, 30, or 45) for
	 * a given ZonedDateTime, based on the minute component of the timestamp.
	 *
	 * @param nowDate         A ZonedDateTime representing the timestamp for which
	 *                        the nearest 15-minute interval is to be determined.
	 * @param hyperParameters is the object of class HyperParameters.
	 * 
	 * @return An Integer representing the nearest 15-minute interval (0, 15, 30, or
	 *         45) based on the minute component of the timestamp.
	 */
	public static Integer getMinute(ZonedDateTime nowDate, HyperParameters hyperParameters) {

		int totalGroups = 60 / hyperParameters.getInterval();
		int startVal = 0;
		int endVal = 0;
		for (int i = 0; i < totalGroups; i++) {
			endVal = startVal + hyperParameters.getInterval();
			boolean check = startVal <= nowDate.getMinute() && nowDate.getMinute() < endVal;
			if (check == false) {

				startVal = endVal;

			} else {

				break;
			}
		}
		// if (endVal == 60) {
		// return 0;
		// }
		return startVal;
	}

	/**
	 * Rearrange the elements of an ArrayList into two groups and combine them. This
	 * method rearranges the elements of a given ArrayList into two groups, where
	 * the elements before a specified split index form the first group, and the
	 * elements after the split index form the second group. It then combines the
	 * two groups to produce a new ArrayList. *
	 * 
	 * @param splitIndex  The index at which the ArrayList should be split into two
	 *                    groups.
	 * @param singleArray An ArrayList of Double values to be rearranged and
	 *                    combined.
	 * @return An ArrayList of Double values with elements arranged in the order of
	 *         the second group followed by the first group.
	 */

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

	/**
	 * Get the index corresponding to a specific hour and minute in a 15-minute
	 * interval grid. This method calculates and returns the index that corresponds
	 * to a specified hour and minute within a 15-minute interval grid. The grid
	 * represents a day, divided into 15-minute intervals.
	 *
	 * @param hour            An Integer representing the hour component.
	 * @param minute          An Integer representing the minute component.
	 * @param hyperParameters is the object of class HyperParameters.
	 * @return An Integer representing the index in the 15-minute interval grid for
	 *         the given hour and minute.
	 */

	public static Integer getIndex(Integer hour, Integer minute, HyperParameters hyperParameters) {

		int k = 0;
		for (int i = 0; i < 24; i++) {
			for (int j = 0; j < (int) 60 / hyperParameters.getInterval(); j++) {
				int h = i;
				int m = j * hyperParameters.getInterval();
				if (hour == h && minute == m) {

					return k;
				} else {
					k = k + 1;
				}
			}
		}

		return k;

	}

	/**
	 * Predict a trend value for a given dataset using a trained model. This method
	 * predicts a trend value for a given dataset using a trained model. The method
	 * reads the model data, performs data interpolation, scales the data, and uses
	 * the model to make the prediction. It then scales back the predicted value to
	 * the original range.
	 *
	 * @param data            An ArrayList of Double representing the dataset for
	 *                        prediction.
	 * @param until           A ZonedDateTime representing the time until which the
	 * @param date            Corresponding date of the data. prediction
	 * @param hyperParameters object of class HyperParameter
	 * @return A double representing the predicted trend value.
	 */

	public ArrayList<Double> predictTrend(ArrayList<Double> data, ArrayList<OffsetDateTime> date, ZonedDateTime until,
			HyperParameters hyperParameters) {
		/// read Model
		String pathTrend = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\Trend.txt";
		// String pathSeasonality =
		// "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\SavedModel.txt";
		double pred = 0;
		ArrayList<Double> trendPrediction = new ArrayList<Double>();
		ZonedDateTime predictionFor = until.plusMinutes(hyperParameters.getInterval());
		int modlelindex = (int) this.decodeDateToColumnIndex(predictionFor, hyperParameters);

		// ArrayList<Double>prediction = new ArrayList<Double>();
		ArrayList<ArrayList<ArrayList<Double>>> val = ReadModels.getModelForSeasonality(pathTrend, hyperParameters)
				.get(0);

		InterpolationManager interpolationManager = new InterpolationManager(data, date, hyperParameters);
		data = interpolationManager.getInterpolatedData();

		ArrayList<Double> scaledData = DataModification.scale(data, hyperParameters.getScalingMin(),
				hyperParameters.getScalingMax());
		// readData
		for (int i = 0; i < hyperParameters.getTrendPoint(); i++) {

			double predTemp = Predictor.predict(scaledData, val.get(modlelindex).get(0), val.get(modlelindex).get(1),
					val.get(modlelindex).get(2), val.get(modlelindex).get(3), val.get(modlelindex).get(4),
					val.get(modlelindex).get(5), val.get(modlelindex).get(7), val.get(modlelindex).get(6));
			scaledData.add(predTemp);
			scaledData.remove(0);
			// System.out.println(scaledData);

			pred = DataModification.scaleBack(predTemp, hyperParameters.getScalingMin(),
					hyperParameters.getScalingMax());
			trendPrediction.add(pred);
			// System.out.println(pred);
		}
		// prediction.add(pred);

		return trendPrediction;

	}

	/**
	 * Decodes a given ZonedDateTime into a column index based on the hour and
	 * minute. The method calculates the column index of the model to be used by
	 * converting the given hour and minute into a standardized time representation
	 * where each 15-minute interval is assigned a unique index.
	 *
	 * @param predictionFor   The ZonedDateTime for which the column index is to be
	 * @param hyperParameters is the object of class HyperParameters. decoded.
	 * @return The column index corresponding to the specified ZonedDateTime.
	 * 
	 */

	public double decodeDateToColumnIndex(ZonedDateTime predictionFor, HyperParameters hyperParameters) {
		int hour = predictionFor.getHour();
		int minute = predictionFor.getMinute();
		int index = (Integer) hour * (60 / hyperParameters.getInterval()) + minute / hyperParameters.getInterval();
		int modifiedIndex = index - hyperParameters.getWindowSizeTrend();
		if (modifiedIndex >= 0) {
			return modifiedIndex;
		} else {
			return modifiedIndex + 60 / hyperParameters.getInterval() * 24;
		}
	}

}
