package io.openems.edge.timeofusetariff.entsoe;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.common.utils.XmlUtils.getXmlRootDocument;
import static io.openems.common.utils.XmlUtils.stream;
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
import java.util.function.Function;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Streams;

import io.openems.common.utils.XmlUtils;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

public class Utils {

	private record TimeInterval(ZonedDateTime start, ZonedDateTime end) {
	}

	protected static final DateTimeFormatter FORMATTER_MINUTES = DateTimeFormatter.ofPattern("u-MM-dd'T'HH:mmX");

	/**
	 * Parses the XML response from the Entso-E API to get the Day-Ahead prices.
	 * 
	 * @param xml                 The XML string to be parsed.
	 * @param exchangeRate        The exchange rate of user currency to EUR.
	 * @param preferredResolution The user preferred resolution.
	 * @return The {@link ImmutableSortedMap}
	 * @throws ParserConfigurationException on error.
	 * @throws SAXException                 on error
	 * @throws IOException                  on error
	 */
	protected static ImmutableSortedMap<ZonedDateTime, Double> parsePrices(String xml, double exchangeRate,
			Resolution preferredResolution) throws ParserConfigurationException, SAXException, IOException {
		var root = getXmlRootDocument(xml);

		var allPrices = parseXml(root);

		if (allPrices.isEmpty()) {
			return ImmutableSortedMap.of();
		}

		final var globalTimeInterval = parseTimeInterval(root, "period.timeInterval");
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
						t -> durations.stream() //
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
								.findFirst().orElse(null)));
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
}
