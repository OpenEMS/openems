package io.openems.edge.weather.openmeteo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

public class DefaultWeatherDataParserTest {

	private WeatherDataParser weatherDataParser;
	private JsonObject testJson;

	@Before
	public void setUp() throws Exception {
		this.weatherDataParser = new DefaultWeatherDataParser();
		this.testJson = JsonUtils.parse(JSON_RESPONSE).getAsJsonObject();
	}

	@Test
	public void testParseQuarterly_ShouldParseAllFieldsCorrectly() {
		var apiBlock = this.testJson.getAsJsonObject("minutely_15");
		var responseOffset = ZoneOffset.ofTotalSeconds(this.testJson.get("utc_offset_seconds").getAsInt());
		var targetZone = ZoneId.of("Europe/Berlin");
		final var result = this.weatherDataParser.parseQuarterly(//
				apiBlock, //
				responseOffset, //
				targetZone);

		assertEquals(2, result.size());

		var snapshot1 = result.get(0);
		var timestamp1 = ZonedDateTime.parse("2025-03-23T00:00Z", DateTimeFormatter.ISO_DATE_TIME)
				.withZoneSameInstant(ZoneId.of("Europe/Berlin"));
		assertEquals(timestamp1, snapshot1.datetime());
		assertEquals(0.0, snapshot1.globalHorizontalIrradiance(), 0.);
		assertEquals(100.0, snapshot1.directNormalIrradiance(), 0.);

		var snapshot2 = result.get(1);
		var timestamp2 = ZonedDateTime.parse("2025-03-23T00:15Z", DateTimeFormatter.ISO_DATE_TIME)
				.withZoneSameInstant(ZoneId.of("Europe/Berlin"));
		assertEquals(timestamp2, snapshot2.datetime());
		assertEquals(2.0, snapshot2.globalHorizontalIrradiance(), 0.);
		assertEquals(110.0, snapshot2.directNormalIrradiance(), 0.);
	}

	@Test
	public void testParseQuarterly_ShouldConvertTimeZoneCorrectly() {
		var apiBlock = this.testJson.getAsJsonObject("minutely_15");
		var responseOffset = ZoneOffset.ofTotalSeconds(this.testJson.get("utc_offset_seconds").getAsInt());
		var targetZone = ZoneId.of("Europe/Moscow");
		final var result = this.weatherDataParser.parseQuarterly(//
				apiBlock, //
				responseOffset, //
				targetZone);

		assertEquals(ZonedDateTime.parse("2025-03-23T00:00Z", DateTimeFormatter.ISO_DATE_TIME)
				.withZoneSameInstant(ZoneId.of("Europe/Moscow")), result.get(0).datetime());
	}

	@Test
	public void testParseQuarterly_ShouldConvertTimeZoneCorrectly_WhenDaylightSavingTimeChange() throws Exception {
		var apiBlock = JsonUtils.parse(JSON_RESPONSE_DST_TRANSITION)//
				.getAsJsonObject()//
				.getAsJsonObject("minutely_15");
		var responseOffset = ZoneOffset.ofTotalSeconds(this.testJson.get("utc_offset_seconds").getAsInt());
		var targetZone = ZoneId.of("Europe/Berlin");
		final var result = this.weatherDataParser.parseQuarterly(//
				apiBlock, //
				responseOffset, //
				targetZone);

		var expectedTimes = new ZonedDateTime[] {
				LocalDateTime.parse("2025-03-29T23:45").atOffset(responseOffset).toZonedDateTime()
						.withZoneSameInstant(targetZone),
				LocalDateTime.parse("2025-03-30T00:00").atOffset(responseOffset).toZonedDateTime()
						.withZoneSameInstant(targetZone),
				LocalDateTime.parse("2025-03-30T01:00").atOffset(responseOffset).toZonedDateTime()
						.withZoneSameInstant(targetZone),
				LocalDateTime.parse("2025-03-30T02:00").atOffset(responseOffset).toZonedDateTime()
						.withZoneSameInstant(targetZone),
				LocalDateTime.parse("2025-03-30T03:00").atOffset(responseOffset).toZonedDateTime()
						.withZoneSameInstant(targetZone) };

		for (int i = 0; i < expectedTimes.length; i++) {
			assertEquals(expectedTimes[i], result.get(i).datetime());
		}
	}

