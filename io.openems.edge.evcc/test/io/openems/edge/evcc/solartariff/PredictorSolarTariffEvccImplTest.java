package io.openems.edge.evcc.solartariff;

import static io.openems.common.test.TestUtils.createDummyClock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.Test;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.predictor.api.prediction.LogVerbosity;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

public class PredictorSolarTariffEvccImplTest {

	final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

	@Test
	public void testSolarTariffPrediction() throws Exception {
		final var sut = new PredictorSolarTariffEvccImpl();
		final var api = new PredictorSolarTariffEvccApi();
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var clock = createDummyClock();
		
		final LocalDateTime localCurrentHour = LocalDateTime.now().withSecond(0).withNano(0).withMinute(0);
		final ZoneId localZone = ZoneId.systemDefault();
		final ZonedDateTime localZoned = localCurrentHour.atZone(localZone);
		final ZonedDateTime currentHour = localZoned.withZoneSameInstant(ZoneId.of("UTC"));
		
		final int expectedFirstValue = this.getSolarPredictionValue(localZoned);
		final int expectedSecondHourValue = this.getSolarPredictionValue(localZoned.plusHours(1));

		new ComponentTest(sut).addReference("httpBridgeFactory", httpTestBundle.factory())
				.addReference("componentManager", new DummyComponentManager(clock))
				.activate(MyConfig.create().setId("predictor0").setUrl("http://evcc:7070/api/tariff/solar")
						.setLogVerbosity(LogVerbosity.REQUESTED_PREDICTIONS).build())

				// Case: API not found
				.next(new TestCase("API Not Found").onBeforeProcessImage(() -> {
					httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
					httpTestBundle.triggerNextCycle();
				}).onAfterProcessImage(() -> {
					assertEquals(Prediction.EMPTY_PREDICTION, sut.createNewPrediction(sut.getChannelAddresses()[0]));
				}))

				// Case: API unknown error
				.next(new TestCase("API Unknown Error").onBeforeProcessImage(() -> {
					httpTestBundle
							.forceNextFailedResult(new HttpError.UnknownError(new Exception("Simulated failure")));
					httpTestBundle.triggerNextCycle();
				}).onAfterProcessImage(() -> {
					assertEquals(Prediction.EMPTY_PREDICTION, sut.createNewPrediction(sut.getChannelAddresses()[0]));
				}))
				
				// Case: API success
				.next(new TestCase("API success").onBeforeProcessImage(() -> {
					String jsonResponse = this.generateDynamicJson(60);
					httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok(jsonResponse));
					httpTestBundle.triggerNextCycle();
					//TODO how to check asynchronous httpBridge response?
				}))

				// Case: response handling (manual)
				.next(new TestCase("API response").onBeforeProcessImage(() -> {
					// simulate response
					String jsonResponse = this.generateDynamicJson(60);
					api.handleResponse(HttpResponse.ok(jsonResponse));
					assertNotEquals(TimeOfUsePrices.EMPTY_PRICES, api.getPrediction());
				}))//
				
				// Case: simulated API processing (60 minutes)
				.next(new TestCase("Successful API response").onBeforeProcessImage(() -> {
					String jsonResponse = this.generateDynamicJson(60);
					assertEquals(Prediction.EMPTY_PREDICTION, sut.createNewPrediction(sut.getChannelAddresses()[0]));

					Prediction prediction = api.parsePrediction(jsonResponse);
					assertNotEquals(Prediction.EMPTY_PREDICTION, prediction);

					Integer[] predictions = prediction.asArray();

					// check object count
					assertEquals(8, predictions.length);

					// check first timestamp (in UTC)
					assertEquals(currentHour, prediction.getFirstTime());

					// check quaterly values
					assertEquals(expectedFirstValue, predictions[0].intValue());
					assertEquals(expectedFirstValue, predictions[1].intValue());
					assertEquals(expectedFirstValue, predictions[2].intValue());
					assertEquals(expectedFirstValue, predictions[3].intValue());

					// check first value of next hour
					assertEquals(expectedSecondHourValue, predictions[4].intValue());
				})) //

				// Case: simulated API processing (30 minutes)
				.next(new TestCase("Successful API response").onBeforeProcessImage(() -> {
					String jsonResponse = this.generateDynamicJson(30);
					Prediction prediction = api.parsePrediction(jsonResponse);
					assertNotEquals(Prediction.EMPTY_PREDICTION, prediction);

					Integer[] predictions = prediction.asArray();

					// check object count
					assertEquals(8, predictions.length);

					// check first timestamp (in UTC)
					assertEquals(currentHour, prediction.getFirstTime());

					// check quaterly values
					assertEquals(expectedFirstValue, predictions[0].intValue());
					assertEquals(expectedFirstValue, predictions[1].intValue());
					assertEquals(expectedFirstValue, predictions[2].intValue());
					assertEquals(expectedFirstValue, predictions[3].intValue());

					// check first value of next hour
					assertEquals(expectedSecondHourValue, predictions[4].intValue());
				})) //

				// Case: simulated API processing (15 minutes)
				.next(new TestCase("Successful API response").onBeforeProcessImage(() -> {
					String jsonResponse = this.generateDynamicJson(15);
					Prediction prediction = api.parsePrediction(jsonResponse);
					assertNotEquals(Prediction.EMPTY_PREDICTION, prediction);

					Integer[] predictions = prediction.asArray();

					// check object count
					assertEquals(8, predictions.length);

					// check first timestamp (in UTC)
					assertEquals(currentHour, prediction.getFirstTime());

					// check quaterly values
					assertEquals(expectedFirstValue, predictions[0].intValue());
					assertEquals(expectedFirstValue, predictions[1].intValue());
					assertEquals(expectedFirstValue, predictions[2].intValue());
					assertEquals(expectedFirstValue, predictions[3].intValue());

					// check first value of next hour
					assertEquals(expectedSecondHourValue, predictions[4].intValue());
				})) //
				
				.deactivate();
	}

	private String generateDynamicJson(int minutes) {
		ZonedDateTime now = ZonedDateTime.now().withMinute(0).withSecond(0).withNano(0);
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
