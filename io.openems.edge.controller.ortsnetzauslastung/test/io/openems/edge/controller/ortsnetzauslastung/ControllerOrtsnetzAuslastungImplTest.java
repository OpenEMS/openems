package io.openems.edge.controller.ortsnetzauslastung;

import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyBridgeHttpExecutor;
import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyEndpointFetcher;
import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.GRID_STATUS;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.GRID_STATUS_L1;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.LOCATION_BLOCKED;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.NO_COORDINATES;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.NO_GRID_METER;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.NO_VOLTAGE;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.REDISPATCH_ACTIVE;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.REQUEST_REJECTED;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.STORAGE_RECOMMENDATION;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.THIRD_PARTY_USAGE_NOT_ACCEPTED;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.UNABLE_TO_SEND;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.FREQUENCY;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.bridge.http.dummy.DummyEndpointFetcher;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.DebugMode;
import io.openems.common.types.HttpStatus;
import io.openems.common.types.MeterType;
import io.openems.edge.common.meta.ThirdPartyUsageAcceptance;
import io.openems.edge.common.meta.types.Coordinates;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.controller.ortsnetzauslastung.Types.GridStatus;
import io.openems.edge.controller.ortsnetzauslastung.Types.StorageRecommendation;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.meter.test.DummyElectricityMeter;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.test.DummyPredictorManager;
import io.openems.edge.timedata.test.DummyTimedata;

public class ControllerOrtsnetzAuslastungImplTest {

	private static final String COMPONENT_ID = "ctrlOrtsnetzAuslastung0";
	private static final String URL = "https://www.ortsnetz-auslastung.de/v1/measurements";
	private static final String RESPONSE = """
			{"accepted":true,"created":true,
			 "status":{"l1":"yellow","l2":"green","l3":"green","overall":"yellow"},
			 "storage_recommendation":"charge",
			 "redispatch":{"active":true,"affected_units":3,"active_measures":2,"data_updated_at":null}}
			""";

	private static final JsonElement PRODUCED_TODAY_WH = new JsonPrimitive(5000);

	private static class TestTimedata extends DummyTimedata {

		private final JsonElement producedTodayWh;

		private TestTimedata(JsonElement producedTodayWh) {
			super("timedata0");
			this.producedTodayWh = producedTodayWh;
		}

		@Override
		public SortedMap<ChannelAddress, JsonElement> queryHistoricEnergy(String edgeId, ZonedDateTime fromDate,
				ZonedDateTime toDate, Set<ChannelAddress> channels) {
			final var result = new TreeMap<ChannelAddress, JsonElement>();
			channels.forEach(channel -> result.put(channel, this.producedTodayWh));
			return result;
		}
	}

	private static class TestPredictorManager extends DummyPredictorManager {

		private final Prediction prediction;

		private TestPredictorManager(Prediction prediction) {
			this.prediction = prediction;
		}

		@Override
		public Prediction getPrediction(ChannelAddress channelAddress) {
			return this.prediction;
		}
	}

	private static Prediction predictionOf(ZonedDateTime time, int quarters, int power) {
		final var values = new Integer[quarters];
		Arrays.fill(values, power);
		return Prediction.from(time.toInstant(), values);
	}

	private record Setup(ControllerOrtsnetzAuslastungImpl sut, TimeLeapClock clock, DummyElectricityMeter meter, DummyMeta meta, DummySum sum,
			DummyEndpointFetcher fetcher, List<JsonObject> requests, ControllerTest test) {
	}

	private static Setup setup(HttpResponse<String> response) throws Exception {
		return setup(response, "");
	}