	@Test
	public void testParseHourly_ShouldParseAllFieldsCorrectly() {
		var apiBlock = this.testJson.getAsJsonObject("hourly");
		var responseOffset = ZoneOffset.ofTotalSeconds(this.testJson.get("utc_offset_seconds").getAsInt());
		var targetZone = ZoneId.of("Europe/Berlin");
		final var result = this.weatherDataParser.parseHourly(//
				apiBlock, //
				responseOffset, //
				targetZone);

		assertEquals(2, result.size());

		var snapshot1 = result.get(0);
		var timestamp1 = ZonedDateTime.parse("2025-03-23T00:00Z", DateTimeFormatter.ISO_DATE_TIME)
				.withZoneSameInstant(ZoneId.of("Europe/Berlin"));
		assertEquals(timestamp1, snapshot1.datetime());
		assertEquals(3, snapshot1.weatherCode());
		assertEquals(14.0, snapshot1.temperature(), 0.);
		assertFalse(snapshot1.isDay());

		var snapshot2 = result.get(1);
		var timestamp2 = ZonedDateTime.parse("2025-03-23T00:15Z", DateTimeFormatter.ISO_DATE_TIME)
				.withZoneSameInstant(ZoneId.of("Europe/Berlin"));
		assertEquals(timestamp2, snapshot2.datetime());
		assertEquals(0, snapshot2.weatherCode());
		assertEquals(16.9, snapshot2.temperature(), 0.);
		assertTrue(snapshot2.isDay());
	}

	@Test
	public void testParseHourly_ShouldConvertTimeZoneCorrectly() {
		var apiBlock = this.testJson.getAsJsonObject("hourly");
		var responseOffset = ZoneOffset.ofTotalSeconds(this.testJson.get("utc_offset_seconds").getAsInt());
		var targetZone = ZoneId.of("Europe/Moscow");
		final var result = this.weatherDataParser.parseHourly(//
				apiBlock, //
				responseOffset, //
				targetZone);

		assertEquals(ZonedDateTime.parse("2025-03-23T00:00Z", DateTimeFormatter.ISO_DATE_TIME)
				.withZoneSameInstant(ZoneId.of("Europe/Moscow")), result.get(0).datetime());
	}

	@Test
	public void testParseHourly_ShouldConvertTimeZoneCorrectly_WhenDaylightSavingTimeChange() throws Exception {
		var apiBlock = JsonUtils.parse(JSON_RESPONSE_DST_TRANSITION)//
				.getAsJsonObject()//
				.getAsJsonObject("hourly");
		var responseOffset = ZoneOffset.ofTotalSeconds(this.testJson.get("utc_offset_seconds").getAsInt());
		var targetZone = ZoneId.of("Europe/Berlin");
		final var result = this.weatherDataParser.parseHourly(//
				apiBlock, //
				responseOffset, //
				targetZone);

		var expectedTimes = new ZonedDateTime[] {
				LocalDateTime.parse("2025-03-29T23:00").atOffset(responseOffset).toZonedDateTime()
						.withZoneSameInstant(targetZone),
				LocalDateTime.parse("2025-03-30T00:00").atOffset(responseOffset).toZonedDateTime()
						.withZoneSameInstant(targetZone),
				LocalDateTime.parse("2025-03-30T01:00").atOffset(responseOffset).toZonedDateTime()
						.withZoneSameInstant(targetZone),
				LocalDateTime.parse("2025-03-30T02:00").atOffset(responseOffset).toZonedDateTime()
						.withZoneSameInstant(targetZone),
				LocalDateTime.parse("2025-03-30T03:00").atOffset(responseOffset).toZonedDateTime()
						.withZoneSameInstant(targetZone) };

		for (int i = 0; i < expectedTimes.length; i++) {
			assertEquals(expectedTimes[i], result.get(i).datetime());
		}
	}

