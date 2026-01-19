package io.openems.edge.timeofusetariff.entsoe;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.UrlBuilder;

public class EntsoeApi {

	public static final DateTimeFormatter URL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
	public static final ZoneId UTC = ZoneId.of("UTC");
	public static final String URI = "https://web-api.tp.entsoe.eu/api";

	/**
	 * Creates and configures a {@link BridgeHttp.EndPoint} for querying the ENTSO-E
	 * Transparency Platform API for day-ahead electricity prices.
	 * 
	 * @param biddingZone the {@link BiddingZone} to query (e.g. Germany, Austria)
	 * @param token       the ENTSO-E security token used for authentication
	 * @param fromDate    the start time of the query period (inclusive)
	 * @param toDate      the end time of the query period (exclusive)
	 * @return a configured {@link BridgeHttp.EndPoint}.
	 */
	public static Endpoint createEndPoint(BiddingZone biddingZone, String token, ZonedDateTime fromDate,
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
}
