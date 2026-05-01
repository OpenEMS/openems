package io.openems.common.bridge.http.thirdparty.entsoe;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.api.UrlBuilder;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProviderChain;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.types.EntsoeBiddingZone;
import io.openems.common.types.MarketPriceData;
import io.openems.common.utils.ArrayUtils;
import io.openems.common.utils.DateUtils;
import io.openems.common.utils.StringUtils;
import io.openems.common.utils.TimeRangeValues;
import io.openems.common.utils.TimeSpan;
import io.openems.common.xml.serialization.XmlObject;
import io.openems.common.xml.serialization.XmlParser;

public class EntsoeApi {
	public static final EntsoeApi INSTANCE = new EntsoeApi();
	public static final DateTimeFormatter URL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
	public static final DateTimeFormatter XML_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX");
	public static final ZoneId UTC = ZoneId.of("UTC");
	public static final String URI = "https://web-api.tp.entsoe.eu/api";

	/**
	 * Entsoe is providing new values after this hour of the day.
	 */
	public static final int ENTSOE_UPDATE_HOUR = 14;

	private EntsoeApi() {
	}

	/**
	 * Calculates metrics identifier for the specific entsoe request.
	 *
	 * @param endpoint Endpoint that is called
	 * @return Identifier as string
	 */
	public String getMetricsIdentifier(BridgeHttp.Endpoint endpoint) {
		if (endpoint.url().startsWith(URI)) {
			var urlBuilder = UrlBuilder.parse(endpoint.url());
			var biddingZoneCode = urlBuilder.queryParams().get("in_Domain");
			var biddingZoneName = EntsoeBiddingZone.byCode(biddingZoneCode) //
					.map(EntsoeBiddingZone::toString) //
					.orElse(biddingZoneCode);

			return "%s_%s".formatted(URI, biddingZoneName);
		}

		return URI;
	}

	/**
	 * Creates and configures a {@link BridgeHttp.Endpoint} for querying the ENTSO-E
	 * Transparency Platform API for day-ahead electricity prices.
	 *
	 * @param biddingZone the {@link EntsoeBiddingZone} to query (e.g. Germany,
	 *                    Austria)
	 * @param token       the ENTSO-E security token used for authentication
	 * @param fromDate    the start time of the query period (inclusive)
	 * @param toDate      the end time of the query period (exclusive)
	 * @return a configured {@link BridgeHttp.Endpoint}.
	 */
	public BridgeHttp.Endpoint createEndPoint(EntsoeBiddingZone biddingZone, String token, ZonedDateTime fromDate,
			ZonedDateTime toDate) {
		var urlBuilder = UrlBuilder.parse(URI) //
				.withQueryParam("securityToken", token) //
				.withQueryParam("documentType", "A44") //
				.withQueryParam("in_Domain", biddingZone.code) //
				.withQueryParam("out_Domain", biddingZone.code) //
				.withQueryParam("contract_MarketAgreement.type", "A01") //
				.withQueryParam("periodStart", fromDate.withZoneSameInstant(UTC) //
						.format(URL_DATE_FORMATTER)) //
				.withQueryParam("periodEnd", toDate.withZoneSameInstant(UTC) //
						.format(URL_DATE_FORMATTER));

		return BridgeHttp.create(urlBuilder.toEncodedString()).build();
	}

	/**
	 * Reads prices from entsoe response.
	 *
	 * @param response    Response from entsoe
	 * @param biddingZone The fetched bidding zone
	 * @param clock       Clock to get current time
	 * @return Parsed market price data
	 */
	public MarketPriceData handleResponse(HttpResponse<String> response, EntsoeBiddingZone biddingZone, Clock clock)
			throws OpenemsNamedException {
		final var responseText = response.data();
		final var xml = XmlParser.INSTANCE.parseXml(responseText);

		return this.readPriceData(xml, biddingZone, clock);
	}

