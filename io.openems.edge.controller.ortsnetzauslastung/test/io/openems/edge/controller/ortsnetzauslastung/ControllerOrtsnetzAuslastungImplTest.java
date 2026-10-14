package io.openems.edge.controller.ortsnetzauslastung;

import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyBridgeHttpExecutor;
import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyEndpointFetcher;
import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.NO_COORDINATES;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.UNABLE_TO_SEND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.openems.common.OpenemsConstants;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.types.DebugMode;
import io.openems.common.utils.StringUtils;
import io.openems.edge.common.meta.types.Coordinates;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class ControllerOrtsnetzAuslastungImplTest {

	public static final String COMPONENT_ID = "ctrlOrtsnetzAuslastung0";
	private static final String URL = "https://www.ortsnetz-auslastung.de/v1/measurements";

	@Test
	public void test() throws Exception {
		final var httpTestBundle = DummyBridgeHttpBundle.of();
		final var sut = new ControllerOrtsnetzAuslastungImpl();
		final var meta = new DummyMeta().withCoordinates(Coordinates.of(52.520008, 13.404954));
		final var meter = new DummyElectricityMeter("meter0");

		new ControllerTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("oem", new DummyOpenemsEdgeOem()) //
				.addReference("meta", meta) //
				.addReference("meter", meter) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setMeterId("meter0") //
						.setUrl(URL) //
						.setDebugMode(DebugMode.OFF) //
						.build()) //
				.next(new TestCase().output(COMPONENT_ID, NO_COORDINATES, false)) //
				.deactivate();

		{
			// single-phase measurement: only L1 voltage and no frequency available
			meter.withVoltageL1(230123); // 230.123 V
			var data = sut.collectData();
			var json = data.toJson();
			assertTrue(data.observedAt().toString().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"));
			assertEquals(52.520008, json.get("latitude").getAsDouble(), 0.000001);
			assertEquals(13.404954, json.get("longitude").getAsDouble(), 0.000001);
			assertEquals(230.123, json.get("l1_v").getAsDouble(), 0.000001);
			assertEquals(-1., json.get("l2_v").getAsDouble(), 0.000001);
			assertEquals(-1., json.get("l3_v").getAsDouble(), 0.000001);
			assertFalse(json.has("grid_frequency_hz"));
			assertEquals("OpenEMS-" + OpenemsConstants.VERSION, json.get("integration_version").getAsString());
		}
		{
			// three-phase measurement with frequency
			meter.withVoltageL2(229900) //
					.withVoltageL3(230400);
			TestUtils.withValue(meter, ElectricityMeter.ChannelId.FREQUENCY, 50010); // 50.01 Hz
			var json = sut.collectData().toJson();
			assertEquals(229.9, json.get("l2_v").getAsDouble(), 0.000001);
			assertEquals(230.4, json.get("l3_v").getAsDouble(), 0.000001);
			assertEquals(50.01, json.get("grid_frequency_hz").getAsDouble(), 0.000001);
		}
	}

	@Test
	public void testWithoutCoordinates() throws Exception {
		final var sut = new ControllerOrtsnetzAuslastungImpl();
		final var meter = new DummyElectricityMeter("meter0") //
				.withVoltageL1(230123);

		new ControllerTest(sut) //
				.addReference("httpBridgeFactory", DummyBridgeHttpBundle.of().factory()) //
				.addReference("oem", new DummyOpenemsEdgeOem()) //
				.addReference("meta", new DummyMeta()) // no coordinates
				.addReference("meter", meter) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setMeterId("meter0") //
						.setUrl(URL) //
						.setDebugMode(DebugMode.OFF) //
						.build()) //
				.next(new TestCase().output(COMPONENT_ID, NO_COORDINATES, true)) //
				.deactivate();
	}

	@Test
	public void testWithoutPhaseVoltage() throws Exception {
		final var sut = new ControllerOrtsnetzAuslastungImpl();
		final var meta = new DummyMeta().withCoordinates(Coordinates.of(52.520008, 13.404954));
		final var meter = new DummyElectricityMeter("meter0"); // no voltage available

		new ControllerTest(sut) //
				.addReference("httpBridgeFactory", DummyBridgeHttpBundle.of().factory()) //
				.addReference("oem", new DummyOpenemsEdgeOem()) //
				.addReference("meta", meta) //
				.addReference("meter", meter) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setMeterId("meter0") //
						.setUrl(URL) //
						.setDebugMode(DebugMode.OFF) //
						.build()) //
				.next(new TestCase().output(COMPONENT_ID, UNABLE_TO_SEND, true)) //
				.deactivate();
	}

	@Test
	public void testSendData() throws Exception {
		final var clock = createDummyClock();
		final var executor = dummyBridgeHttpExecutor(clock);
		final var fetcher = dummyEndpointFetcher();

		fetcher.addEndpointHandler(t -> {
			assertEquals(HttpMethod.POST, t.method());
			return HttpResponse.ok("{}");
		});

		final var sut = new ControllerOrtsnetzAuslastungImpl();
		final var meta = new DummyMeta().withCoordinates(Coordinates.of(52.520008, 13.404954));
		final var meter = new DummyElectricityMeter("meter0") //
				.withVoltageL1(230100) //
				.withVoltageL2(229900) //
				.withVoltageL3(230400);

		new ControllerTest(sut) //
				.addReference("httpBridgeFactory", //
						DummyBridgeHttpFactory.ofBridgeImpl(() -> fetcher, () -> executor)) //
				.addReference("oem", new DummyOpenemsEdgeOem()) //
				.addReference("meta", meta) //
				.addReference("meter", meter) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setMeterId("meter0") //
						.setUrl(URL) //
						.setDebugMode(DebugMode.DETAILED) //
						.build()) //
				.next(new TestCase()) // run() subscribes the send task
				.next(new TestCase() //
						.also(testCase -> {
							executor.update(); // executes the queued send task
							executor.update(); // ... and the actual request
							assertEquals("HTTP:200 OK", sut.debugLog());
						}) //
						.output(COMPONENT_ID, UNABLE_TO_SEND, false)) //
				.deactivate();
	}

	@Test
	public void testIntegrationVersionMaxLength() {
		// integration_version is built from OEM model and OpenEMS version; max 32 chars
		var version = StringUtils.toShortString("OpenEMS-" + OpenemsConstants.VERSION, 32);
		assertTrue(version.length() <= 32);
	}
}
