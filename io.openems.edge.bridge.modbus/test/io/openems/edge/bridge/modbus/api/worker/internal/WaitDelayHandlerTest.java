package io.openems.edge.bridge.modbus.api.worker.internal;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.openems.edge.bridge.modbus.api.LogVerbosity;

public class WaitDelayHandlerTest {

	@Test
	public void test() {
		var ticker = new FakeTicker();
		var sut = new WaitDelayHandler(LogVerbosity.DEV_REFACTORING, ticker, () -> {
		});

		sut.onFinished();
		ticker.advance(100, TimeUnit.MILLISECONDS);
		sut.onBeforeProcessImage(false);

		sut.onFinished();
		ticker.advance(100, TimeUnit.MILLISECONDS);
		sut.onBeforeProcessImage(false);
	}

}
