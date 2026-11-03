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
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.THIRD_PARTY_USAGE_DECLINED;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.UNABLE_TO_SEND;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.FREQUENCY;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.bridge.http.dummy.DummyEndpointFetcher;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.test.TimeLeapClock;
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

public class ControllerOrtsnetzAuslastungImplTest {

	private static final String COMPONENT_ID = "ctrlOrtsnetzAuslastung0";
	private static final String URL = "https://www.ortsnetz-auslastung.de/v1/measurements";
	private static final String RESPONSE = """
			{"accepted":true,"created":true,
			 "status":{"l1":"yellow","l2":"green","l3":"green","overall":"yellow"},
			 "storage_recommendation":"charge",
			 "redispatch":{"active":true,"affected_units":3,"active_measures":2,"data_updated_at":null}}
			""";

	private record Setup(TimeLeapClock clock, DummyElectricityMeter meter, DummyMeta meta, DummySum sum,
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
		final var meta = new DummyMeta().withCoordinates(Coordinates.of(52.520008, 13.404954));
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

		final var test = new ControllerTest(new ControllerOrtsnetzAuslastungImpl()) //
				.addReference("httpBridgeFactory", DummyBridgeHttpFactory.ofBridgeImpl(() -> fetcher, () -> executor)) //
				.addReference("oem", new DummyOpenemsEdgeOem()) //
				.addReference("meta", meta) //
				.addReference("sum", sum) //
				.addReference("componentManager", new DummyComponentManager(clock).addComponent(meter)) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setUrl(URL) //
						.setMeterId(meterId) //
						.setPlantCapacityKwp(9.8) //
						.setDebugMode(DebugMode.OFF) //
						.build());
		return new Setup(clock, meter, meta, sum, fetcher, requests, test);
	}

	@Test
	public void testSendAndResponse() throws Exception {
		final var s = setup(new HttpResponse<>(HttpStatus.ACCEPTED, RESPONSE));
		s.test() //
				.next(new TestCase() // first measurement is sent immediately
						.onAfterControllersCallbacks(() -> assertEquals(1, s.requests().size())) //
						.output(COMPONENT_ID, NO_COORDINATES, false) //
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

	@Test
	public void testThirdPartyUsageDeclined() throws Exception {
		final var s = setup(new HttpResponse<>(HttpStatus.ACCEPTED, RESPONSE));
		s.meta().withThirdPartyUsageAcceptance(ThirdPartyUsageAcceptance.DECLINED);
		s.test() //
				.next(new TestCase() //
						.output(COMPONENT_ID, THIRD_PARTY_USAGE_DECLINED, true) //
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
