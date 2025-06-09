package io.openems.edge.evcc.solartariff;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.cycleSubscriber;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.dummyBridgeHttpExecutor;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.dummyEndpointFetcher;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.ofBridgeImpl;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.api.UrlBuilder;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.predictor.api.prediction.LogVerbosity;
import io.openems.edge.predictor.api.prediction.Prediction;

public class PredictorSolarTariffEvccImplTest {

	final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

	@Test
	public void testSolarTariffPrediction() throws Exception {
		final var clock = createDummyClock();
		final var sut = new PredictorSolarTariffEvccImpl();
		final var api = new PredictorSolarTariffEvccApi(clock);

		final var url = "http://evcc:7070/api/tariff/solar";
		final var endpointFetcher = dummyEndpointFetcher();
		endpointFetcher.addEndpointHandler(endpoint -> {

			if (endpoint.url().equals(UrlBuilder.parse(url).toEncodedString())) {
				return HttpResponse.ok(this.generateDynamicJson(clock, 60));
			}

			throw HttpError.ResponseError.notFound();
		});

		final var executor = dummyBridgeHttpExecutor();

		final var factory = ofBridgeImpl(//
				() -> cycleSubscriber(), //
				() -> endpointFetcher, //
				() -> executor //
		);
		final var urlFail = "http://evcc:7070/api/tarif/solar";

		final ZonedDateTime localCurrentHour = ZonedDateTime.now(clock).withSecond(0).withNano(0).withMinute(0);
		ZonedDateTime currentHour = localCurrentHour.withZoneSameInstant(ZoneId.of("UTC"));

		new ComponentTest(sut).addReference("httpBridgeFactory", factory)
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(//
						MyConfig.create() //
								.setId("predictor0") //
								.setUrl(urlFail) //
								.setLogVerbosity(LogVerbosity.REQUESTED_PREDICTIONS) //
								.build())

				// Case: API not found
				.next(new TestCase("API Not Found") //
						.timeleap(clock, 10, ChronoUnit.SECONDS) //
						.onAfterProcessImage(() -> {
							executor.update();
							assertEquals(Prediction.EMPTY_PREDICTION,
									sut.createNewPrediction(sut.getChannelAddresses()[0]));
						}))
				.deactivate();

		new ComponentTest(sut).addReference("httpBridgeFactory", factory)
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(//
						MyConfig.create() //
								.setId("predictor0") //
								.setUrl(url) //
								.setLogVerbosity(LogVerbosity.REQUESTED_PREDICTIONS) //
								.build())

				// Case: API success
				.next(new TestCase("API success") //
						.timeleap(clock, 10, ChronoUnit.SECONDS) //
						.onAfterProcessImage(() -> {
							int expectedFirstValue = this.getSolarPredictionValue(currentHour);
							executor.update();
							Prediction prediction = sut.createNewPrediction(sut.getChannelAddresses()[0]);
							assertNotEquals(Prediction.EMPTY_PREDICTION, prediction);
							assertEquals(expectedFirstValue, prediction.getFirst().intValue());
						}))
				.deactivate();

		// API response manual
		new ComponentTest(sut)
				// Case: response handling (manual)
				.next(new TestCase("API response") //
						.onBeforeProcessImage(() -> {
							// simulate response
							String jsonResponse = this.generateDynamicJson(clock, 60);
							api.handleResponse(HttpResponse.ok(jsonResponse));
							assertNotEquals(Prediction.EMPTY_PREDICTION, api.getPrediction());
						}))//

				// Case: simulated API processing (60 minutes)
				.next(new TestCase("Successful API response") //
						.timeleap(clock, 6, ChronoUnit.HOURS) //
						.onBeforeProcessImage(() -> {
							assertEquals(Prediction.EMPTY_PREDICTION, api.getPrediction());
							assertEquals(null, api.getCurrentPrediction());

							String jsonResponse = this.generateDynamicJson(clock, 60);
							Prediction prediction = api.parsePrediction(jsonResponse);
							assertNotEquals(Prediction.EMPTY_PREDICTION, prediction);

							Integer[] predictions = prediction.asArray();

							// check object count
							assertEquals(8, predictions.length);

							// check first timestamp (in UTC, respecting time leap)
							assertEquals(currentHour.plusHours(6), prediction.getFirstTime());

							int expectedFirstValue = this.getSolarPredictionValue(currentHour.plusHours(6));
							// check quaterly values
							assertEquals(expectedFirstValue, predictions[0].intValue());
							assertEquals(expectedFirstValue, predictions[1].intValue());
							assertEquals(expectedFirstValue, predictions[2].intValue());
							assertEquals(expectedFirstValue, predictions[3].intValue());

							// check first value of next hour
							int expectedSecondHourValue = this.getSolarPredictionValue(currentHour.plusHours(7));
							assertEquals(expectedSecondHourValue, predictions[4].intValue());
						})) //

				// Case: simulated API processing (30 minutes)
				.next(new TestCase("Successful API response") //
						.onBeforeProcessImage(() -> {
							String jsonResponse = this.generateDynamicJson(clock, 30);
							Prediction prediction = api.parsePrediction(jsonResponse);
							assertNotEquals(Prediction.EMPTY_PREDICTION, prediction);

							Integer[] predictions = prediction.asArray();

							// check object count
							assertEquals(8, predictions.length);

							// check first timestamp (in UTC)
							assertEquals(currentHour.plusHours(6), prediction.getFirstTime());

							int expectedFirstValue = this.getSolarPredictionValue(currentHour.plusHours(6));
							// check quaterly values
							assertEquals(expectedFirstValue, predictions[0].intValue());
							assertEquals(expectedFirstValue, predictions[1].intValue());
							assertEquals(expectedFirstValue, predictions[2].intValue());
							assertEquals(expectedFirstValue, predictions[3].intValue());

							// check first value of next hour
							int expectedSecondHourValue = this.getSolarPredictionValue(currentHour.plusHours(7));
							assertEquals(expectedSecondHourValue, predictions[4].intValue());
						})) //

				// Case: simulated API processing (15 minutes)
				.next(new TestCase("Successful API response") //
						.onBeforeProcessImage(() -> {
							String jsonResponse = this.generateDynamicJson(clock, 15);
							Prediction prediction = api.parsePrediction(jsonResponse);
							assertNotEquals(Prediction.EMPTY_PREDICTION, prediction);

							Integer[] predictions = prediction.asArray();

							// check object count
							assertEquals(8, predictions.length);

							// check first timestamp (in UTC)
							assertEquals(currentHour.plusHours(6), prediction.getFirstTime());

							int expectedFirstValue = this.getSolarPredictionValue(currentHour.plusHours(6));
							// check quaterly values
							assertEquals(expectedFirstValue, predictions[0].intValue());
							assertEquals(expectedFirstValue, predictions[1].intValue());
							assertEquals(expectedFirstValue, predictions[2].intValue());
							assertEquals(expectedFirstValue, predictions[3].intValue());

							// check first value of next hour
							int expectedSecondHourValue = this.getSolarPredictionValue(currentHour.plusHours(7));
							assertEquals(expectedSecondHourValue, predictions[4].intValue());
						})) //

				// Case: simulated API processing (old data, caching)
				.next(new TestCase("Successful API response") //
						.timeleap(clock, -6, ChronoUnit.HOURS) //
						.onBeforeProcessImage(() -> {

							String jsonResponsePast = String.format(
									"""
											    { "result": { "rates": [{ "start": "%s", "end": "%s", "value": 100 },{ "start": "%s", "end": "%s", "value": 200 }]}}
											""",
									currentHour.minusMinutes(120), currentHour.minusMinutes(60),
									currentHour.minusMinutes(60), currentHour);

							Prediction prediction = api.parsePrediction(jsonResponsePast);
							assertEquals(Prediction.EMPTY_PREDICTION, prediction);

							Integer[] predictions = prediction.asArray();

							// no future data
							assertEquals(0, predictions.length);

							String jsonResponseOld = String.format(
									"""
											    { "result": { "rates": [{ "start": "%s", "end": "%s", "value": 100 },{ "start": "%s", "end": "%s", "value": 200 }]}}
											""",
									currentHour.minusMinutes(60), currentHour, currentHour,
									currentHour.plusMinutes(60));

							prediction = api.parsePrediction(jsonResponseOld);
							assertNotEquals(Prediction.EMPTY_PREDICTION, prediction);

							predictions = prediction.asArray();

							// past data is dropped
							assertEquals(4, predictions.length);

							// check first timestamp (in UTC) from second dataset
							assertEquals(currentHour, prediction.getFirstTime());

							// check quaterly values
							assertEquals(200, predictions[0].intValue());
							assertEquals(200, predictions[1].intValue());
							assertEquals(200, predictions[2].intValue());
							assertEquals(200, predictions[3].intValue());

							// should cache stored values on old retrieval
							api.handleResponse(HttpResponse.ok(jsonResponsePast));
							assertNotEquals(Prediction.EMPTY_PREDICTION, api.getPrediction());

							predictions = prediction.asArray();

							// past data is dropped
							assertEquals(4, predictions.length);

							// check first timestamp (in UTC) from second dataset
							assertEquals(currentHour, prediction.getFirstTime());

							// check quaterly values
							assertEquals(200, predictions[0].intValue());
							assertEquals(200, predictions[1].intValue());
							assertEquals(200, predictions[2].intValue());
							assertEquals(200, predictions[3].intValue());

						})) //

				// Case: simulated API processing (invalid data, caching)
				.next(new TestCase("Successful API response") //
						.onBeforeProcessImage(() -> {
							// JSON data
							final String jsonResponseInvalid = String.format(
									"""
											    { "res": { "rates": [{ "start": "%s", "end": "%s", "value": 100 },{ "start": "%s", "end": "%s", "value": 200 }]}}
											""",
									currentHour, currentHour.plusHours(1), currentHour.plusHours(1),
									currentHour.plusHours(2));

							final String jsonResponseInvalidData = String.format(
									"""
											    { "result": { "rates": [{ "start": "%s", "end": "%s", "val": 100 },{ "start": "%s", "end": "%s", "val": 200 }]}}
											""",
									currentHour, currentHour.plusHours(1), currentHour.plusHours(1),
									currentHour.plusHours(2));

							// should have been initialized before
							assertNotEquals(Prediction.EMPTY_PREDICTION, api.getPrediction());

							Prediction prediction = api.parsePrediction(jsonResponseInvalid);
							assertEquals(Prediction.EMPTY_PREDICTION, prediction);
							assertNotEquals(Prediction.EMPTY_PREDICTION, api.getPrediction());

							prediction = api.parsePrediction(jsonResponseInvalidData);
							assertEquals(Prediction.EMPTY_PREDICTION, prediction);
							assertNotEquals(Prediction.EMPTY_PREDICTION, api.getPrediction());

						})) //

				.deactivate();
	}

	private String generateDynamicJson(Clock clock, int minutes) {
		ZonedDateTime now = ZonedDateTime.now(clock).withMinute(0).withSecond(0).withNano(0);
		ZonedDateTime endTime = now.plusHours(2);

		StringBuilder jsonBuilder = new StringBuilder();
		jsonBuilder.append("{ \"result\": { \"rates\": [");

		for (ZonedDateTime time = now; time.isBefore(endTime); time = time.plusMinutes(minutes)) {
			jsonBuilder.append(String.format("""
					    { "start": "%s", "end": "%s", "value": %d },
					""", time.format(this.formatter), time.plusMinutes(minutes).format(this.formatter),
					this.getSolarPredictionValue(time)));
		}

		jsonBuilder.setLength(jsonBuilder.length() - 2);
		jsonBuilder.append("]}}");

		return jsonBuilder.toString();
	}

	// simulate solar production
	private int getSolarPredictionValue(ZonedDateTime time) {
		int baseValue = time.getHour();
		int factor = time.getHour() > 6 && time.getHour() < 18 ? (time.getHour() - 6) * 500 : 0;
		return baseValue + factor;
	}
}
