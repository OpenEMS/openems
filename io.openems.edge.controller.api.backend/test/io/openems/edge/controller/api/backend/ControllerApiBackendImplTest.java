package io.openems.edge.controller.api.backend;

import java.net.Proxy.Type;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.websocket.DummyWebsocketServer;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyCycle;
import io.openems.edge.common.test.TimeLeapClock;

public class ControllerApiBackendImplTest {

	private static final String CTRL_ID = "ctrl0";

	@Test
	public void test() throws Exception {

		try (final var server = DummyWebsocketServer.create() //
				.build()) {
			server.start();

			// block until Port is not anymore zero
			int port;
			do {
				Thread.sleep(500);
				port = server.getPort();
			} while (port == 0);

			final var clock = new TimeLeapClock(
					Instant.ofEpochSecond(1577836800L) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);
			final var sut = new ControllerApiBackendImpl();
			new ComponentTest(sut) //
					.addReference("componentManager", new DummyComponentManager(clock)) //
					.addReference("cycle", new DummyCycle(1000)) //
					.addReference("resendHistoricDataWorker", new ResendHistoricDataWorker()) //
					.addComponent(new DummySum()) //
					.activate(MyConfig.create() //
							.setId(CTRL_ID) //
							.setUri("ws://localhost:" + port) //
							.setApikey("12345") //
							.setProxyType(Type.DIRECT) //
							.setProxyAddress("") //
							.setPersistencePriority(PersistencePriority.HIGH) //
							.setAggregationPriority(PersistencePriority.VERY_LOW) //
							.setResendPriority(PersistencePriority.MEDIUM) //
							.build());

			// Stop connection
			sut.deactivate();
			server.stop();
		}
	}

}
