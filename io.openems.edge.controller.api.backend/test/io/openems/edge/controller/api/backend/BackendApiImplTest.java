package io.openems.edge.controller.api.backend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.Proxy.Type;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.function.BiConsumer;

import org.java_websocket.WebSocket;
import org.junit.Test;

import com.google.gson.JsonObject;

import io.openems.common.OpenemsConstants;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyCycle;
import io.openems.edge.common.test.TimeLeapClock;

public class BackendApiImplTest {

	private static final String CTRL_ID = "ctrl0";
	private static final ChannelAddress CTRL_UNABLE_TO_SEND = new ChannelAddress(CTRL_ID,
			BackendApi.ChannelId.UNABLE_TO_SEND.id());

	private static final String SUM_ID = OpenemsConstants.SUM_ID;
	private static final ChannelAddress SUM_GRID_ACTIVE_POWER = new ChannelAddress(SUM_ID,
			Sum.ChannelId.GRID_ACTIVE_POWER.id());
	private static final ChannelAddress SUM_PRODUCTION_ACTIVE_POWER = new ChannelAddress(SUM_ID,
			Sum.ChannelId.PRODUCTION_ACTIVE_POWER.id());

	private static class TimestampedDataNotificationHandler implements io.openems.common.websocket.OnNotification {

		protected boolean wasCalled = false;

		private BiConsumer<Long, JsonObject> callback = (timestamp, values) -> {
		};

		public TimestampedDataNotificationHandler() {
		}

		public void onNotification(BiConsumer<Long, JsonObject> callback) {
			this.wasCalled = false;
			this.callback = callback;
		}

		public void waitForCallback() throws InterruptedException {
			while (!this.wasCalled) {
				Thread.sleep(100);
			}
		}

		@Override
		public void run(WebSocket websocket, JsonrpcNotification notification) throws OpenemsNamedException {
			if (notification.getMethod().equals(TimestampedDataNotification.METHOD)) {
				for (String timestamp : notification.getParams().keySet()) {
					JsonObject values = JsonUtils.getAsJsonObject(notification.getParams().get(timestamp));
					this.callback.accept(Long.valueOf(timestamp), values);
					this.wasCalled = true;
					return;
				}
			}
		}

	}

	@Test
	public void test() throws Exception {
		TimestampedDataNotificationHandler handler = new TimestampedDataNotificationHandler();

		try (final DummyWebsocketServer server = DummyWebsocketServer.create() //
				.onNotification(handler) //
				.build()) {
			int port = server.startBlocking();

			final TimeLeapClock clock = new TimeLeapClock(
					Instant.ofEpochSecond(1577836800L) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);
			final BackendApiImpl sut = new BackendApiImpl();
			ComponentTest test = new ComponentTest(sut) //
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
				assertTrue(values.size() > 50); // all values
			});
			test.next(new TestCase()); //
			handler.waitForCallback();

			// Only changed value
			handler.onNotification((timestamp, values) -> {
				assertTrue(values.size() == 1); // exactly one value
				assertEquals(Integer.valueOf(1000),
						JsonUtils.getAsOptionalInt(values, SUM_GRID_ACTIVE_POWER.toString()).get());
			});
			test.next(new TestCase() //
					.input(SUM_GRID_ACTIVE_POWER, 1000) //
			);
			handler.waitForCallback();

			// Disconnect
			server.stop();

			test.next(new TestCase() //
					.input(SUM_GRID_ACTIVE_POWER, 1000) //
					.input(SUM_PRODUCTION_ACTIVE_POWER, 200)); //
//					.output(CTRL_UNABLE_TO_SEND, true));

			Thread.sleep(100);

			handler.onNotification((timestamp, values) -> {
				assertTrue(values.size() == 2); // exactly one value
				assertEquals(Integer.valueOf(1000),
						JsonUtils.getAsOptionalInt(values, SUM_GRID_ACTIVE_POWER.toString()).get());
				assertEquals(Integer.valueOf(300),
						JsonUtils.getAsOptionalInt(values, SUM_PRODUCTION_ACTIVE_POWER.toString()).get());
			});
			test.next(new TestCase() //
					.input(SUM_PRODUCTION_ACTIVE_POWER, 300)); //
			handler.waitForCallback();
		}
	}

}
