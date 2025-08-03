package io.openems.edge.io.shelly.shelly1;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class IoShelly1ImplTest {

	@Test
	public void test() throws Exception {
		final var sut = new IoShelly1Impl();
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
						.output(IoShelly1.ChannelId.SLAVE_COMMUNICATION_FAILED, false)) //

				// Test case for an invalid JSON response
				.next(new TestCase("Invalid read response") //
						.onBeforeProcessImage(() -> { //
							httpTestBundle.forceNextFailedResult(HttpError.ResponseError.notFound());
							httpTestBundle.triggerNextCycle();
						}) //
						.onAfterProcessImage(() -> assertEquals("?", sut.debugLog())) //

						.output(IoShelly1.ChannelId.RELAY, null) // expecting WriteValue
						.output(IoShelly1.ChannelId.SLAVE_COMMUNICATION_FAILED, true)) //

				.deactivate();//
	}
}