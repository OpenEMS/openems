package io.openems.edge.predictor.similardaymodel;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import java.util.stream.Collectors;

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
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.predictor.api.oneday.AbstractPredictor24Hours;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;
import io.openems.edge.predictor.api.oneday.Predictor24Hours;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Predictor.SimilardayModel", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)

public class SimilarDayPredictorImpl extends AbstractPredictor24Hours implements Predictor24Hours, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(SimilarDayPredictorImpl.class);

	public static final int NUM_OF_DAYS_OF_WEEK = 7;
	public static final int PREDCTION_FOR_ONE_DAY = 0;
	public static final int PREDCTION_FOR_TWO_DAY = 1;
	public static final int PREDCTION_FOR_THREE_DAY = 2;
	public static final int PREDCTION_FOR_FOUR_DAY = 3;
	public static final int PREDCTION_FOR_FIVE_DAY = 4;
	public static final int PREDCTION_FOR_SIX_DAY = 5;
	public static final int PREDCTION_FOR_SEVEN_DAY = 6;

	@Reference
	private Timedata timedata;

	protected Config config;

	@Reference
	private ComponentManager componentManager;

	public SimilarDayPredictorImpl() throws OpenemsNamedException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				SimilarDayPredictor.ChannelId.values() //
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

		var now = ZonedDateTime.now(this.componentManager.getClock());
		// From now time to Last 4 weeks
		var fromDate = now.minus(this.config.numOfWeeks(), ChronoUnit.WEEKS);

		final SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryResult;

		// Query database
		try {
			queryResult = this.timedata.queryHistoricData(null, fromDate, now, Sets.newHashSet(channelAddress),
					new Resolution(15, ChronoUnit.MINUTES));
		} catch (OpenemsNamedException e) {
			this.logError(this.log, e.getMessage());
			e.printStackTrace();
			return Prediction24Hours.EMPTY;
		}

		// Extract data
		List<Integer> result = queryResult.values().stream() //
				.map(SortedMap::values) //
				// extract JsonElement values as flat stream
				.flatMap(Collection::stream) //
				// convert JsonElement to Integer
				.map(v -> {
					if (v.isJsonNull()) {
						return (Integer) null;
					}
					return v.getAsInt();
				})
				// get as Array
				.collect(Collectors.toList());

		// Num of Data per day
		// TODO change this variable based on the resolution which is 900 in query
		var numOfDataPerDay = 96;

		var mainData = getSlicedArrayList(result, numOfDataPerDay);

		// Getting the indexes of the last four similar days
		var lastFourSimilarDays = getCorrectIndexes(mainData, NUM_OF_DAYS_OF_WEEK, PREDCTION_FOR_ONE_DAY);

		// Getting the average predictions
		var nextOneDayPredictions = getAverage(lastFourSimilarDays);

		return new Prediction24Hours(nextOneDayPredictions.stream().toArray(Integer[]::new));
	}

	/**
	 * This methods takes a List of integers and returns a 2dimension List of
	 * integers, specific to correct days.
	 *
	 * @param arrlist array list of all data.
	 * @param n       number of data per day.
	 * @return 2dimension array list
	 */
	private static List<List<Integer>> getSlicedArrayList(List<Integer> arrlist, int n) {
		List<List<Integer>> twoDimensionalArrayList = new ArrayList<>();
		for (var i = 0; i < arrlist.size(); i = i + n) {
			twoDimensionalArrayList.add(arrlist.subList(i, i + n));
		}
		return twoDimensionalArrayList;

	}

	/**
	 * This methods get the average of data based on the indexes.
	 *
	 * @param twoDimensionalArrayList The actual data.
	 * @return Average values of the last four days.
	 */
	private static List<Integer> getAverage(List<List<Integer>> twoDimensionalArrayList) {
		List<Integer> averageList = new ArrayList<>();
		var rows = twoDimensionalArrayList.size();
		var cols = twoDimensionalArrayList.get(0).size();
		for (var i = 0; i < cols; i++) {
			var sumRow = 0;
			for (var j = 0; j < rows; j++) {
				if (twoDimensionalArrayList.get(j).get(i) != null) {
					sumRow += twoDimensionalArrayList.get(j).get(i);
				}
			}
			averageList.add(sumRow / twoDimensionalArrayList.size());
		}
		return averageList;

	}

	/**
	 * Data manipulation, to get the proper indexes.
	 *
	 * @param mainData      all data points.
	 * @param numDaysOfWeek total number of days of week.
	 * @param whichDay      current actual day.
	 * @return proper indexed days.
	 */
	private static List<List<Integer>> getCorrectIndexes(List<List<Integer>> mainData, int numDaysOfWeek,
			int whichDay) {
		List<Integer> indexes = new ArrayList<>();
		List<List<Integer>> days = new ArrayList<>();
		for (var i = 0; i < mainData.size(); i++) {
			if (isMember(whichDay, numDaysOfWeek, i)) {
				indexes.add(i);
			}
		}

		for (Integer i : indexes) {
			days.add(mainData.get(i));
		}

		return days;

	}

	/**
	 * Check if the day belongs to correct day.
	 *
	 * @param whichDay      current actual day.
	 * @param numDaysOfWeek total number of days of week.
	 * @param nthTerm       nthterm
	 * @return boolean value to represent is value member of correct day
	 */
	private static boolean isMember(int whichDay, int numDaysOfWeek, int nthTerm) {
		if (numDaysOfWeek == 0) {
			return nthTerm == whichDay;
		}
		return (nthTerm - whichDay) % numDaysOfWeek == 0 && (nthTerm - whichDay) / numDaysOfWeek >= 0;

	}

}
