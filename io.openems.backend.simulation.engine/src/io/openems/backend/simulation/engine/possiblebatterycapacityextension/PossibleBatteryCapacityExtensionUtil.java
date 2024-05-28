package io.openems.backend.simulation.engine.possiblebatterycapacityextension;

import static io.openems.common.utils.JsonUtils.toJsonArray;
import static java.util.stream.Collectors.toMap;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.backend.common.edgewebsocket.EdgeWebsocket;
import io.openems.backend.common.metadata.Metadata;
import io.openems.backend.common.metadata.User;
import io.openems.backend.common.timedata.TimedataManager;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;

public final class PossibleBatteryCapacityExtensionUtil {

	private static final double THRESHOLD = 0.2;

	private PossibleBatteryCapacityExtensionUtil() {
	}

	/**
	 * Handles a {@link PossibleBatteryCapacityExtensionRequest}.
	 * 
	 * @param timedataManager the active {@link TimedataManager}
	 * @param user            the current user
	 * @param edgeId          the current edgeId
	 * @param metadata        the active {@link Metadata}
	 * @param edgeWebsocket   the active {@link EdgeWebsocket}
	 * @param request         the current
	 *                        {@link PossibleBatteryCapacityExtensionRequest}
	 * @return the result
	 * @throws OpenemsException on error
	 */
	public static CompletableFuture<JsonrpcResponseSuccess> handleRequest(//
			TimedataManager timedataManager, //
			User user, //
			String edgeId, //
			Metadata metadata, //
			EdgeWebsocket edgeWebsocket, //
			PossibleBatteryCapacityExtensionRequest request //
	) throws OpenemsException {

		var result = PossibleBatteryCapacityExtensionUtil.calculateRecommendationData(timedataManager, edgeId, request,
				user, metadata, edgeWebsocket);

		return CompletableFuture.completedFuture(//
				new PossibleBatteryCapacityExtensionResponse(request.id, result) //
		);
	}

