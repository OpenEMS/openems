package io.openems.edge.timeofusetariff.entsoe;

import static io.openems.common.utils.XmlUtils.stream;

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

import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.XmlUtils;
import io.openems.edge.common.currency.Currency;

public class Utils {

	private static final DateTimeFormatter FORMATTER_MINUTES = DateTimeFormatter.ofPattern("u-MM-dd'T'HH:mmX");

	private static record QueryResult(ZonedDateTime start, List<Float> prices) {
		protected static class Builder {
			private ZonedDateTime start;
			private List<Float> prices = new ArrayList<>();

			public Builder start(ZonedDateTime start) {
				this.start = start;
				return this;
			}

			public Builder prices(List<Float> prices) {
				this.prices.addAll(prices);
				return this;
			}

			public ImmutableSortedMap<ZonedDateTime, Float> toMap() {

				var result = new TreeMap<ZonedDateTime, Float>();
				var timestamp = this.start.withZoneSameInstant(ZoneId.systemDefault());
				var quarterHourIncrements = this.prices.size() * 4;

				for (int i = 0; i < quarterHourIncrements; i++) {
					result.put(timestamp, this.prices.get(i / 4));
					timestamp = timestamp.plusMinutes(15);
				}

				return ImmutableSortedMap.copyOf(result);
			}
		}

		public static Builder create() {
			return new Builder();
		}
	}

	/**
	 * Parses the xml response from the Entso-E API.
	 * 
	 * @param xml          The xml string to be parsed.
	 * @param resolution   PT15M or PT60M
	 * @param exchangeRate The exchange rate of user currency to EUR.
	 * @return The {@link ImmutableSortedMap}
	 * @throws ParserConfigurationException on error.
	 * @throws SAXException                 on error
	 * @throws IOException                  on error
	 */
	protected static ImmutableSortedMap<ZonedDateTime, Float> parse(String xml, String resolution, double exchangeRate)
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
							.map(s -> Float.parseFloat(s) * (float) exchangeRate) //
							.toList());
				});

		return result.toMap();
	}

	/**
	 * Parses the response string from Exchange rate API.
	 * 
	 * @param response The Response string from ExcahngeRate API.
	 * @param currency The {@link Curreny} selected by User.
	 * @return the exchange rate.
	 * @throws OpenemsNamedException on error.
	 */
	protected static Double exchangeRateParser(String response, Currency currency) throws OpenemsNamedException {

		var line = JsonUtils.parseToJsonObject(response);
		var data = JsonUtils.getAsJsonObject(line, "rates");

		return JsonUtils.getAsDouble(data, currency.toString());
	}

}