	@Test
	public void testParseDaily_ShouldParseAllFieldsCorrectly() {
		var apiBlock = this.testJson.getAsJsonObject("daily");
		final var result = this.weatherDataParser.parseDaily(apiBlock);

		assertEquals(2, result.size());

		var snapshot1 = result.get(0);
		var timestamp1 = LocalDate.parse("2025-03-23");
		assertEquals(timestamp1, snapshot1.date());
		assertEquals(3, snapshot1.weatherCode());
		assertEquals(14.0, snapshot1.minTemperature(), 0.);
		assertEquals(18.2, snapshot1.maxTemperature(), 0.);
		assertEquals(48645.81, snapshot1.sunshineDuration(), 0.);

		var snapshot2 = result.get(1);
		var timestamp2 = LocalDate.parse("2025-03-24");
		assertEquals(timestamp2, snapshot2.date());
		assertEquals(0, snapshot2.weatherCode());
		assertEquals(16.9, snapshot2.minTemperature(), 0.);
		assertEquals(22.4, snapshot2.maxTemperature(), 0.);
		assertEquals(47899.74, snapshot2.sunshineDuration(), 0.);
	}

	private static final String JSON_RESPONSE = """
			{
			  "timezone": "GMT",
			  "utc_offset_seconds": 0,
			  "minutely_15": {
			    "time": [
			      "2025-03-23T00:00",
			      "2025-03-23T00:15"
			    ],
			    "shortwave_radiation": [
			      0.0,
			      2.0
			    ],
			    "direct_normal_irradiance": [
			      100.0,
			      110.0
			    ]
			  },
			  "hourly": {
			    "time": [
			      "2025-03-23T00:00",
			      "2025-03-23T00:15"
			    ],
			    "weather_code": [
			      3,
			      0
			    ],
			    "temperature_2m": [
			      14.0,
			      16.9
			    ],
			    "is_day": [
			      0,
			      1
			    ]
			  },
			  "daily": {
			    "time": [
			      "2025-03-23",
			      "2025-03-24"
			    ],
			    "weather_code": [
			      3,
			      0
			    ],
			    "temperature_2m_min": [
			      14.0,
			      16.9
			    ],
			    "temperature_2m_max": [
			      18.2,
			      22.4
			    ],
			    "sunshine_duration": [
			      48645.81,
			      47899.74
			    ]
			  }
			}
			""".stripIndent();

	private static final String JSON_RESPONSE_DST_TRANSITION = """
			{
			  "timezone": "Europe/Berlin",
			  "utc_offset_seconds": 7200,
			  "minutely_15": {
			    "time": [
			      "2025-03-29T23:45",
			      "2025-03-30T00:00",
			      "2025-03-30T01:00",
			      "2025-03-30T02:00",
			      "2025-03-30T03:00"
			    ],
			    "shortwave_radiation": [
			      0.0, 1.0, 2.0, 3.0, 4.0
			    ],
			    "direct_normal_irradiance": [
			      100.0, 105.0, 110.0, 115.0, 120.0
			    ]
			  },
			  "hourly": {
			    "time": [
			      "2025-03-29T23:00",
			      "2025-03-30T00:00",
			      "2025-03-30T01:00",
			      "2025-03-30T02:00",
			      "2025-03-30T03:00"
			    ],
			    "weather_code": [
			      3, 3, 0, 0, 1
			    ],
			    "temperature_2m": [
			      10.0, 9.5, 9.0, 8.5, 8.0
			    ],
			    "is_day": [
			      0, 0, 0, 0, 1
			    ]
			  }
			}
			""".stripIndent();
}
