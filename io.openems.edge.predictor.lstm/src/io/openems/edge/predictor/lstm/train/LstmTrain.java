package io.openems.edge.predictor.lstm.train;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.predictor.lstm.common.GetObject;
import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.common.ReadCsv;
import io.openems.edge.predictor.lstm.common.Saveobject;
import io.openems.edge.predictor.lstm.multithread.TrainInBatch;
import io.openems.edge.timedata.api.Timedata;

public class LstmTrain implements Runnable {

	private Timedata timedata;
	private String channelAddress;

	public LstmTrain(Timedata timedata, String channelAddress) {
		this.timedata = timedata;
		this.channelAddress = channelAddress;
	}

	@Override
	public void run() {

		HyperParameters hyperParameters;

		ZonedDateTime nowDate = ZonedDateTime.of(2023, 07, 21, 0, 0, 0, 0, ZonedDateTime.now().getZone());
		ZonedDateTime until = ZonedDateTime.of(//
				nowDate.getYear(), //
				nowDate.getMonthValue(), //
				nowDate.minusDays(1).getDayOfMonth(), //
				23, //
				45, //
				0, //
				0, //
				nowDate.getZone());
		ZonedDateTime temp = until.minusDays(30);

		ZonedDateTime fromDate = ZonedDateTime.of(//
				temp.getYear(), //
				temp.getMonthValue(), //
				temp.getDayOfMonth(), //
				0, //
				0, //
				0, //
				0, //
				ZonedDateTime.now().getZone());

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> querryResult = new TreeMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>>();

		try {
			querryResult = this.timedata.queryHistoricData(null, fromDate, until,
					Sets.newHashSet(ChannelAddress.fromString(this.channelAddress)),
					new Resolution(15, ChronoUnit.MINUTES));

		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}

  //		ArrayList<Double> data = this.getData(querryResult);
  //		ArrayList<OffsetDateTime> date = this.getDate(querryResult);

		// reading the csv file for as the valodation data

		// HyperParameters hyperParameters;
		try {
			hyperParameters = (HyperParameters) GetObject.get();
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Creating new hyperparameter object");
			hyperParameters = HyperParameters.getInstance();
		}
		int check = hyperParameters.getOuterLoopCount();
		for (int i = check; i <= 8; i++) {
			hyperParameters.setOuterLoopCount(i);

			String pathTrain = Integer.toString(i + 1) + ".csv";
			String pathValidate = Integer.toString(27) + ".csv";

			ReadCsv obj1 = new ReadCsv(pathTrain);
			final ReadCsv obj2 = new ReadCsv(pathValidate);

			new TrainInBatch(obj1.getData(), obj1.getDates(), obj2.getData(), obj2.getDates(), hyperParameters);

			hyperParameters.setEpochTrack(0);
			hyperParameters.setBatchTrack(0);
			hyperParameters.setOuterLoopCount(hyperParameters.getOuterLoopCount() + 1);
			Saveobject.save(hyperParameters);
		}

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
