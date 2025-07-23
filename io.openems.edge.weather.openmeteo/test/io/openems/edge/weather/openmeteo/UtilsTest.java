package io.openems.edge.weather.openmeteo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import io.openems.edge.weather.api.WeatherData;
import io.openems.edge.weather.api.WeatherSnapshot;

public class UtilsTest {

	private static final double DELTA = 1e-6;
	private static final String[] WEATHER_VARIABLES = { //
			"shortwave_radiation", //
			"direct_normal_irradiance", //
			"temperature_2m", //
			"weather_code" //
	};

	private JsonElement testJsonElement;

	@Before
	public void setUp() {
		this.testJsonElement = JsonParser.parseString(JSON_RESPONSE);
	}

	@Test
	public void testParseWeatherDataFromJson() {
		WeatherData result = Utils.parseWeatherDataFromJson(this.testJsonElement, WEATHER_VARIABLES,
				ZoneId.of("Europe/Berlin"));

		assertEquals(2, result.toMap().size());

		ZonedDateTime timestamp1 = ZonedDateTime.parse("2025-03-23T00:00Z", DateTimeFormatter.ISO_DATE_TIME)
				.withZoneSameInstant(ZoneId.of("Europe/Berlin"));
		WeatherSnapshot snapshot1 = result.getAt(timestamp1);
		assertNotNull(snapshot1);
		assertEquals(8.6, snapshot1.temperature(), DELTA);
		assertEquals(0.0, snapshot1.globalHorizontalIrradiance(), DELTA);
		assertEquals(100.0, snapshot1.directNormalIrradiance(), DELTA);
		assertEquals(1, snapshot1.weatherCode());

		ZonedDateTime timestamp2 = ZonedDateTime.parse("2025-03-23T00:15Z", DateTimeFormatter.ISO_DATE_TIME)
				.withZoneSameInstant(ZoneId.of("Europe/Berlin"));
		WeatherSnapshot snapshot2 = result.getAt(timestamp2);
		assertNotNull(snapshot2);
		assertEquals(8.4, snapshot2.temperature(), DELTA);
		assertEquals(2.0, snapshot2.globalHorizontalIrradiance(), DELTA);
		assertEquals(110.0, snapshot2.directNormalIrradiance(), DELTA);
		assertEquals(2, snapshot2.weatherCode());
	}

	@Test
	public void testParseWeatherDataFromJson_CorrectTimeZoneConversion() {
		WeatherData result = Utils.parseWeatherDataFromJson(this.testJsonElement, WEATHER_VARIABLES,
				ZoneId.of("Europe/Moscow"));

		assertEquals(ZonedDateTime.parse("2025-03-23T00:00Z", DateTimeFormatter.ISO_DATE_TIME)
				.withZoneSameInstant(ZoneId.of("Europe/Moscow")), result.getFirstTime());
	}

	private static final String JSON_RESPONSE = """
			{
			  "timezone": "GMT",
			  "minutely_15": {
			    "time": [
			      "2025-03-23T00:00",
			      "2025-03-23T00:15"
			    ],
			    "temperature_2m": [
			      8.6,
			      8.4
			    ],
			    "shortwave_radiation": [
			      0.0,
			      2.0
			    ],
			    "direct_normal_irradiance": [
			      100.0,
			      110.0
			    ],
			    "weather_code": [
			      1,
			      2
			    ]
			  }
			}
			""".stripIndent();
}
