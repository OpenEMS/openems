package io.openems.edge.io.shelly.shellypmminigen3;

import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.CURRENT;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.FREQUENCY;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.VOLTAGE;
import static io.openems.edge.common.type.Phase.SinglePhase.L1;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.io.shelly.common.ShellyCommon;
import io.openems.edge.timedata.test.DummyTimedata;

public class IoShellyPmMiniGen3ImplTest {

	private static final String COMPONENT_ID = "ioMiniGen3";

	private static final ChannelAddress SLAVE_COMMUNICATION_FAILED = new ChannelAddress(COMPONENT_ID,
			"SlaveCommunicationFailed");

	@Test
	public void test() throws Exception {
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var sut = new IoShellyPmMiniGen3Impl();
		
		// Pre-set the response for the /shelly endpoint that will be called during activation
		httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
			{
				"name": "shellypmminigen3-test",
				"id": "shellypmminigen3-12345",
				"mac": "AA:BB:CC:DD:EE:FF",
				"model": "S3PM-001PCEU16",
				"gen": 3,
				"fw_id": "20230912-114516/v1.14.0-gcb84623",
				"ver": "1.14.0",
				"app": "PmMiniG3",
				"auth_en": false,
				"auth_domain": "shellypmminigen3-12345"
			}
		"""));

		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setIp("127.0.0.1") //
						.setType(MeterType.GRID) //
						.setPhase(L1) //
						.build()) //

				// Test case for a successful read response
				.next(new TestCase("Successful read response").onBeforeControllersCallbacks(() -> {
					httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
							{
							  "id": 0,
							  "voltage": 232.4,
							  "current": 0.026,
							  "apower": 3.9,
							  "freq": 50,
							  "aenergy": {
							    "total": 59138.3,
							    "by_minute": [0, 0, 211.747],
							    "minute_ts": 1730237700
							  },
							  "ret_aenergy": {
							    "total": 0,
							    "by_minute": [0, 0, 0],
							    "minute_ts": 1730237700
							  }
							}
							                        """));
					httpTestBundle.triggerNextCycle();
				}) //
						.output(ACTIVE_POWER, 4) //
						.output(CURRENT, 26) //
						.output(VOLTAGE, 232400) //
						.output(FREQUENCY, 50000) //
						.output(SLAVE_COMMUNICATION_FAILED, false) //
						.output(ShellyCommon.ChannelId.AUTH_ENABLED_WARNING, false) //
						.output(ShellyCommon.ChannelId.DEVICE_GENERATION, "3")) //

				// Test case for an invalid read response
				.next(new TestCase("Invalid read response").onBeforeControllersCallbacks(() -> {
					httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
					httpTestBundle.triggerNextCycle();
				}) //
						.output(ACTIVE_POWER, null) //
						.output(CURRENT, null) //
						.output(VOLTAGE, null) //
						.output(FREQUENCY, null) //
						.output(SLAVE_COMMUNICATION_FAILED, true) //
						.output(ShellyCommon.ChannelId.AUTH_ENABLED_WARNING, false) //
						.output(ShellyCommon.ChannelId.DEVICE_GENERATION, "3")) //

				.deactivate();
	}
	
	@Test
	public void testAuthenticationWarning() throws Exception {
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var sut = new IoShellyPmMiniGen3Impl();
		
		// Pre-set the response for the /shelly endpoint with authentication enabled
		httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
			{
				"name": "shellypmminigen3-test",
				"id": "shellypmminigen3-12345",
				"mac": "AA:BB:CC:DD:EE:FF",
				"model": "S3PM-001PCEU16",
				"gen": 3,
				"fw_id": "20230912-114516/v1.14.0-gcb84623",
				"ver": "1.14.0",
				"app": "PmMiniG3",
				"auth_en": true,
				"auth_domain": "shellypmminigen3-12345"
			}
		"""));
		
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setIp("127.0.0.1") //
						.setType(MeterType.GRID) //
						.setPhase(L1) //
						.build()) //
				.next(new TestCase("Authentication enabled warning") //
						.output(ShellyCommon.ChannelId.AUTH_ENABLED_WARNING, true) //
						.output(ShellyCommon.ChannelId.DEVICE_GENERATION, "3")) //
				.deactivate();//
	}
}