	/**
	 * Reads prices from entsoe response content.
	 *
	 * @param xml         XML data from entsoe
	 * @param biddingZone The fetched bidding zone
	 * @param clock       Clock to get current time
	 * @return Parsed market price data
	 */
	public MarketPriceData readPriceData(XmlObject xml, EntsoeBiddingZone biddingZone, Clock clock) {
		this.throwOnError(xml);
		final var wholeTimeSpan = this.parseTimeSpan(xml.getChildObject("period.timeInterval"));

		var sequenceNodes = this.getSequenceNodes(xml, biddingZone);
		var sequences = Arrays.stream(sequenceNodes).map(this::parseSequence).toList();
		var currency = this.getCurrency(sequences);

		var bestResolution = sequences.stream().map(x -> x.values.getResolution()).min(Comparator.naturalOrder())
				.orElse(DurationUnit.ofMinutes(15));

		var valuesBuilder = TimeRangeValues.builder(wholeTimeSpan.getStartInclusive(), wholeTimeSpan.getEndExclusive(),
				bestResolution, Double.class);
		for (var sequence : sequences) {
			valuesBuilder.fillMissingDataFromData(sequence.values());
		}

		var creationTime = Instant.parse(xml.getChild("createdDateTime").getValue());
		return new MarketPriceData(valuesBuilder.build(), currency, creationTime, Instant.now(clock));
	}

	/**
	 * Calculates when the data should be fetched the next time from entsoe.
	 *
	 * @param response      Last response data from entsoe
	 * @param executionHour The hour of the day new data should be fetched
	 * @param clock         Clock instance for current time
	 * @return The delay until the next run
	 * @throws OpenemsNamedException Throws if XML can't be parsed
	 */
	public DelayTimeProvider.Delay calculateNextFetchDelay(HttpResponse<String> response, int executionHour,
			Clock clock) throws OpenemsNamedException {
		final var responseText = response.data();
		final var xml = XmlParser.INSTANCE.parseXml(responseText);

		return this.calculateNextFetchDelay(xml, executionHour, clock);
	}

	/**
	 * Calculates when the data should be fetched the next time from entsoe.
	 *
	 * @param xml           Last parsed xml response data from entsoe
	 * @param executionHour The hour of the day new data should be fetched
	 * @param clock         Clock instance for current time
	 * @return The delay until the next run
	 */
	public DelayTimeProvider.Delay calculateNextFetchDelay(XmlObject xml, int executionHour, Clock clock) {
		if (xml.hasChild("Reason")) {
			// We received an error text - don't calculate last received date
			return this.calculateNextFetchDelay((Instant) null, executionHour, clock);
		}

		var lastReceivedDate = Arrays.stream(this.getSequenceNodes(xml, null)) //
				.map(this::parseSequence) //
				.map(x -> x.values().getLastTime()) //
				.max(Comparator.naturalOrder()) //
				.orElse(null);

		return this.calculateNextFetchDelay(lastReceivedDate, executionHour, clock);
	}

	/**
	 * Calculates when the data should be fetched the next time from entsoe.
	 *
	 * @param clock            Clock instance for current time
	 * @param executionHour    The hour of the day new data should be fetched
	 * @param lastReceivedDate The last timepoint that is currently available
	 * @return The delay until the next run
	 */
	public DelayTimeProvider.Delay calculateNextFetchDelay(Instant lastReceivedDate, int executionHour, Clock clock) {
		final var now = Instant.now(clock);
		final var nextMidnight = now.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS);
		final var todayAtExecuteHour = now.truncatedTo(ChronoUnit.DAYS).plus(executionHour, ChronoUnit.HOURS);

		// Case 1: No prices at all or data doesn't extend to next day (incomplete)
		if (lastReceivedDate == null || lastReceivedDate.isBefore(nextMidnight)) {
			var nextRun = DateUtils.roundDownToQuarter(now.plus(1, ChronoUnit.HOURS));
			return DelayTimeProvider.Delay.of(Duration.between(now, nextRun).minusMinutes(1));
		}

		// Case 2: Complete data available, but before 14:00
		if (now.isBefore(todayAtExecuteHour)) {
			return DelayTimeProviderChain.fixedDelay(Duration.between(now, todayAtExecuteHour)) //
					.plusRandomDelay(60, ChronoUnit.SECONDS).getDelay();
		}