	private static Setup setup(HttpResponse<String> response, String meterId) throws Exception {
		final var clock = createDummyClock();
		final var meter = new DummyElectricityMeter("meter0") //
				.withMeterType(MeterType.GRID) //
				.withVoltageL1(230100) //
				.withVoltageL2(0) // no measurement
				.withVoltageL3(229500) //
				.withCurrentL1(-5000) //
				.withCurrentL2(0) //
				.withCurrentL3(2500) //
				.withActivePower(-1500);
		TestUtils.withValue(meter, FREQUENCY, 50010);
		final var meta = new DummyMeta() //
				.withCoordinates(Coordinates.of(52.520008, 13.404954)) //
				.withThirdPartyUsageAcceptance(ThirdPartyUsageAcceptance.ACCEPTED);
		final var sum = new DummySum() //
				.withEssSoc(62) //
				.withEssCapacity(10200);

		final var requests = new ArrayList<JsonObject>();
		final var fetcher = dummyEndpointFetcher();
		fetcher.addEndpointHandler(endpoint -> {
			requests.add(JsonParser.parseString(endpoint.body()).getAsJsonObject());
			return response;
		});
		final var executor = dummyBridgeHttpExecutor(clock, true);

		final var sut = new ControllerOrtsnetzAuslastungImpl();
		final var today = ZonedDateTime.now(clock);
		final var test = new ControllerTest(sut) //
				.addReference("httpBridgeFactory", DummyBridgeHttpFactory.ofBridgeImpl(() -> fetcher, () -> executor)) //
				.addReference("oem", new DummyOpenemsEdgeOem()) //
				.addReference("meta", meta) //
				.addReference("sum", sum) //
				.addReference("componentManager", new DummyComponentManager(clock).addComponent(meter)) //
				.addReference("predictorManager", new TestPredictorManager(predictionOf(today, 96, 1000))) //
				.addReference("timedata", new TestTimedata(PRODUCED_TODAY_WH)) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setUrl(URL) //
						.setMeterId(meterId) //
						.setPlantCapacityKwp(9.8) //
						.setDebugMode(DebugMode.OFF) //
						.build());
		return new Setup(sut, clock, meter, meta, sum, fetcher, requests, test);
	}

	@Test
	public void testSendAndResponse() throws Exception {
		final var s = setup(new HttpResponse<>(HttpStatus.ACCEPTED, RESPONSE));
		s.test() //
				.next(new TestCase() // first measurement is sent immediately
						.onAfterControllersCallbacks(() -> assertEquals(1, s.requests().size())) //
						.output(COMPONENT_ID, NO_COORDINATES, false) //
						.output(COMPONENT_ID, THIRD_PARTY_USAGE_NOT_ACCEPTED, false) //
						.output(COMPONENT_ID, NO_GRID_METER, false) //
						.output(COMPONENT_ID, NO_VOLTAGE, false)) //
				.next(new TestCase() // response is applied
						.output(COMPONENT_ID, UNABLE_TO_SEND, false) //
						.output(COMPONENT_ID, GRID_STATUS, GridStatus.YELLOW) //
						.output(COMPONENT_ID, GRID_STATUS_L1, GridStatus.YELLOW) //
						.output(COMPONENT_ID, STORAGE_RECOMMENDATION, StorageRecommendation.CHARGE) //
						.output(COMPONENT_ID, REDISPATCH_ACTIVE, true)) //
				.next(new TestCase() //
						.timeleap(s.clock(), 4, MINUTES) //
						.onAfterControllersCallbacks(() -> assertEquals(1, s.requests().size()))) //
				.next(new TestCase() //
						.timeleap(s.clock(), 1, MINUTES) //
						.onAfterControllersCallbacks(() -> assertEquals(2, s.requests().size()))) //
				.deactivate();

		final var first = s.requests().get(0);
		assertEquals(52.520008, first.get("latitude").getAsDouble(), 0.000001);
		assertEquals(230.1, first.get("l1_v").getAsDouble(), 0.000001);
		assertEquals(-1., first.get("l2_v").getAsDouble(), 0.000001);
		assertEquals(229.5, first.get("l3_v").getAsDouble(), 0.000001);
		assertEquals(230.1, first.get("l1_v_avg").getAsDouble(), 0.000001);
		assertFalse(first.has("l2_v_min"));
		assertEquals(50.01, first.get("grid_frequency_hz").getAsDouble(), 0.000001);
		assertEquals(-1500, first.get("grid_power_w").getAsInt());
		assertEquals(5., first.get("l1_a").getAsDouble(), 0.000001);
		assertEquals(-1., first.get("l2_a").getAsDouble(), 0.000001);
		assertEquals(2.5, first.get("l3_a").getAsDouble(), 0.000001);
		assertEquals(62, first.get("battery_soc_percent").getAsInt());
		assertEquals(9.8, first.get("plant_capacity_kwp").getAsDouble(), 0.000001);
		assertEquals(10.2, first.get("battery_capacity").getAsDouble(), 0.000001);
		assertTrue(first.get("observed_at").getAsString().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));

		// Static values are only sent again if they change
		final var second = s.requests().get(1);
		assertFalse(second.has("plant_capacity_kwp"));
		assertFalse(second.has("battery_capacity"));
	}

	@Test
	public void testMissingVoltageDoesNotChangeInterval() throws Exception {
		final var s = setup(new HttpResponse<>(HttpStatus.ACCEPTED, RESPONSE));
		s.test() //
				.next(new TestCase() //
						.onAfterControllersCallbacks(() -> assertEquals(1, s.requests().size()))) //
				.next(new TestCase() //
						.timeleap(s.clock(), 30, SECONDS) //
						.onBeforeProcessImage(() -> s.meter() //
								.withVoltageL1(null) //
								.withVoltageL3(null)) //
						.output(COMPONENT_ID, NO_VOLTAGE, true)) //
				.next(new TestCase() //
						.timeleap(s.clock(), 30, SECONDS) //
						.onBeforeProcessImage(() -> s.meter() //
								.withVoltageL1(230100) //
								.withVoltageL3(229500)) //
						.output(COMPONENT_ID, NO_VOLTAGE, false) //
						.onAfterControllersCallbacks(() -> assertEquals(1, s.requests().size()))) //
				.deactivate();
	}

	@Test
	public void testStatistics() throws Exception {
		final var s = setup(new HttpResponse<>(HttpStatus.ACCEPTED, "{}"));
		s.test() //
				.next(new TestCase()) // first measurement
				.next(new TestCase() //
						.onBeforeProcessImage(() -> s.meter().withVoltageL1(228000))) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> s.meter().withVoltageL1(234000))) //
				.next(new TestCase() //
						.timeleap(s.clock(), 5, MINUTES) //
						.onBeforeProcessImage(() -> s.meter().withVoltageL1(231000))) //
				.deactivate();

		final var second = s.requests().get(1);
		assertEquals(228., second.get("l1_v_min").getAsDouble(), 0.000001);
		assertEquals(234., second.get("l1_v_max").getAsDouble(), 0.000001);
		assertEquals(231., second.get("l1_v_avg").getAsDouble(), 0.000001);
		assertEquals(231., second.get("l1_v").getAsDouble(), 0.000001);
	}

	@Test
	public void testNonJsonResponseIsSuccess() throws Exception {
		final var s = setup(HttpResponse.ok("OK"));
		s.test() //
				.next(new TestCase()) //
				.next(new TestCase() //
						.output(COMPONENT_ID, UNABLE_TO_SEND, false) //
						.output(COMPONENT_ID, GRID_STATUS, GridStatus.UNDEFINED)) //
				.deactivate();
	}

	@Test
	public void testLocationBlocked() throws Exception {
		final var s = setup(new HttpResponse<>(HttpStatus.FORBIDDEN, "{}"));
		s.test() //
				.next(new TestCase()) //
				.next(new TestCase() //
						.output(COMPONENT_ID, LOCATION_BLOCKED, true) //
						.output(COMPONENT_ID, UNABLE_TO_SEND, false)) //
				.deactivate();
	}

	@Test
	public void testRequestRejected() throws Exception {
		final var s = setup(new HttpResponse<>(HttpStatus.UNPROCESSABLE_ENTITY, "{\"detail\":\"invalid\"}"));
		s.test() //
				.next(new TestCase()) //
				.next(new TestCase() //
						.output(COMPONENT_ID, REQUEST_REJECTED, true) //
						.output(COMPONENT_ID, UNABLE_TO_SEND, false)) //
				.deactivate();
	}

	@Test
	public void testWithoutCoordinates() throws Exception {
		final var s = setup(new HttpResponse<>(HttpStatus.ACCEPTED, RESPONSE));
		s.meta().withCoordinates(null);
		s.test() //
				.next(new TestCase() //
						.output(COMPONENT_ID, NO_COORDINATES, true) //
						.onAfterControllersCallbacks(() -> assertEquals(0, s.requests().size()))) //
				.deactivate();
	}

	@ParameterizedTest
	@EnumSource(value = ThirdPartyUsageAcceptance.class, names = { "UNDECIDED", "DECLINED" })
	public void testThirdPartyUsageNotAccepted(ThirdPartyUsageAcceptance acceptance) throws Exception {
		final var s = setup(new HttpResponse<>(HttpStatus.ACCEPTED, RESPONSE));
		s.meta().withThirdPartyUsageAcceptance(acceptance);
		s.test() //
				.next(new TestCase() //
						.output(COMPONENT_ID, THIRD_PARTY_USAGE_NOT_ACCEPTED, true) //
						.onAfterControllersCallbacks(() -> assertEquals(0, s.requests().size()))) //
				.deactivate();
	}

	@Test
	public void testWithoutValidVoltage() throws Exception {
		final var s = setup(new HttpResponse<>(HttpStatus.ACCEPTED, RESPONSE));
		s.meter() //
				.withVoltageL1(0) //
				.withVoltageL3(600_000); // above 500 V
		s.test() //
				.next(new TestCase() //
						.output(COMPONENT_ID, NO_VOLTAGE, true) //
						.onAfterControllersCallbacks(() -> assertEquals(0, s.requests().size()))) //
				.deactivate();
	}

	@Test
	public void testWithoutGridMeter() throws Exception {
		final var s = setup(new HttpResponse<>(HttpStatus.ACCEPTED, RESPONSE));
		s.meter().withMeterType(MeterType.PRODUCTION);
		s.test() //
				.next(new TestCase() //
						.output(COMPONENT_ID, NO_GRID_METER, true) //
						.onAfterControllersCallbacks(() -> assertEquals(0, s.requests().size()))) //
				.deactivate();
	}

	@Test
	public void testConfiguredMeter() throws Exception {
		// Configured meter is used independent of its meter type
		final var s = setup(new HttpResponse<>(HttpStatus.ACCEPTED, RESPONSE), "meter0");
		s.meter().withMeterType(MeterType.PRODUCTION);
		s.test() //
				.next(new TestCase() //
						.output(COMPONENT_ID, NO_GRID_METER, false) //
						.onAfterControllersCallbacks(() -> assertEquals(1, s.requests().size()))) //
				.deactivate();
	}

	@Test
	public void testConfiguredMeterNotAvailable() throws Exception {
		final var s = setup(new HttpResponse<>(HttpStatus.ACCEPTED, RESPONSE), "meter1");
		s.test() //
				.next(new TestCase() //
						.output(COMPONENT_ID, NO_GRID_METER, true) //
						.onAfterControllersCallbacks(() -> assertEquals(0, s.requests().size()))) //
				.deactivate();
	}

	@Test
	public void testPvForecastIsSentWithSecondMeasurement() throws Exception {
		final var s = setup(new HttpResponse<>(HttpStatus.ACCEPTED, RESPONSE));
		s.test() //
				.next(new TestCase()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							// Wait for the update in the background thread
							for (var i = 0; i < 100 && s.sut().getPvForecastKwh(s.clock().instant()) == null; i++) {
								Thread.sleep(50);
							}
						}) //
						.timeleap(s.clock(), 5, MINUTES)) //
				.deactivate();

		assertFalse(s.requests().get(0).has("pv_forecast_kwh"));
		// 5 kWh produced + 95 remaining quarters with 1 kW
		assertEquals(28.75, s.requests().get(1).get("pv_forecast_kwh").getAsDouble(), 0.000001);
	}

	@Test
	public void testPvForecastDisabled() throws Exception {
		final var requests = new ArrayList<JsonObject>();
		final var clock = createDummyClock();
		final var fetcher = dummyEndpointFetcher();
		fetcher.addEndpointHandler(endpoint -> {
			requests.add(JsonParser.parseString(endpoint.body()).getAsJsonObject());
			return new HttpResponse<>(HttpStatus.ACCEPTED, RESPONSE);
		});
		final var executor = dummyBridgeHttpExecutor(clock, true);
		final var meter = new DummyElectricityMeter("meter0") //
				.withMeterType(MeterType.GRID) //
				.withVoltageL1(230100);
		final var now = ZonedDateTime.now(clock);
		final var sut = new ControllerOrtsnetzAuslastungImpl();
		new ControllerTest(sut) //
				.addReference("httpBridgeFactory", DummyBridgeHttpFactory.ofBridgeImpl(() -> fetcher, () -> executor)) //
				.addReference("oem", new DummyOpenemsEdgeOem()) //
				.addReference("meta", new DummyMeta() //
						.withCoordinates(Coordinates.of(52.520008, 13.404954)) //
						.withThirdPartyUsageAcceptance(ThirdPartyUsageAcceptance.ACCEPTED)) //
				.addReference("sum", new DummySum()) //
				.addReference("componentManager", new DummyComponentManager(clock).addComponent(meter)) //
				.addReference("predictorManager", new TestPredictorManager(predictionOf(now, 96, 1000))) //
				.addReference("timedata", new TestTimedata(PRODUCED_TODAY_WH)) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setUrl(URL) //
						.setPlantCapacityKwp(9.8) //
						.setSendPvForecast(false) //
						.setDebugMode(DebugMode.OFF) //
						.build()) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> Thread.sleep(200)) //
						.timeleap(clock, 5, MINUTES)) //
				.deactivate();

		assertEquals(2, requests.size());
		assertFalse(requests.get(1).has("pv_forecast_kwh"));
		assertEquals(null, sut.getPvForecastKwh(clock.instant()));
	}

	@Test
	public void testCalculatePvForecast() {
		final var midnight = ZonedDateTime.parse("2026-11-04T00:00:00+01:00[Europe/Berlin]");
		final var noon = midnight.plusHours(12).plusMinutes(5);
		final var prediction = predictionOf(noon.minusMinutes(5), 192, 1000);

		// 3 kWh produced + 47 remaining quarters with 1 kW
		assertEquals(14.75, ControllerOrtsnetzAuslastungImpl.calculatePvForecastKwh(9.8, noon, 3000., prediction));

		// Last quarter of the day: nothing remains
		final var lastQuarter = midnight.plusHours(23).plusMinutes(50);
		assertEquals(20., ControllerOrtsnetzAuslastungImpl.calculatePvForecastKwh(9.8, lastQuarter, 20_000.,
				Prediction.EMPTY_PREDICTION));

		// Negative predicted values do not reduce the forecast
		assertEquals(3., ControllerOrtsnetzAuslastungImpl.calculatePvForecastKwh(9.8, noon, 3000.,
				predictionOf(noon.minusMinutes(5), 192, -1000)));

		// Not available: no capacity, unknown or negative energy, no prediction
		assertEquals(null, ControllerOrtsnetzAuslastungImpl.calculatePvForecastKwh(0, noon, 3000., prediction));
		assertEquals(null, ControllerOrtsnetzAuslastungImpl.calculatePvForecastKwh(9.8, noon, null, prediction));
		assertEquals(null, ControllerOrtsnetzAuslastungImpl.calculatePvForecastKwh(9.8, noon, -1., prediction));
		assertEquals(null,
				ControllerOrtsnetzAuslastungImpl.calculatePvForecastKwh(9.8, noon, 3000., Prediction.EMPTY_PREDICTION));

		// Prediction ends before midnight
		assertEquals(null, ControllerOrtsnetzAuslastungImpl.calculatePvForecastKwh(9.8, noon, 3000.,
				predictionOf(noon.minusMinutes(5), 4, 1000)));

		// More than 10 kWh per kWp would be rejected by the API
		assertEquals(null, ControllerOrtsnetzAuslastungImpl.calculatePvForecastKwh(1, noon, 3000., prediction));
	}

	@Test
	public void testPvForecastWithoutProducedEnergy() throws Exception {
		final var s = setup(new HttpResponse<>(HttpStatus.ACCEPTED, RESPONSE));
		final var sut = new ControllerOrtsnetzAuslastungImpl();
		final var now = ZonedDateTime.now(s.clock());
		new ControllerTest(sut) //
				.addReference("httpBridgeFactory", DummyBridgeHttpFactory.ofDummyBridge()) //
				.addReference("oem", new DummyOpenemsEdgeOem()) //
				.addReference("meta", s.meta()) //
				.addReference("sum", s.sum()) //
				.addReference("componentManager", new DummyComponentManager(s.clock())) //
				.addReference("predictorManager", new TestPredictorManager(predictionOf(now, 96, 1000))) //
				.addReference("timedata", new TestTimedata(JsonNull.INSTANCE)) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setUrl(URL) //
						.setPlantCapacityKwp(9.8) //
						.setDebugMode(DebugMode.OFF) //
						.build());
		assertEquals(null, sut.calculatePvForecastKwh(now));
	}

	@Test
	public void testIntegrationVersion() {
		assertEquals("OpenEMS-2026.11.0", //
				ControllerOrtsnetzAuslastungImpl.getIntegrationVersion("OpenEMS", "2026.11.0"));

		// Model is shortened, version is kept
		final var version = ControllerOrtsnetzAuslastungImpl.getIntegrationVersion("FENECON Home 10",
				"2026.11.0-SNAPSHOT");
		assertEquals(32, version.length());
		assertTrue(version.endsWith("-2026.11.0-SNAPSHOT"));
	}

	@Test
	public void testIntervalStatistics() {
		final var statistics = new IntervalStatistics();
		assertEquals(null, statistics.get(1000));
		statistics.add(229000);
		statistics.add(null);
		statistics.add(231001);
		final var result = statistics.get(1000);
		assertEquals(229., result.min(), 0.000001);
		assertEquals(231.001, result.max(), 0.000001);
		assertEquals(230.001, result.avg(), 0.000001);
		statistics.reset();
		assertEquals(null, statistics.get(1000));
	}

	@Test
	public void testValidation() {
		assertEquals(null, ControllerOrtsnetzAuslastungImpl.validVoltage(0));
		assertEquals(null, ControllerOrtsnetzAuslastungImpl.validVoltage(500_001));
		assertEquals(230000, ControllerOrtsnetzAuslastungImpl.validVoltage(230000));
		assertEquals(null, ControllerOrtsnetzAuslastungImpl.validFrequency(0));
		assertEquals(50000, ControllerOrtsnetzAuslastungImpl.validFrequency(50000));
	}
}
