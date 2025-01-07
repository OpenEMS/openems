package io.openems.edge.io.shelly.shelly25;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class IoShelly25ImplTest {

	@Test
	public void test() throws Exception {
		final var sut = new IoShelly25Impl();
		final var httpTestBundle = new DummyBridgeHttpBundle();
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
													"overtemperature": false,
													"overpower": false
												},
												{
													"ison": false,
													"overtemperature": true,
													"overpower": true
												}
											]
										}
									"""));
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("x -", sut.debugLog())) //

						.output(IoShelly25.ChannelId.RELAY_1, null) // expecting WriteValue
						.output(IoShelly25.ChannelId.RELAY_1_OVERPOWER, false) //
						.output(IoShelly25.ChannelId.RELAY_1_OVERTEMP, false) //
						.output(IoShelly25.ChannelId.RELAY_2, null) // expecting WriteValue
						.output(IoShelly25.ChannelId.RELAY_2_OVERPOWER, true) //
						.output(IoShelly25.ChannelId.RELAY_2_OVERTEMP, true) //
						.output(IoShelly25.ChannelId.SLAVE_COMMUNICATION_FAILED, false)) //

				// Test case for an invalid JSON response
				.next(new TestCase("Invalid read response") //
						.onBeforeProcessImage(() -> { //
							httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("? ?", sut.debugLog())) //

						.output(IoShelly25.ChannelId.RELAY_1, null) // expecting WriteValue
						.output(IoShelly25.ChannelId.RELAY_1_OVERPOWER, null) //
						.output(IoShelly25.ChannelId.RELAY_1_OVERTEMP, null) //
						.output(IoShelly25.ChannelId.RELAY_2, null) // expecting WriteValue
						.output(IoShelly25.ChannelId.RELAY_2_OVERPOWER, null) //
						.output(IoShelly25.ChannelId.RELAY_2_OVERTEMP, null) //
						.output(IoShelly25.ChannelId.SLAVE_COMMUNICATION_FAILED, true)) //

				.deactivate();//
	}
}