package io.openems.edge.weather.openmeteo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.google.gson.JsonElement;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.edge.common.meta.types.Coordinates;
import io.openems.edge.weather.api.QuarterlyWeatherSnapshot;

@RunWith(MockitoJUnitRunner.class)
public class HistoricalWeatherServiceTest {

	private static final String DUMMY_API_KEY = "dummy-api-key";

	@Mock
	private BridgeHttp httpBridge;

	@Mock
	private WeatherDataParser weatherDataParser;

	private HistoricalWeatherService historicalWeatherService;

	@Before
	public void setUp() {
		this.historicalWeatherService = new HistoricalWeatherService(//
				this.httpBridge, //
				DUMMY_API_KEY, //
				this.weatherDataParser);
	}

	@Test
	public void testGetWeatherData_ShouldCompleteSuccessfully_WhenValidCoordinates() {
		final var coordinates = new Coordinates(52.52, 13.405);
		final var dateFrom = LocalDate.of(2025, 8, 17);
		final var dateTo = LocalDate.of(2025, 8, 18);
		final var targetZone = ZoneId.of("Europe/Berlin");

		var jsonRespone = new JsonObject();
		var responseZone = ZoneId.of("UTC");
		jsonRespone.addProperty(HistoricalQueryParams.TIMEZONE, responseZone.toString());
		jsonRespone.add(QuarterlyWeatherVariables.JSON_KEY, new JsonObject());

		var httpResponse = HttpResponse.<JsonElement>ok(jsonRespone);
		when(this.httpBridge.getJson(anyString())).thenReturn(CompletableFuture.completedFuture(httpResponse));

		var historicalWeatherData = List.of(
				new QuarterlyWeatherSnapshot(ZonedDateTime.of(2025, 8, 17, 0, 0, 0, 0, ZoneId.of("UTC")), 1.0, 2.0));
		when(this.weatherDataParser.parseQuarterly(any(), any(), any())).thenReturn(historicalWeatherData);

		var result = this.historicalWeatherService.getWeatherData(coordinates, dateFrom, dateTo, targetZone);

		assertEquals(historicalWeatherData, result.join());
		verify(this.weatherDataParser).parseQuarterly(//
				eq(jsonRespone.getAsJsonObject(QuarterlyWeatherVariables.JSON_KEY)), //
				eq(responseZone), //
				eq(targetZone));
	}

	@Test
	public void testGetWeatherData_ShouldReturnFailedFuture_WhenNoCoordinates() {
		var dateFrom = LocalDate.of(2025, 8, 17);
		var dateTo = LocalDate.of(2025, 8, 18);
		var zone = ZoneId.of("Europe/Berlin");

		var result = this.historicalWeatherService.getWeatherData(null, dateFrom, dateTo, zone);

		var exception = assertThrows(CompletionException.class, result::join);
		assertTrue(exception.getCause() instanceof OpenemsException);
		assertEquals("Can't get historical weather data, coordinates are missing", exception.getCause().getMessage());
	}

	@Test
	public void testBuildHistoricalUrl_ShouldBuildCorrectUrl_WhenApiKey() {
		var coordinates = new Coordinates(52.52, 13.405);
		var dateFrom = LocalDate.of(2025, 8, 17);
		var dateTo = LocalDate.of(2025, 8, 18);
		var zone = ZoneId.of("Europe/Berlin");
		String url = this.historicalWeatherService.buildHistoricalUrl(coordinates, dateFrom, dateTo, zone);

		assertTrue(url.contains("https://customer-historical-forecast-api.open-meteo.com/v1/forecast?"));
		assertTrue(url.contains("apikey=" + DUMMY_API_KEY));
		assertTrue(url.contains("latitude=" + coordinates.latitude()));
		assertTrue(url.contains("longitude=" + coordinates.longitude()));
		assertTrue(url.contains("timezone=" + zone.toString().replace("/", "%2F")));
		assertTrue(url.contains("start_date=" + dateFrom.toString()));
		assertTrue(url.contains("end_date=" + dateTo.toString()));
		assertTrue(url.contains("minutely_15=" + String.join("%2C", QuarterlyWeatherVariables.ALL)));
	}

	@Test
	public void testBuildHistoricalUrl_ShouldBuildCorrectUrl_WhenNoApiKey() {
		this.historicalWeatherService = new HistoricalWeatherService(//
				this.httpBridge, //
				null, //
				this.weatherDataParser);

		var coordinates = new Coordinates(52.52, 13.405);
		var dateFrom = LocalDate.of(2025, 8, 17);
		var dateTo = LocalDate.of(2025, 8, 18);
		var zone = ZoneId.of("Europe/Berlin");
		String url = this.historicalWeatherService.buildHistoricalUrl(coordinates, dateFrom, dateTo, zone);

		assertTrue(url.contains("https://historical-forecast-api.open-meteo.com/v1/forecast?"));
		assertFalse(url.contains("apikey="));
		assertTrue(url.contains("latitude=" + coordinates.latitude()));
		assertTrue(url.contains("longitude=" + coordinates.longitude()));
		assertTrue(url.contains("timezone=" + zone.toString().replace("/", "%2F")));
		assertTrue(url.contains("start_date=" + dateFrom.toString()));
		assertTrue(url.contains("end_date=" + dateTo.toString()));
		assertTrue(url.contains("minutely_15=" + String.join("%2C", QuarterlyWeatherVariables.ALL)));
	}
}
