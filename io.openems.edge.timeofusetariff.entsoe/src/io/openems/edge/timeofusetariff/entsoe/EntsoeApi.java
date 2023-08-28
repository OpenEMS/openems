package io.openems.edge.timeofusetariff.entsoe;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class EntsoeApi {

	public static final DateTimeFormatter FORMATTER_MINUTES = DateTimeFormatter.ofPattern("u-MM-dd'T'HH:mmX");
	public static final DateTimeFormatter FORMATTER_SECONDS = DateTimeFormatter.ofPattern("u-MM-dd'T'HH:mm:ssX");
	public static final ZoneId UTC = ZoneId.of("UTC");
	public static final String URI = "https://web-api.tp.entsoe.eu/api";

	/**
	 * Queries the ENTSO-E API for day-ahead prices.
	 * 
	 * @param token    the Security Token
	 * @param areaCode Area EIC code; see
	 *                 https://transparency.entsoe.eu/content/static_content/Static%20content/web%20api/Guide.html#_areas
	 * @param fromDate the From-Date
	 * @param toDate   the To-Date
	 * @return The response string.
	 * @throws IOException on error
	 */
	protected static String query(String token, String areaCode, ZonedDateTime fromDate, ZonedDateTime toDate)
			throws IOException {
		var client = new OkHttpClient();
		var request = new Request.Builder() //
				.url(URI) //
				.header("SECURITY_TOKEN", token) //
				.post(RequestBody
						// ProcessType A01 -> Day ahead
						// DocumentType A44 -> Price Document
						.create("""
								<StatusRequest_MarketDocument xmlns="urn:iec62325.351:tc57wg16:451-5:statusrequestdocument:4:0">
								   <mRID>SampleCallToRestfulApi</mRID>
								   <type>A59</type>
								   <sender_MarketParticipant.mRID codingScheme="A01">10X1001A1001A450</sender_MarketParticipant.mRID>
								   <sender_MarketParticipant.marketRole.type>A07</sender_MarketParticipant.marketRole.type>
								   <receiver_MarketParticipant.mRID codingScheme="A01">10X1001A1001A450</receiver_MarketParticipant.mRID>
								   <receiver_MarketParticipant.marketRole.type>A32</receiver_MarketParticipant.marketRole.type>
								   <createdDateTime>%s</createdDateTime>
								   <AttributeInstanceComponent>
								      <attribute>DocumentType</attribute>
								      <attributeValue>A44</attributeValue>
								   </AttributeInstanceComponent>
								   <AttributeInstanceComponent>
								      <attribute>In_Domain</attribute>
								      <attributeValue>%s</attributeValue>
								   </AttributeInstanceComponent>
								   <AttributeInstanceComponent>
								      <attribute>Out_Domain</attribute>
								      <attributeValue>%s</attributeValue>
								   </AttributeInstanceComponent>
								   <AttributeInstanceComponent>
								      <attribute>TimeInterval</attribute>
								      <attributeValue>%s/%s</attributeValue>
								   </AttributeInstanceComponent>
								</StatusRequest_MarketDocument>"""
								.formatted(//
										FORMATTER_SECONDS.format(ZonedDateTime.now(UTC)), //
										areaCode, areaCode, //
										FORMATTER_MINUTES.format(fromDate.withZoneSameInstant(UTC)), //
										FORMATTER_MINUTES.format(toDate.withZoneSameInstant(UTC)) //
								), MediaType.parse("application/xml"))) //
				.build();

		try (var response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unable to get response from ENTSO-E API: " + response);
			}

			return response.body().string();
		}
	}
}