		// Case 3: After 14:00, schedule for next day at 14:00
		return DelayTimeProviderChain.fixedAtEveryFull(clock, DurationUnit.ofHours(24)) //
				.plusFixedAmount(Duration.ofHours(executionHour)) //
				.plusRandomDelay(60, ChronoUnit.SECONDS) //
				.getDelay();
	}

	private void throwOnError(XmlObject root) {
		if (root.hasChild("Reason")) {
			var reason = root.getChildObject("Reason");
			throw new RuntimeException("Entsoe returned an error (%s): %s".formatted(reason.getChild("code").getValue(),
					reason.getChild("text").getValue()));
		}
	}

	private TimeSpan parseTimeSpan(XmlObject xmlTimeInterval) {
		return TimeSpan.between(//
				Instant.from(XML_DATE_FORMATTER.parse(xmlTimeInterval.getChild("start").getValue())), //
				Instant.from(XML_DATE_FORMATTER.parse(xmlTimeInterval.getChild("end").getValue())) //
		);
	}

	private XmlObject[] getSequenceNodes(XmlObject xml, EntsoeBiddingZone biddingZone) {
		var xmlTimeSeries = xml.getChildObjects("TimeSeries");

		var xmlTimeSeriesByClassificationSequence = xmlTimeSeries.stream()
				.filter(x -> x.hasChild("classificationSequence_AttributeInstanceComponent.position"))
				.collect(Collectors.groupingBy(
						x -> x.getChild("classificationSequence_AttributeInstanceComponent.position").getValueAsInt()));

		if (xmlTimeSeriesByClassificationSequence.isEmpty()) {
			// Use all time series if no positions are provided
			return xmlTimeSeries.toArray(XmlObject[]::new);
		}

		if (biddingZone == EntsoeBiddingZone.GERMANY || biddingZone == EntsoeBiddingZone.AUSTRIA) {
			// Get prices from position 1 first. If it doesn't have a time slot, try
			// position 2
			return ArrayUtils.concatLists(//
					XmlObject[]::new, //
					xmlTimeSeriesByClassificationSequence.get(1), //
					xmlTimeSeriesByClassificationSequence.get(2) //
			);
		} else {
			// Get prices from position 2 first. If it doesn't have a time slot, try
			// position 1
			return ArrayUtils.concatLists(//
					XmlObject[]::new, //
					xmlTimeSeriesByClassificationSequence.get(2), //
					xmlTimeSeriesByClassificationSequence.get(1) //
			);
		}
	}

	private String getCurrency(List<Sequence> sequences) {
		var currencies = sequences.stream() //
				.map(Sequence::currency) //
				.filter(x -> !StringUtils.isNullOrBlank(x)) //
				.distinct() //
				.toArray(String[]::new);

		if (currencies.length == 0) {
			throw new RuntimeException("Missing currency data in sequences");
		}
		if (currencies.length > 1) {
			throw new RuntimeException("Received multiple currencies (" + String.join(", ", currencies) + ")");
		}

		return currencies[0];
	}

	private Sequence parseSequence(XmlObject xml) {
		var xmlPeriod = xml.getChildObject("Period");
		var timeSpan = this.parseTimeSpan(xmlPeriod.getChildObject("timeInterval"));
		var resolution = Duration.parse(xmlPeriod.getChild("resolution").getValue()); // "PT15M" or "PT60M"
		var currency = xml.getChild("currency_Unit.name").getValue();

		var priceUnit = xml.getChild("price_Measure_Unit.name").getValue();
		if (!priceUnit.equals("MWH")) {
			throw new RuntimeException("Received sequence with unexpected Price Unit. Expected MWH, got " + priceUnit);
		}

		var valuesBuilder = TimeRangeValues.builder(timeSpan.getStartInclusive(), timeSpan.getEndExclusive(),
				DurationUnit.of(resolution), Double.class);
		var xmlPoints = xmlPeriod.getChildObjects("Point").toArray(XmlObject[]::new);

		for (var xmlPoint : xmlPoints) {
			var position = xmlPoint.getChild("position").getValueAsInt();
			var amount = xmlPoint.getChild("price.amount").getValueAsDouble();

			valuesBuilder.setByPosition(position - 1, amount);
		}

		var values = valuesBuilder.fillMissingDataWithPreviousData().build();
		return new Sequence(currency, values);
	}

	protected record Sequence(String currency, TimeRangeValues<Double> values) {
	}

}
