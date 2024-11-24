package io.openems.edge.timeofusetariff.entsoe;

import static io.openems.common.utils.XmlUtils.stream;
import static io.openems.common.utils.XmlUtils.getXmlRootDocument;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;

import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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

import io.openems.common.utils.XmlUtils;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

public class Utils {

	protected static final DateTimeFormatter FORMATTER_MINUTES = DateTimeFormatter.ofPattern("u-MM-dd'T'HH:mmX");

	/**
	 * Parses the XML response from the Entso-E API to get the Day-Ahead prices.
	 * 
	 * @param xml          The XML string to be parsed.
	 * @param exchangeRate The exchange rate of user currency to EUR.
	 * @return The {@link TimeOfUsePrices}
	 * @throws ParserConfigurationException on error.
	 * @throws SAXException                 on error
	 * @throws IOException                  on error
	 */
	protected static TimeOfUsePrices parsePrices(String xml, double exchangeRate)
			throws ParserConfigurationException, SAXException, IOException {
		var root = getXmlRootDocument(xml);

		var allPrices = parseXml(root, exchangeRate);

		if (allPrices.isEmpty()) {
			return TimeOfUsePrices.EMPTY_PRICES;
		}

		var shortestDuration = allPrices.rowKeySet().stream() //
				.sorted() //
				.findFirst().get();

		final var prices = ImmutableSortedMap.copyOf(allPrices.row(shortestDuration));
		final var minTimestamp = prices.firstKey();
		final var maxTimestamp = prices.lastKey().plus(shortestDuration);

		var result = Stream //
				.iterate(minTimestamp, //
						t -> t.isBefore(maxTimestamp), //
						t -> t.plusMinutes(15)) //
				.collect(ImmutableSortedMap.<ZonedDateTime, ZonedDateTime, Double>toImmutableSortedMap(//
						Comparator.naturalOrder(), //
						Function.identity(), //
						t -> prices.floorEntry(t).getValue()));

		return TimeOfUsePrices.from(result);
	}

	protected static ImmutableTable<Duration, ZonedDateTime, Double> parseXml(Element root, double exchangeRate) {
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
						parsePeriod(result, period, exchangeRate);
					} catch (Exception e) {
						e.printStackTrace();
					}
				});
		return result.build();
	}

	protected static void parsePeriod(ImmutableTable.Builder<Duration, ZonedDateTime, Double> result, Node period,
			double exchangeRate) throws Exception {
		final var duration = Duration.parse(stream(period) //
				// <resolution>
				.filter(n -> n.getNodeName() == "resolution") //
				.map(XmlUtils::getContentAsString) //
				.findFirst().get() /* "PT15M" or "PT60M" */);
		final var start = ZonedDateTime.parse(//
				stream(period) //
						// <timeInterval>
						.filter(n -> n.getNodeName() == "timeInterval") //
						.flatMap(XmlUtils::stream) //
						// <start>
						.filter(n -> n.getNodeName() == "start") //
						.map(XmlUtils::getContentAsString) //
						.findFirst().get(),
				FORMATTER_MINUTES).withZoneSameInstant(ZoneId.of("UTC"));

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
					final var timestamp = start.plusMinutes((position - 1) * duration.toMinutes());
					final var price = stream(point) //
							// <price.amount>
							.filter(n -> n.getNodeName() == "price.amount") //
							.map(XmlUtils::getContentAsString) //
							.map(s -> parseDouble(s) * exchangeRate) //
							.findFirst().get();
					result.put(duration, timestamp, price);
				});
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

}
