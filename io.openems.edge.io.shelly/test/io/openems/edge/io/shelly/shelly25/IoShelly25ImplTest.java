package io.openems.edge.io.shelly.shelly25;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class IoShelly25ImplTest {

	private static final String COMPONENT_ID = "io25";

	private static final ChannelAddress RELAY_1 = new ChannelAddress(COMPONENT_ID, "Relay1");
	private static final ChannelAddress RELAY_2 = new ChannelAddress(COMPONENT_ID, "Relay2");
	private static final ChannelAddress RELAY_1_OVERTEMP = new ChannelAddress(COMPONENT_ID, "Relay1Overtemp");
	private static final ChannelAddress RELAY_2_OVERTEMP = new ChannelAddress(COMPONENT_ID, "Relay2Overtemp");
	private static final ChannelAddress RELAY_1_OVERPOWER = new ChannelAddress(COMPONENT_ID, "Relay1Overpower");
	private static final ChannelAddress RELAY_2_OVERPOWER = new ChannelAddress(COMPONENT_ID, "Relay2Overpower");
	private static final ChannelAddress SLAVE_COMMUNICATION_FAILED = new ChannelAddress(COMPONENT_ID,
			"SlaveCommunicationFailed");

	@Test
	public void test() throws Exception {
		final var httpTestBundle = new DummyBridgeHttpBundle();

		final var sut = new IoShelly25Impl();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setIp("127.0.0.1") //
						.build()) //

				// Test case for an invalid JSON response
				.next(new TestCase("Invalid read response") //
						.onBeforeControllersCallbacks(() -> { //
							assertEquals("Expected initial debug log", sut.debugLog(), "?|?"); //
							httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
							httpTestBundle.triggerNextCycle();
						}) //
						.output(RELAY_1, null) //
						.output(RELAY_2, null) //
						.output(RELAY_1_OVERTEMP, null) //
						.output(RELAY_2_OVERTEMP, null) //
						.output(RELAY_1_OVERPOWER, null) //
						.output(RELAY_2_OVERPOWER, null) //
						.output(SLAVE_COMMUNICATION_FAILED, true)) //

				.deactivate();//
	}
}
