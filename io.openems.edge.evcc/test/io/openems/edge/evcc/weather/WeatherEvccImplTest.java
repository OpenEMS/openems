package io.openems.edge.evcc.weather;

import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.cycleSubscriber;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.dummyBridgeHttpExecutor;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.dummyEndpointFetcher;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.ofBridgeImpl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.predictor.api.prediction.LogVerbosity;
import io.openems.edge.weather.api.WeatherSnapshot;

public class WeatherEvccImplTest {

	@Test
	public void testWeatherEvccParsesValues() throws Exception {
		TimeLeapClock clock = new TimeLeapClock(ZoneId.of("UTC"));

		var sut = new WeatherEvccImpl();

		var executor = dummyBridgeHttpExecutor();
		var endpointFetcher = dummyEndpointFetcher();
		endpointFetcher.addEndpointHandler(endpoint -> {
			if (endpoint.url().equals("http://evcc:7070/api/tariff/solar")) {
				return HttpResponse.ok(this.generateWeatherJson(clock, 15));
			}
			throw HttpError.ResponseError.notFound();
		});

		var factory = ofBridgeImpl(() -> cycleSubscriber(), () -> endpointFetcher, () -> executor);

		new ComponentTest(sut).addReference("httpBridgeFactory", factory)
				.addReference("componentManager", new DummyComponentManager(clock))
				.activate(MyConfig.create().setId("weatherEvcc0").setApiUrl("http://evcc:7070/api/tariff/solar")
						.setLogVerbosity(LogVerbosity.NONE).build())

				.next(new TestCase("Initial fetch and verify values").timeleap(clock, 10, ChronoUnit.SECONDS)
						.onAfterProcessImage(() -> {
							executor.update();

							assertTrue("Forecast should not be empty after polling",
									sut.getForecastService().getWeatherForecast().asArray().length > 0);

							ZonedDateTime firstKey = sut.getForecastService().getWeatherForecast().getFirstTime();
							WeatherSnapshot firstSnapshot = sut.getForecastService().getWeatherForecast()
									.getAt(firstKey);
							assertEquals(this.getGhiValue(firstKey), firstSnapshot.globalHorizontalIrradiance(), 0);

							// check a value 30 minutes later
							ZonedDateTime midKey = firstKey.plusMinutes(30);
							WeatherSnapshot midSnapshot = sut.getForecastService().getWeatherForecast().getAt(midKey);
							assertEquals(this.getGhiValue(midKey), midSnapshot.globalHorizontalIrradiance(), 0);
						}))

				.next(new TestCase("Next quarter-hour fetch and verify").timeleap(clock, 15, ChronoUnit.MINUTES)
						.onAfterProcessImage(() -> {
							executor.update();
							assertTrue("Forecast should still contain entries",
									sut.getForecastService().getWeatherForecast().asArray().length > 0);
							ZonedDateTime k = sut.getForecastService().getWeatherForecast().getFirstTime();
							assertEquals(this.getGhiValue(k),
									sut.getForecastService().getWeatherForecast().getAt(k).globalHorizontalIrradiance(),
									0);
						}))

				.deactivate();
	}

	private String generateWeatherJson(TimeLeapClock clock, int intervalMinutes) {
		ZonedDateTime now = ZonedDateTime.now(clock).withHour(12).withMinute(0).withSecond(0).withNano(0);
		ZonedDateTime end = now.plusHours(2);
		StringBuilder json = new StringBuilder();
		json.append("{ \"rates\": [");
		for (ZonedDateTime t = now; t.isBefore(end); t = t.plusMinutes(intervalMinutes)) {
			ZonedDateTime next = t.plusMinutes(intervalMinutes);
			json.append(String.format("{ \"start\": \"%s\", \"end\": \"%s\", \"value\": %d },", t.toString(),
					next.toString(), this.getGhiValue(t)));
		}
		json.setLength(json.length() - 1);
		json.append("]}");
		return json.toString();
	}

	private int getGhiValue(ZonedDateTime time) {
		int hour = time.getHour();
		return ((hour >= 6 && hour <= 18) ? (hour - 6) * 100 : hour) + 1;
	}
}
