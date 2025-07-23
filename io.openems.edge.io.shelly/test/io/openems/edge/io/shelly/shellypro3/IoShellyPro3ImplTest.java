package io.openems.edge.io.shelly.shellypro3;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.io.shelly.common.ShellyCommon;

public class IoShellyPro3ImplTest {

	@Test
	public void test() throws Exception {
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var sut = new IoShellyPro3Impl();
		
		// Pre-set the response for the /shelly endpoint that will be called during activation
		httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
			{
				"name": "shellypro3-test",
				"id": "shellypro3-12345",
				"mac": "AA:BB:CC:DD:EE:FF",
				"model": "SPSW-003XE16EU",
				"gen": 2,
				"fw_id": "20230912-114516/v1.14.0-gcb84623",
				"ver": "1.14.0",
				"app": "Pro3",
				"auth_en": false,
				"auth_domain": "shellypro3-12345"
			}
		"""));
		
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.activate(MyConfig.create() //
						.setId("io0") //
						.setIp("127.0.0.1") //
						.build()) //

				// Test case for successful JSON responses for all relays
				.next(new TestCase("Successful read response for all relays") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
									    {
									        "id": 0,
									        "source": "HTTP",
									        "output": true
									    }
									"""));
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
									    {
									        "id": 1,
									        "source": "HTTP",
									        "output": false
									    }
									"""));
							httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
									    {
									        "id": 2,
									        "source": "HTTP",
									        "output": true
									    }
									"""));
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("x x -", sut.debugLog()))

						.output(IoShellyPro3.ChannelId.RELAY_1, null) // expecting WriteValue
						.output(IoShellyPro3.ChannelId.RELAY_2, null) // expecting WriteValue
						.output(IoShellyPro3.ChannelId.RELAY_3, null) // expecting WriteValue
						.output(IoShellyPro3.ChannelId.SLAVE_COMMUNICATION_FAILED, false) //
						.output(ShellyCommon.ChannelId.AUTH_ENABLED_WARNING, false) //
						.output(ShellyCommon.ChannelId.DEVICE_GENERATION, "2")) //

				// Test case for an invalid JSON response
				.next(new TestCase("Invalid read response for all relays") //
						.onBeforeProcessImage(() -> {
							httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("? ? ?", sut.debugLog()))

						.output(IoShellyPro3.ChannelId.RELAY_1, null) //
						.output(IoShellyPro3.ChannelId.RELAY_2, null) //
						.output(IoShellyPro3.ChannelId.RELAY_3, null) //
						.output(IoShellyPro3.ChannelId.SLAVE_COMMUNICATION_FAILED, true) //
						.output(ShellyCommon.ChannelId.AUTH_ENABLED_WARNING, false) //
						.output(ShellyCommon.ChannelId.DEVICE_GENERATION, "2")) //

				.deactivate(); //
	}
	
	@Test
	public void testAuthenticationWarning() throws Exception {
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var sut = new IoShellyPro3Impl();
		
		// Pre-set the response for the /shelly endpoint with authentication enabled
		httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok("""
			{
				"name": "shellypro3-test",
				"id": "shellypro3-12345",
				"mac": "AA:BB:CC:DD:EE:FF",
				"model": "SPSW-003XE16EU",
				"gen": 2,
				"fw_id": "20230912-114516/v1.14.0-gcb84623",
				"ver": "1.14.0",
				"app": "Pro3",
				"auth_en": true,
				"auth_domain": "shellypro3-12345"
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
						.output(ShellyCommon.ChannelId.DEVICE_GENERATION, "2")) //
				.deactivate();//
	}
}
