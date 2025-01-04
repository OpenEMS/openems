package io.openems.edge.io.shelly.shellypro3;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class IoShellyPro3ImplTest {

	@Test
	public void test() throws Exception {
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var sut = new IoShellyPro3Impl();
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
						.output(IoShellyPro3.ChannelId.SLAVE_COMMUNICATION_FAILED, false)) //

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
						.output(IoShellyPro3.ChannelId.SLAVE_COMMUNICATION_FAILED, true)) //

				.deactivate(); //
	}
}
