package io.openems.edge.controller.api.backend;

import java.net.Proxy.Type;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.websocket.DummyWebsocketServer;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyCycle;

public class ControllerApiBackendImplTest {

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
					.addReference("resendHistoricDataWorkerFactory", new DummyResendHistoricDataWorkerFactory()) //
					.addReference("requestHandlerFactory", new DummyBackendOnRequestFactory()) //
					.addReference("oem", new DummyOpenemsEdgeOem()) //
					.addComponent(new DummySum()) //
					.activate(MyConfig.create() //
							.setId("ctrl0") //
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
