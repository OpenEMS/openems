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

		System.out.println("=====> Training for : " + this.channelAddress);

		ZonedDateTime nowDate = ZonedDateTime.now();
		ZonedDateTime until = nowDate.withHour(23).withMinute(45).withSecond(0).withNano(0).minusDays(1);
		ZonedDateTime fromDate = until.minusDays(this.days).withHour(0).withMinute(0).withSecond(0).withNano(0);

		// (60/ 5) * 24* 45
		// (60/ 5) * 24* 15
		// 66 percent and 33 percent

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> querryResult = new TreeMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>();
		HyperParameters hyperParameters = ReadAndSaveModels.read(this.channelAddress.split("/")[1]);

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> trainMap = new TreeMap<>();
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> validateMap = new TreeMap<>();

		try {
			querryResult = this.timedata.queryHistoricData(null, fromDate, until,
					Sets.newHashSet(ChannelAddress.fromString(this.channelAddress)),
					new Resolution(hyperParameters.getInterval(), ChronoUnit.MINUTES));

			int totalItems = querryResult.size();
			int trainSize = (int) (totalItems * 0.66);

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

		var trainingData = this.getData(trainMap);
		var trainingDate = this.getDate(trainMap);

		var validationData = this.getData(validateMap);
		var validationDate = this.getDate(validateMap);

		// TODO call adapt method
		// Read an save model.adapt method
		// ReadAndSaveModels.adapt(hyperParameters, validateBatchData,
		// validateBatchDate);

		new TrainAndValidateBatch(//
				constantScaling(removeNegatives(trainingData), 1), trainingDate, //
				constantScaling(removeNegatives(validationData), 1), validationDate, //
				hyperParameters);

		this.parent._setLastTrainedTime(hyperParameters.getLastTrainedDate().toString());
		this.parent._setModelError(Collections.min(hyperParameters.getRmsErrorSeasonality()));

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

		// TODO remove this later
		if (this.isAllNulls(data)) {
			System.out.println("Data is all null, use a different predictor");
		}

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
	private boolean isAllNulls(ArrayList<Double> array) {
		return array.stream().allMatch(Objects::isNull);
	}

}
