package io.openems.edge.timeofusetariff.entsoe;

import static io.openems.common.utils.XmlUtils.stream;
import static java.lang.Double.parseDouble;

import java.io.IOException;
import java.io.StringReader;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import io.openems.common.utils.XmlUtils;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

public class Utils {

	private static final DateTimeFormatter FORMATTER_MINUTES = DateTimeFormatter.ofPattern("u-MM-dd'T'HH:mmX");

	private static record QueryResult(ZonedDateTime start, List<Float> prices) {
		protected static class Builder {
			private ZonedDateTime start;
			private List<Double> prices = new ArrayList<>();

			public Builder start(ZonedDateTime start) {
				this.start = start;
				return this;
			}

			public Builder prices(List<Double> prices) {
				this.prices.addAll(prices);
				return this;
			}

			public TimeOfUsePrices toTimeOfUsePrices() {
				var result = new TreeMap<ZonedDateTime, Double>();
				var timestamp = this.start.withZoneSameInstant(ZoneId.systemDefault());
				var quarterHourIncrements = this.prices.size() * 4;

				for (int i = 0; i < quarterHourIncrements; i++) {
					result.put(timestamp, this.prices.get(i / 4));
					timestamp = timestamp.plusMinutes(15);
				}
				return TimeOfUsePrices.from(result);
			}
		}

		public static Builder create() {
			return new Builder();
		}
	}

	/**
	 * Parses the XML response from the Entso-E API to get the Day-Ahead prices.
	 * 
	 * @param xml          The XML string to be parsed.
	 * @param resolution   PT15M or PT60M
	 * @param exchangeRate The exchange rate of user currency to EUR.
	 * @return The {@link TimeOfUsePrices}
	 * @throws ParserConfigurationException on error.
	 * @throws SAXException                 on error
	 * @throws IOException                  on error
	 */
	protected static TimeOfUsePrices parsePrices(String xml, String resolution, double exchangeRate)
			throws ParserConfigurationException, SAXException, IOException {
		var dbFactory = DocumentBuilderFactory.newInstance();
		var dBuilder = dbFactory.newDocumentBuilder();
		var is = new InputSource(new StringReader(xml));
		var doc = dBuilder.parse(is);
		var root = doc.getDocumentElement();
		var result = QueryResult.create();

		stream(root) //
				// <TimeSeries>
				.filter(n -> n.getNodeName() == "TimeSeries") //
				.flatMap(XmlUtils::stream) //
				// <Period>
				.filter(n -> n.getNodeName() == "Period") //
				// Find Period with correct resolution
				.filter(p -> stream(p) //
						.filter(n -> n.getNodeName() == "resolution") //
						.map(XmlUtils::getContentAsString) //
						.anyMatch(r -> r.equals(resolution))) //
				.forEach(period -> {

					var start = ZonedDateTime.parse(//
							stream(period) //
									// <timeInterval>
									.filter(n -> n.getNodeName() == "timeInterval") //
									.flatMap(XmlUtils::stream) //
									// <start>
									.filter(n -> n.getNodeName() == "start") //
									.map(XmlUtils::getContentAsString) //
									.findFirst().get(),
							FORMATTER_MINUTES).withZoneSameInstant(ZoneId.of("UTC"));

					if (result.start == null) {
						// Avoiding overwriting of start due to multiple periods.
						result.start(start);
					}

					result.prices(stream(period) //
							// <Point>
							.filter(n -> n.getNodeName() == "Point") //
							.flatMap(XmlUtils::stream) //
							// <price.amount>
							.filter(n -> n.getNodeName() == "price.amount") //
							.map(XmlUtils::getContentAsString) //
							.map(s -> parseDouble(s) * exchangeRate) //
							.toList());
				});

		return result.toTimeOfUsePrices();
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
