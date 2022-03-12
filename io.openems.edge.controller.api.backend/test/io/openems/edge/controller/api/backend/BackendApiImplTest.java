package io.openems.edge.controller.api.backend;

import java.net.Proxy.Type;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.java_websocket.WebSocket;
import org.junit.Test;

import com.google.gson.JsonObject;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingBiConsumer;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.DummyWebsocketServer;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyCycle;
import io.openems.edge.common.test.TimeLeapClock;

public class BackendApiImplTest {

	private static final String CTRL_ID = "ctrl0";
	private static final String SUM_ID = Sum.SINGLETON_COMPONENT_ID;

	private static final ChannelAddress SUM_GRID_ACTIVE_POWER = new ChannelAddress(SUM_ID,
			Sum.ChannelId.GRID_ACTIVE_POWER.id());
	private static final ChannelAddress SUM_PRODUCTION_ACTIVE_POWER = new ChannelAddress(SUM_ID,
			Sum.ChannelId.PRODUCTION_ACTIVE_POWER.id());

	private static class TimestampedDataNotificationHandler implements io.openems.common.websocket.OnNotification {

		protected boolean wasCalled = false;

		private ThrowingBiConsumer<Long, JsonObject, OpenemsNamedException> callback = (timestamp, values) -> {
		};

		public TimestampedDataNotificationHandler() {
		}

		public void onNotification(ThrowingBiConsumer<Long, JsonObject, OpenemsNamedException> callback) {
			this.wasCalled = false;
			this.callback = callback;
		}

		public void waitForCallback(int timeout) throws InterruptedException, OpenemsException {
			var start = Instant.now();
			while (!this.wasCalled) {
				if (Duration.between(start, Instant.now()).getSeconds() > timeout) {
					throw new OpenemsException("Timeout [" + timeout + "s]");
				}
				Thread.sleep(100);
			}
		}

		@Override
		public void run(WebSocket websocket, JsonrpcNotification notification) throws OpenemsNamedException {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (notification.getMethod().equals(TimestampedDataNotification.METHOD)) {
				for (String timestamp : notification.getParams().keySet()) {
					var values = JsonUtils.getAsJsonObject(notification.getParams().get(timestamp));
					this.wasCalled = true;
					this.callback.accept(Long.valueOf(timestamp), values);
				}
			}
		}

	}

	@Test
	public void test() throws Exception {
		var handler = new TimestampedDataNotificationHandler();

		try (final var server = DummyWebsocketServer.create() //
				.onNotification(handler) //
				.build()) {
			var port = server.startBlocking();

			final var clock = new TimeLeapClock(
					Instant.ofEpochSecond(1577836800L) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);
			final var sut = new BackendApiImpl();
			var test = new ComponentTest(sut) //
					.addReference("componentManager", new DummyComponentManager(clock)) //
					.addReference("cycle", new DummyCycle(1000)) //
					.addComponent(new DummySum()) //
					.activate(MyConfig.create() //
							.setId(CTRL_ID) //
							.setUri("ws://localhost:" + port) //
							.setApikey("12345") //
							.setProxyType(Type.DIRECT) //
							.setProxyAddress("") //
							.setPersistencePriority(PersistencePriority.VERY_LOW) //
							.build());

			while (!sut.isConnected()) {
				Thread.sleep(100);
			}

			// All Values initially
			handler.onNotification((timestamp, values) -> {
				assertTrue(values.size() > 40); // all values
			});
			test.next(new TestCase()); //
			handler.waitForCallback(5000);

			// Only changed value
			handler.onNotification((timestamp, values) -> {
				assertTrue(values.size() == 1); // exactly one value
				assertEquals(Integer.valueOf(1000),
						JsonUtils.getAsOptionalInt(values, SUM_GRID_ACTIVE_POWER.toString()).get());
			});
			test.next(new TestCase() //
					.input(SUM_GRID_ACTIVE_POWER, 1000) //
			);
			handler.waitForCallback(5000);

			// Only changed value
			handler.onNotification((timestamp, values) -> {
				assertTrue(values.size() == 1); // exactly one value
				assertEquals(Integer.valueOf(2000),
						JsonUtils.getAsOptionalInt(values, SUM_PRODUCTION_ACTIVE_POWER.toString()).get());
			});
			test.next(new TestCase() //
					.input(SUM_PRODUCTION_ACTIVE_POWER, 2000) //
			);
			handler.waitForCallback(5000);

			// All values after 5 minutes
			handler.onNotification((timestamp, values) -> {
				assertTrue(values.size() > 40); // all values
			});
			test.next(new TestCase() //
					.timeleap(clock, 6, ChronoUnit.MINUTES));
			handler.waitForCallback(5000);
		}
	}

	private static void assertTrue(boolean condition) throws OpenemsException {
		try {
			org.junit.Assert.assertTrue(condition);
		} catch (AssertionError e) {
			System.err.println("AssertionError: " + e.getMessage());
			System.exit(1);
		}
	}

	private static void assertEquals(Object expected, Object actual) throws OpenemsException {
		try {
			org.junit.Assert.assertEquals(expected, actual);
		} catch (AssertionError e) {
			System.err.println("AssertionError: " + e.getMessage());
			System.exit(1);
		}
	}

}
