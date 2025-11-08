package io.openems.edge.timeofusetariff.entsoe;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.common.utils.XmlUtils.getXmlRootDocument;
import static io.openems.common.utils.XmlUtils.stream;
import static io.openems.edge.timeofusetariff.api.AncillaryCosts.parseForGermany;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

import java.io.IOException;
import java.io.StringReader;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Streams;

import io.openems.common.bridge.http.time.DelayTimeProvider.Delay;
import io.openems.common.bridge.http.time.DelayTimeProviderChain;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jscalendar.JSCalendar.Task;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.utils.DateUtils;
import io.openems.common.utils.XmlUtils;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

public class Utils {

	private record TimeInterval(ZonedDateTime start, ZonedDateTime end) {
	}

	private static final int API_EXECUTE_HOUR = 14;
	protected static final DateTimeFormatter FORMATTER_MINUTES = DateTimeFormatter.ofPattern("u-MM-dd'T'HH:mmX");

	/**
	 * Parses the XML response from the Entso-E API to get the Day-Ahead prices.
	 * 
	 * @param xml                 The XML string to be parsed.
	 * @param preferredResolution The user preferred resolution.
	 * @param biddingZone         The {@link BiddingZone}
	 * @return The {@link ImmutableSortedMap}
	 * @throws ParserConfigurationException on error.
	 * @throws SAXException                 on error
	 * @throws IOException                  on error
	 */
	protected static ImmutableSortedMap<ZonedDateTime, Double> parsePrices(String xml, Resolution preferredResolution,
			BiddingZone biddingZone) throws ParserConfigurationException, SAXException, IOException {
		var root = getXmlRootDocument(xml);

		final var globalTimeInterval = parseTimeInterval(root, "period.timeInterval");

		var allPrices = parseXmlWithFallback(root, preferredResolution, globalTimeInterval, biddingZone);

		if (allPrices.isEmpty()) {
			return ImmutableSortedMap.of();
		}

		final var durations = Streams // Sorted Durations. Starts with preferredResolution
				.concat(Stream.of(getDuration(allPrices, preferredResolution)), allPrices.rowKeySet().stream().sorted()) //
				.distinct() //
				.toList();

		return Stream //
				.iterate(globalTimeInterval.start, //
						t -> t.isBefore(globalTimeInterval.end), //
						t -> t.plusMinutes(15)) //
				.collect(ImmutableSortedMap.<ZonedDateTime, ZonedDateTime, Double>toImmutableSortedMap(//
						Comparator.naturalOrder(), //
						Function.identity(), //
						t -> {
							return durations.stream() //
									.map(duration -> {
										var time = Stream.of(Resolution.values()) //
												.filter(r -> r.duration.equals(duration)) //
												.map(r -> switch (r) {
												case HOURLY -> t.truncatedTo(ChronoUnit.HOURS);
												case QUARTERLY -> roundDownToQuarter(t);
												}) //
												.findFirst().orElse(t); // should not happen
										return allPrices.get(duration, time);
									}) //
									.filter(Objects::nonNull) //
									.findFirst().orElseGet(() -> {
										return null;
									});
						}));

	}

	protected static ImmutableTable<Duration, ZonedDateTime, Double> parseXmlWithFallback(Element root,
			Resolution preferredResolution, TimeInterval globalTimeInterval, BiddingZone biddingZone) {
		// Check if classificationSequence exists
		var hasClassificationSequence = stream(root) //
				.filter(n -> n.getNodeName() == "TimeSeries") //
				.anyMatch(timeSeries -> hasSequence(timeSeries));

		if (!hasClassificationSequence) {
			// Fallback to old logic - parse all TimeSeries without position filtering
			return parseXml(root);
		} else {
			// merge sequence 1 and position 2
			var sequence1Prices = parseXmlForSequence(root, 1);
			var sequence2Prices = parseXmlForSequence(root, 2);

			return mergeSequences(sequence1Prices, sequence2Prices, root, preferredResolution, globalTimeInterval,
					biddingZone);
		}
	}

