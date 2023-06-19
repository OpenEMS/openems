package io.openems.edge.bridge.modbus.api.worker.internal;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class WaitDelayHandlerTest {

	@Test
	public void test() {
		var ticker = new FakeTicker();
		var sut = new WaitDelayHandler(ticker, () -> {
		});

		sut.onFinished();
		ticker.advance(100, TimeUnit.MILLISECONDS);
		sut.onBeforeProcessImage();

		sut.onFinished();
		ticker.advance(100, TimeUnit.MILLISECONDS);
		sut.onBeforeProcessImage();
	}

}
