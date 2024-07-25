package io.openems.edge.predictor.lstmmodel.train;

import static io.openems.edge.predictor.lstmmodel.preprocessing.DataModification.constantScaling;
import static io.openems.edge.predictor.lstmmodel.preprocessing.DataModification.removeNegatives;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.predictor.lstmmodel.LstmModel;
import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.common.ReadAndSaveModels;
import io.openems.edge.timedata.api.Timedata;

public class LstmTrain implements Runnable {

	private final Logger log = LoggerFactory.getLogger(LstmTrain.class);

	private Timedata timedata;
	private String channelAddress;
	private LstmModel parent;
	private long days;

	public LstmTrain(Timedata timedata, String channelAddress, LstmModel parent, long days) {
		this.timedata = timedata;
		this.channelAddress = channelAddress;
		this.parent = parent;
		this.days = days;
	}

	@Override
	public void run() {

		var nowDate = ZonedDateTime.now();
		var until = nowDate.withHour(23).withMinute(45).withSecond(0).withNano(0).minusDays(1);
		var fromDate = until.minusDays(this.days).withHour(0).withMinute(0).withSecond(0).withNano(0);

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> querryResult = new TreeMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>();
		HyperParameters hyperParameters = ReadAndSaveModels.read(this.channelAddress.split("/")[1]);

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> trainMap = new TreeMap<>();
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> validateMap = new TreeMap<>();

		try {
			querryResult = this.timedata.queryHistoricData(null, fromDate, until,
					Sets.newHashSet(ChannelAddress.fromString(this.channelAddress)),
					new Resolution(hyperParameters.getInterval(), ChronoUnit.MINUTES));

			int totalItems = querryResult.size();
			int trainSize = (int) (totalItems * 0.66); // 66% train and 33% validation

			int count = 0;
			for (Map.Entry<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> entry : querryResult.entrySet()) {
				if (count < trainSize) {
					trainMap.put(entry.getKey(), entry.getValue());
				} else {
					validateMap.put(entry.getKey(), entry.getValue());
				}
				count++;
			}

		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}

		// Get the training data
		var trainingData = this.getData(trainMap);

		if (this.cannotTrainConditions(trainingData)) {
			this.parent._setCannotTrainCondition(true);
			this.log.info("Cannot proceed with training: Data is all null or insufficient data.");
			return;
		}
		// Get the training Date
		var trainingDate = this.getDate(trainMap);
		// Get the training data
		var validationData = this.getData(validateMap);
		// Get the validationDate
		var validationDate = this.getDate(validateMap);

		/**
		 * TODO Read an save model.adapt method ReadAndSaveModels.adapt(hyperParameters,
		 * validateBatchData, validateBatchDate);
		 */
		new TrainAndValidateBatch(//
				constantScaling(removeNegatives(trainingData), 1), trainingDate, //
				constantScaling(removeNegatives(validationData), 1), validationDate, //
				hyperParameters);

		this.parent._setLastTrainedTime(hyperParameters.getLastTrainedDate().toString());
		this.parent._setModelError(Collections.min(hyperParameters.getRmsErrorSeasonality()));
		this.parent._setCannotTrainCondition(false);

	}

	/**
	 * Extracts data values.
	 *
	 * @param queryResult The SortedMap queryResult.
	 * @return An ArrayList of Double values extracted from non-null JsonElement
	 *         values.
	 */
	public ArrayList<Double> getData(SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryResult) {

		ArrayList<Double> data = new ArrayList<>();

		queryResult.values().stream()//
				.map(SortedMap::values)//
				.flatMap(Collection::stream)//
				.map(v -> {
					if (v.isJsonNull()) {
						return null;
					}
					return v.getAsDouble();
				}).forEach(value -> data.add(value));

		return data;
	}

	/**
	 * Extracts OffsetDateTime objects from the keys of a SortedMap containing
	 * ZonedDateTime keys.
	 *
	 * @param queryResult The SortedMap containing ZonedDateTime keys and associated
	 *                    data.
	 * @return An ArrayList of OffsetDateTime objects extracted from the
	 *         ZonedDateTime keys.
	 */
	public ArrayList<OffsetDateTime> getDate(
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryResult) {
		return queryResult.keySet().stream()//
				.map(ZonedDateTime::toOffsetDateTime)//
				.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Checks if all elements in an ArrayList are null.
	 *
	 * @param array The ArrayList to be checked.
	 * @return true if all elements in the ArrayList are null, false otherwise.
	 */
	private boolean cannotTrainConditions(ArrayList<Double> array) {
		if (array.isEmpty()) {
			return true; // Cannot train with no data
		}

		boolean allNulls = array.stream().allMatch(Objects::isNull);
		if (allNulls) {
			return true; // Cannot train with all null data
		}

		var nonNanCount = array.stream().filter(d -> d != null && !Double.isNaN(d)).count();
		var validProportion = (double) nonNanCount / array.size();
		return validProportion <= 0.5; // Cannot train with 50% or more invalid data
	}
}
