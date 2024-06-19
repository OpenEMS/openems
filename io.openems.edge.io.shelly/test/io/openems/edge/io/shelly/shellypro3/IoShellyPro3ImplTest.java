package io.openems.edge.io.shelly.shellypro3;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class IoShellyPro3ImplTest {

	private static final String COMPONENT_ID = "io25";

	private static final ChannelAddress RELAY_1 = new ChannelAddress(COMPONENT_ID, "Relay1");
	private static final ChannelAddress RELAY_2 = new ChannelAddress(COMPONENT_ID, "Relay2");
	private static final ChannelAddress RELAY_3 = new ChannelAddress(COMPONENT_ID, "Relay3");
	private static final ChannelAddress SLAVE_COMMUNICATION_FAILED = new ChannelAddress(COMPONENT_ID,
			"SlaveCommunicationFailed");

	@Test
	public void test() throws Exception {
		final var httpTestBundle = new DummyBridgeHttpBundle();

		final var sut = new IoShellyPro3Impl();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setIp("127.0.0.1") //
						.build()) //

				// Test case for successful JSON responses for all relays
				.next(new TestCase("Successful read response for all relays") //
						.onBeforeControllersCallbacks(() -> {
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
						.output(SLAVE_COMMUNICATION_FAILED, false)) //

				// Test case for an invalid JSON response
				.next(new TestCase("Invalid read response for all relays") //
						.onBeforeControllersCallbacks(() -> {
							assertEquals("Expected initial debug log", sut.debugLog(), "x|x|-");
							httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
							httpTestBundle.triggerNextCycle();
						}) //
						.output(RELAY_1, null)
						.output(RELAY_2, null)
						.output(RELAY_3, null)
						.output(SLAVE_COMMUNICATION_FAILED, true))
				.deactivate(); //
	}
}