	protected static TimeOfUsePrices processPrices(Clock clock, ImmutableSortedMap<ZonedDateTime, Double> timePriceMap,
			double exchangeRate, TimeOfUsePrices gridFees) {

		// Filter timePriceMap to include only entries from "now" onward.
		// filteredMap will have 34 hour data maximum (when called during 14:00).
		var filteredPrices = timePriceMap.tailMap(ZonedDateTime.now(clock), true);

		if (filteredPrices.isEmpty()) {
			return TimeOfUsePrices.EMPTY_PRICES; // or handle as appropriate
		}

		// always consists of 36 hour values.
		var gridFeesArray = gridFees.asArray();
		var filterSize = Math.min(filteredPrices.size(), gridFeesArray.length);

		// Trim grid fees
		gridFeesArray = Arrays.copyOf(gridFeesArray, filterSize);

		// Build the result map with adjusted prices
		ImmutableSortedMap.Builder<ZonedDateTime, Double> resultBuilder = new ImmutableSortedMap.Builder<>(
				Comparator.naturalOrder());

		int index = 0;
		for (var entry : filteredPrices.entrySet()) {
			if (index >= gridFeesArray.length) {
				break; // defensive check
			}

			// converting grid fees from ct/KWh -> EUR/MWh
			var gridFeesPerMwh = gridFeesArray[index] * 10;
			var priceWithFee = (entry.getValue() + gridFeesPerMwh) * exchangeRate;
			resultBuilder.put(entry.getKey(), priceWithFee);
			index++;
		}

		return TimeOfUsePrices.from(resultBuilder.build());
	}