	protected static JsonObject calculateRecommendationData(//
			TimedataManager timedataManager, //
			String edgeId, //
			PossibleBatteryCapacityExtensionRequest request, //
			User user, //
			Metadata metadata, //
			EdgeWebsocket edgeWebsocket //
	) throws OpenemsException {

		final var edge = metadata.getEdgeOrError(edgeId);

		if (!PossibleBatteryCapacityExtensionUtil.isRecommendationAllowedForProducttype(edge.getProducttype())) {
			throw new OpenemsException("Producttype is not supported: " + edge.getProducttype());
		}

		final var json = new JsonObject();
		try {
			final var datapoints = timedataManager.queryHistoricData(edgeId, request.getFromDate(), request.getToDate(),
					request.getPowerChannels(), new Resolution(5, ChronoUnit.MINUTES));

			/** TODO: use right channel */
			final var channel = ChannelAddress.fromString("battery0/NumberOfModulesPerTower");
			var numberOfModulesPerTower = edgeWebsocket.getChannelValues(edgeId, Set.of(channel)).entrySet().stream()
					.map(el -> JsonUtils.getAsOptionalInt(el.getValue())) //
					.filter(element -> element.isPresent()) //
					.map(element -> element.get()).findFirst();

			if (numberOfModulesPerTower.isEmpty()
					&& numberOfModulesPerTower.get() >= Home.PRODUCT_DETAILS.higherKey(numberOfModulesPerTower.get())) {
				return JsonUtils.buildJsonObject().build();
			}

			var maxNumberOfModules = Home.PRODUCT_DETAILS.lastKey();
			TreeMap<String, List<Integer>> previousResult = new TreeMap<>();

			for (int i = (numberOfModulesPerTower.get() + 1); i <= maxNumberOfModules; i++) {
				var result = PossibleBatteryCapacityExtensionUtil.calculateDataPerYear(datapoints, i,
						maxNumberOfModules, request);

				if (i > numberOfModulesPerTower.get() + 1) {
					if (!PossibleBatteryCapacityExtensionUtil.resultInThreshold(result, previousResult)) {
						continue;
					}
				}

				json.add(String.valueOf(i), JsonUtils.buildJsonObject() //
						.add("gridBuy", result.get("gridBuy").stream() //
								.map(JsonPrimitive::new) //
								.collect(toJsonArray()))
						.add("gridSell", result.get("gridSell").stream() //
								.map(JsonPrimitive::new) //
								.collect(toJsonArray()))
						.build());
				previousResult = result;
			}

		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
		return json;
	}

	protected static boolean resultInThreshold(TreeMap<String, List<Integer>> currentResult,
			TreeMap<String, List<Integer>> previousResult) {

		var difference = PossibleBatteryCapacityExtensionUtil.calculateDifferenceFactor(previousResult.get("gridSell"),
				currentResult.get("gridSell"));

		if (difference > THRESHOLD) {
			return true;
		}

		return false;
	}

	private static TreeMap<String, List<Integer>> calculateDataPerYear(
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> gridActivePower,
			int currentNumberOfModules, int maxNumberOfModules, PossibleBatteryCapacityExtensionRequest request)
			throws OpenemsNamedException {
		var list = new TreeMap<String, List<Integer>>();
		var newGridActivePower = PossibleBatteryCapacityExtensionUtil.calculateNewGridActivePower(gridActivePower,
				currentNumberOfModules, maxNumberOfModules);

		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> gridBuy = new TreeMap<>();
		SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> gridSell = new TreeMap<>();

		for (final var entry : newGridActivePower.entrySet()) {
			double value = entry //
					.getValue() //
					.values() //
					.stream() //
					.findFirst() //
					.flatMap(JsonUtils::getAsOptionalDouble) //
					.orElse(0.0);

			if (value >= 0.0) {
				gridBuy.put(entry.getKey(), entry.getValue());
				gridSell.put(entry.getKey(), ImmutableMap.<ChannelAddress, JsonElement>builder()
						.put(ChannelAddress.fromString("_sum/GridActivePower"), new JsonPrimitive(0.0)).build()
						.entrySet().stream()
						.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (t, u) -> u, () -> new TreeMap<>())));
			} else {
				gridSell.put(entry.getKey(), entry.getValue());
				gridBuy.put(entry.getKey(), ImmutableMap.<ChannelAddress, JsonElement>builder()
						.put(ChannelAddress.fromString("_sum/GridActivePower"), new JsonPrimitive(0.0)).build()
						.entrySet().stream()
						.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (t, u) -> u, () -> new TreeMap<>())));
			}
		}

		var normalizedGridSell = PossibleBatteryCapacityExtensionUtil.normalizeTable(gridSell)//
				.values().stream()
				.map(el -> JsonUtils.getAsOptionalInt(el.get(new ChannelAddress("_sum", "GridActivePower"))))
				.filter(el -> el.isPresent()).map(element -> {
					return Math.abs(element.orElse(0).intValue() / 12 / 1000);
				}).collect(Collectors.toList());

		var normalizedGridBuy = PossibleBatteryCapacityExtensionUtil.normalizeTable(gridBuy) //
				.values() //
				.stream() //
				.map(el -> JsonUtils.getAsOptionalInt(el.get(new ChannelAddress("_sum", "GridActivePower")))) //
				.filter(el -> el.isPresent()) //
				.map(element -> {
					return element.orElse(0).intValue() / 12 / 1000;
				}).collect(Collectors.toList());

		// TODO create separate type
		list.put("gridSell", normalizedGridSell);
		list.put("gridBuy", normalizedGridBuy);

		return list;
	}

	private static SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> normalizeTable(//
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> table //
	) {
		return table.entrySet().stream() //
				.collect(toMap(t -> {
					return t.getKey().truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1);
				}, Entry::getValue, (t, u) -> {
					return Stream.concat(t.entrySet().stream(), u.entrySet().stream()) //
							.collect(toMap(Entry::getKey, m -> {
								// TODO convert to watt hours here
								return m.getValue();
							}, (d, c) -> {
								if (d.isJsonNull()) {
									return c;
								}
								if (c.isJsonNull()) {
									return d;
								}
								return new JsonPrimitive(d.getAsDouble() + c.getAsDouble());
							}, TreeMap::new));
				}, TreeMap::new));
	}

	private static boolean isRecommendationAllowedForProducttype(String producttype) {
		return switch (producttype) {
		case Home.TYPE -> true;
		default -> false;
		};
	}

	protected static SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> calculateNewGridActivePower(
			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> gridActivePowerMap,
			int currentNumberOfModules, int maxNumberOfModules) throws OpenemsNamedException {

		// maximum number of battery modules
		int maxCapacityDifference = Math.subtractExact(Home.PRODUCT_DETAILS.get(maxNumberOfModules).capacity(), //
				Home.PRODUCT_DETAILS.get(currentNumberOfModules).capacity());

		if (maxCapacityDifference == 0) {
			return gridActivePowerMap;
		} else {

			SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> newGridActivePower = new TreeMap<>();

			int maxAdditionalBattery = maxCapacityDifference / 12; // [W per Hour]
			double additionalBatteryCapacity = 0.0; // [W per Period]

			for (Entry<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> entry : gridActivePowerMap.entrySet()) {
				double value = JsonUtils.getAsOptionalDouble(entry.getValue() //
						.get(ChannelAddress.fromString("_sum/GridActivePower"))).orElse(0.0);

				if (value > 0) {
					// GridBuy turns into discharge
					var maxDischarge = additionalBatteryCapacity; // 2200
					if (value <= maxDischarge) {
						additionalBatteryCapacity -= value; //
						value = 0.0;
					} else {
						value -= maxDischarge;
						additionalBatteryCapacity -= maxDischarge;
					}

				} else {
					// GridSell turns into charge
					var maxCharge = (maxAdditionalBattery - additionalBatteryCapacity) * -1; // 2200
					if (value >= maxCharge) {
						additionalBatteryCapacity -= value; //
						value = 0.0;
					} else {
						value -= maxCharge;
						additionalBatteryCapacity -= maxCharge;
					}
				}

				final double finalValue = value;
				newGridActivePower.put(entry.getKey(), ImmutableMap.<ChannelAddress, JsonElement>builder()
						.put(new ChannelAddress("_sum", "GridActivePower"), new JsonPrimitive(finalValue)).build()
						.entrySet().stream()
						.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (t, u) -> u, () -> new TreeMap<>())));
			}

			return newGridActivePower;
		}
	}

	private static double calculateDifferenceFactor(List<Integer> array1, List<Integer> array2) {
		// Check if the arrays have the same length
		if (array1.size() != array2.size()) {
			throw new IllegalArgumentException("Arrays must have the same length");
		}

		double sumOfDifferences = 0.0;

		// Iterate over the arrays and calculate the absolute difference for each
		// corresponding element
		for (int i = 0; i < array1.size(); i++) {
			double difference = Math.abs(array1.get(i) - array2.get(i));
			sumOfDifferences += difference;
		}

		// Calculate the average difference factor
		double differenceFactor = sumOfDifferences / array1.size();
		return differenceFactor;
	}

	// Mocked Data
	public static final class Home {

		private static final String TYPE = "home";
		private static final ImmutableSortedMap<Integer, ProductData> PRODUCT_DETAILS = ImmutableSortedMap
				.<Integer, ProductData>naturalOrder() //
				.put(4, new ProductData(4480, 8900)) //
				.put(5, new ProductData(5600, 11000)) //
				.put(6, new ProductData(6720, 13200)) //
				.put(7, new ProductData(7840, 15400)) //
				.put(8, new ProductData(8960, 17600)) //
				.put(9, new ProductData(10000, 19800)) //
				.put(10, new ProductData(10000, 22000)) //
				.build();

	}
}
