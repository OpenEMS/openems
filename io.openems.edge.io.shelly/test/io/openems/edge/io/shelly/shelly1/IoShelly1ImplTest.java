package io.openems.edge.io.shelly.shelly1;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.io.shelly.common.ShellyCommon;

public class IoShelly1ImplTest {

	@Test
	public void test() throws Exception {
		final var sut = new IoShelly1Impl();
		final var httpTestBundle = new DummyBridgeHttpBundle();
		
		// Pre-set the response for the /shelly endpoint that will be called during activation
		httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
			{
				"name": "shelly1-test",
				"id": "shelly1-12345",
				"mac": "AA:BB:CC:DD:EE:FF",
				"model": "SHSW-1",
				"gen": 1,
				"fw_id": "20230912-114516/v1.14.0-gcb84623",
				"ver": "1.14.0",
				"app": "shelly1",
				"auth_en": false,
				"auth_domain": "shelly1-12345"
			}
		"""));
		
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setIp("127.0.0.1") //
						.build()) //

				// Test case for a successful JSON response
				.next(new TestCase("Successful read response") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
										{
											"relays": [
												{
													"ison": true,
												    "has_timer": false,
												    "timer_started": 0,
												    "timer_duration": 0,
										      		"timer_remaining": 0,
										      		"source": "http"
												}
											]
										}
									"""));
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("x", sut.debugLog())) //

						.output(IoShelly1.ChannelId.RELAY, null) // expecting WriteValue
						.output(IoShelly1.ChannelId.SLAVE_COMMUNICATION_FAILED, false) //
						.output(ShellyCommon.ChannelId.AUTH_ENABLED_WARNING, false) //
						.output(ShellyCommon.ChannelId.DEVICE_GENERATION, "1")) //

				// Test case for an invalid JSON response
				.next(new TestCase("Invalid read response") //
						.onBeforeProcessImage(() -> { //
							httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("?", sut.debugLog())) //

						.output(IoShelly1.ChannelId.RELAY, null) // expecting WriteValue
						.output(IoShelly1.ChannelId.SLAVE_COMMUNICATION_FAILED, true) //
						.output(ShellyCommon.ChannelId.AUTH_ENABLED_WARNING, false) //
						.output(ShellyCommon.ChannelId.DEVICE_GENERATION, "1")) //

				.deactivate();//
	}
	
	@Test
	public void testAuthenticationWarning() throws Exception {
		final var sut = new IoShelly1Impl();
		final var httpTestBundle = new DummyBridgeHttpBundle();
		
		// Pre-set the response for the /shelly endpoint with authentication enabled
		httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
			{
				"name": "shelly1-test",
				"id": "shelly1-12345",
				"mac": "AA:BB:CC:DD:EE:FF",
				"model": "SHSW-1",
				"gen": 1,
				"fw_id": "20230912-114516/v1.14.0-gcb84623",
				"ver": "1.14.0",
				"app": "shelly1",
				"auth_en": true,
				"auth_domain": "shelly1-12345"
			}
		"""));
		
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setIp("127.0.0.1") //
						.build()) //
				.next(new TestCase("Authentication enabled warning") //
						.output(ShellyCommon.ChannelId.AUTH_ENABLED_WARNING, true) //
						.output(ShellyCommon.ChannelId.DEVICE_GENERATION, "1")) //
				.deactivate();//
	}
}