	protected static ImmutableTable<Duration, ZonedDateTime, Double> parseXml(Element root) {
		var result = ImmutableTable.<Duration, ZonedDateTime, Double>builder();
		stream(root) //
				// <TimeSeries>
				.filter(n -> n.getNodeName() == "TimeSeries") //
				.flatMap(XmlUtils::stream) //
				// <Period>
				.filter(n -> n.getNodeName() == "Period") //
				// Find Period with correct resolution
				.forEach(period -> {
					try {
						parsePeriod(result, period);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
		return result.build();
	}

	protected static TimeInterval parseTimeInterval(Node node, String nodeName) {
		return parseTimeInterval(stream(node) //
				// <period.timeInterval>
				.filter(n -> n.getNodeName() == nodeName) //
				.flatMap(XmlUtils::stream) //
				.toList());
	}

	private static TimeInterval parseTimeInterval(List<Node> nodes) {
		var start = ZonedDateTime.parse(//
				nodes.stream() //
						// <start>2025-01-17T23:00Z</start>
						.filter(n -> n.getNodeName() == "start") //
						.map(XmlUtils::getContentAsString) //
						.findFirst().get(),
				FORMATTER_MINUTES).withZoneSameInstant(ZoneId.of("UTC"));
		var end = ZonedDateTime.parse(//
				nodes.stream() //
						// <end>2025-01-18T23:00Z</end>
						.filter(n -> n.getNodeName() == "end") //
						.map(XmlUtils::getContentAsString) //
						.findFirst().get(),
				FORMATTER_MINUTES).withZoneSameInstant(ZoneId.of("UTC"));
		return new TimeInterval(start, end);
	}

	protected static ImmutableTable<Duration, ZonedDateTime, Double> parseXmlForSequence(Element root, int position) {
		var result = ImmutableTable.<Duration, ZonedDateTime, Double>builder();
		stream(root) //
				// <TimeSeries>
				.filter(n -> n.getNodeName() == "TimeSeries") //
				.filter(timeSeries -> hasExpectedSequence(timeSeries, position)) //
				.flatMap(XmlUtils::stream) //
				// <Period>
				.filter(n -> n.getNodeName() == "Period") //
				// Find Period with correct resolution
				.forEach(period -> {
					try {
						parsePeriod(result, period);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
		return result.build();
	}

	private static ImmutableTable<Duration, ZonedDateTime, Double> mergeSequences(
			ImmutableTable<Duration, ZonedDateTime, Double> sequence1,
			ImmutableTable<Duration, ZonedDateTime, Double> sequence2, Element root, Resolution preferredResolution,
			TimeInterval globalTimeInterval, BiddingZone biddingZone) {

		var result = ImmutableTable.<Duration, ZonedDateTime, Double>builder();

		// If position 1 is empty, use position 2 entirely
		if (sequence1.isEmpty()) {
			return sequence2;
		}

		// If position 2 is empty, use position 1 entirely
		if (sequence2.isEmpty()) {
			return sequence1;
		}

		// Get the global time interval to know all expected time slots
		var duration = getDuration(sequence1, preferredResolution);

		// Iterate through all expected time slots
		var currentTime = globalTimeInterval.start;
		while (currentTime.isBefore(globalTimeInterval.end)) {

			Double price;

			switch (biddingZone) {
			case GERMANY, AUSTRIA -> {
				// get price from position 1 first
				price = getPrice(sequence1, currentTime);

				// If position 1 doesn't have this time slot, try position 2
				if (price == null) {
					price = getPrice(sequence2, currentTime);
				}
			}
			default -> {
				price = getPrice(sequence2, currentTime);

				if (price == null) {
					price = getPrice(sequence1, currentTime);
				}
			}
			}

			// If we found a price (from either position), add it to result
			if (price != null) {
				result.put(duration, currentTime, price);
			}

			// Move to next time slot based on resolution
			currentTime = currentTime.plus(Resolution.QUARTERLY.duration);
		}

		return result.build();
	}

	private static boolean hasSequence(Node timeSeries) {
		return stream(timeSeries) //
				// <classificationSequence_AttributeInstanceComponent.position>
				.filter(n -> n.getNodeName() == "classificationSequence_AttributeInstanceComponent.position") //
				.findAny() //
				.isPresent();
	}

	private static boolean hasExpectedSequence(Node timeSeries, int expectedSequence) {
		return stream(timeSeries) //
				// <classificationSequence_AttributeInstanceComponent.position>
				.filter(n -> n.getNodeName() == "classificationSequence_AttributeInstanceComponent.position") //
				.map(XmlUtils::getContentAsString) //
				.map(Integer::parseInt) //
				.anyMatch(position -> position == expectedSequence);
	}

	private static Double getPrice(ImmutableTable<Duration, ZonedDateTime, Double> sequence,
			ZonedDateTime currentTime) {

		for (var entry : sequence.rowMap().entrySet()) {
			final var key = currentTime.truncatedTo(DurationUnit.of(entry.getKey()));
			return entry.getValue().get(key);
		}

		return null;
	}

	protected static void parsePeriod(ImmutableTable.Builder<Duration, ZonedDateTime, Double> result, Node period)
			throws Exception {
		final var duration = Duration.parse(stream(period) //
				// <resolution>
				.filter(n -> n.getNodeName() == "resolution") //
				.map(XmlUtils::getContentAsString) //
				.findFirst().get() /* "PT15M" or "PT60M" */);
		final var timeInterval = parseTimeInterval(period, "timeInterval");
		final var prices = new TreeMap<Integer, Double>();
		stream(period) //
				// <Point>
				.filter(n -> n.getNodeName() == "Point") //
				.forEach(point -> {
					final var position = stream(point) //
							// <position>
							.filter(n -> n.getNodeName() == "position") //
							.map(XmlUtils::getContentAsString) //
							.map(s -> parseInt(s)) //
							.findFirst().get();
					final var price = stream(point) //
							// <price.amount>
							.filter(n -> n.getNodeName() == "price.amount") //
							.map(XmlUtils::getContentAsString) //
							.map(s -> parseDouble(s)) //
							.findFirst().get();
					prices.put(position, price);
				});

		// Fill missing positions using the last known price
		Double price = null;
		for (var pos = 1;; pos++) {
			var timestamp = timeInterval.start.plusMinutes((pos - 1) * duration.toMinutes());
			if (!timestamp.isBefore(timeInterval.end)) {
				break;
			}
			price = prices.getOrDefault(pos, price);
			result.put(duration, timestamp, price);
		}
	}

	/**
	 * Parses the XML response from the Entso-E API to extract the currency
	 * associated with the prices.
	 * 
	 * @param xml The XML string to be parsed.
	 * @return The currency string.
	 * @throws ParserConfigurationException on error.
	 * @throws SAXException                 on error
	 * @throws IOException                  on error
	 */
	protected static String parseCurrency(String xml) throws ParserConfigurationException, SAXException, IOException {
		var dbFactory = DocumentBuilderFactory.newInstance();
		var dBuilder = dbFactory.newDocumentBuilder();
		var is = new InputSource(new StringReader(xml));
		var doc = dBuilder.parse(is);
		var root = doc.getDocumentElement();

		var result = stream(root) //
				// <TimeSeries>
				.filter(n -> n.getNodeName() == "TimeSeries") //
				.flatMap(XmlUtils::stream) //
				// <currency_Unit.name>
				.filter(n -> n.getNodeName() == "currency_Unit.name") //
				.map(XmlUtils::getContentAsString) //
				.findFirst().get();

		return result;
	}

	/**
	 * Determines the appropriate {@link Duration} from the given
	 * {@link ImmutableTable}.
	 * 
	 * <p>
	 * The method checks if the specified {@code preferredResolution} exists in the
	 * table's row keys. If it exists, the corresponding {@link Duration} is
	 * returned. Otherwise, the method selects the shortest duration from the
	 * available row keys.
	 * 
	 * @param allPrices           the {@link ImmutableTable}.
	 * @param preferredResolution the preferred resolution to look for, encapsulated
	 *                            in a {@link Resolution}.
	 * @return the matching {@link Duration} if the preferred resolution exists in
	 *         the table's row keys, otherwise the shortest {@link Duration} from
	 *         the row keys.
	 */
	protected static Duration getDuration(ImmutableTable<Duration, ZonedDateTime, Double> allPrices,
			Resolution preferredResolution) {
		return allPrices.rowKeySet().stream() //
				// match preferredResolution
				.filter(e -> e.equals(preferredResolution.duration)) //
				.findFirst() //
				// otherwise get shortest duration
				.orElseGet(() -> allPrices.rowKeySet().stream() //
						.sorted() //
						.findFirst()//
						.get());
	}

	/**
	 * Parses the ancillary cost configuration JSON into a schedule of
	 * {@link Task}s.
	 * 
	 * @param biddingZone    the {@link BiddingZone}
	 * @param ancillaryCosts the JSON configuration object
	 * @param logWarn        a {@link Consumer} for a warning message
	 * @return an {@link ImmutableList} of {@link Task} instances representing the
	 *         schedule or an empty list if no valid schedule is provided.
	 * @throws OpenemsNamedException on error.
	 */
	public static ImmutableList<Task<Double>> parseToSchedule(BiddingZone biddingZone, String ancillaryCosts,
			Consumer<String> logWarn) throws OpenemsNamedException {
		if (ancillaryCosts == null || ancillaryCosts.isBlank()) {
			return ImmutableList.of();
		}

		return switch (biddingZone) {
		case GERMANY //
			-> parseForGermany(ancillaryCosts);
		case AUSTRIA, BELGIUM, NETHERLANDS, SWEDEN_SE1, SWEDEN_SE2, SWEDEN_SE3, SWEDEN_SE4 -> {
			logWarn.accept("Parser for " + biddingZone.name() + "-Scheduler is not implemented");
			throw new OpenemsException("Parser for bidding zone " + biddingZone.name() + " is not implemented");
		}
		};

	}

	/**
	 * Calculates the Delay for the next run.
	 * 
	 * @param clock the Clock
	 * @param xml   xml response from API
	 * @return {@link Delay}
	 * @throws ParserConfigurationException on Error
	 * @throws SAXException                 on Error
	 * @throws IOException                  on Error
	 */
	public static Delay calculateDelay(Clock clock, String xml)
			throws ParserConfigurationException, SAXException, IOException {
		final var now = ZonedDateTime.now(clock);
		final var root = getXmlRootDocument(xml);

		final var hasClassificationSequence = stream(root) //
				.filter(n -> n.getNodeName() == "TimeSeries") //
				.anyMatch(timeSeries -> hasSequence(timeSeries));

		final ImmutableTable<Duration, ZonedDateTime, Double> prices;

		if (!hasClassificationSequence) {
			// Without Sequences
			prices = parseXml(root);
		} else {
			// only sequence 1
			prices = parseXmlForSequence(root, 1);
		}

		var nextRun = DateUtils.roundDownToQuarter(now.plusHours(1));

		// Case 1: No prices at all
		if (prices.isEmpty()) {
			return Delay.of(Duration.between(now, nextRun).minusMinutes(1));
		}

		var lastTimestamp = prices.columnKeySet().stream().max(Comparator.naturalOrder()).orElse(null);
		var nextMidnight = now.truncatedTo(ChronoUnit.DAYS).plusDays(1);

		// Case 2: Data doesn't extend to next day (incomplete)
		if (lastTimestamp == null || lastTimestamp.isBefore(nextMidnight)) {
			return Delay.of(Duration.between(now, nextRun).minusMinutes(1));
		}

		final var todayAtExecuteHour = now.truncatedTo(ChronoUnit.DAYS).plusHours(API_EXECUTE_HOUR);

		// Case 3: Complete data available, but before 14:00.
		if (now.isBefore(todayAtExecuteHour)) {
			return DelayTimeProviderChain.fixedDelay(Duration.between(now, todayAtExecuteHour)) //
					.plusRandomDelay(60, ChronoUnit.SECONDS) //
					.getDelay();
		}

		// Case 4: After 14:00, schedule for next day at 14:00
		return DelayTimeProviderChain.fixedAtEveryFull(clock, DurationUnit.ofHours(24)) //
				.plusFixedAmount(Duration.ofHours(API_EXECUTE_HOUR)) //
				.plusRandomDelay(60, ChronoUnit.SECONDS) //
				.getDelay();
	}

}
