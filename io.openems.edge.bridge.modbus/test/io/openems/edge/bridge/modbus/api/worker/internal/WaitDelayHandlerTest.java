package io.openems.edge.bridge.modbus.api.worker.internal;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.google.common.collect.Lists;

public class WaitDelayHandlerTest {

	private static Runnable NO_OP = () -> {
	};

	@Test
	public void test() {
		var ticker = new FakeTicker();
		var sut = new WaitDelayHandler(ticker, NO_OP);

		sut.onFinished();
		ticker.advance(100, TimeUnit.MILLISECONDS);
		sut.onBeforeProcessImage();

		sut.onFinished();
		ticker.advance(100, TimeUnit.MILLISECONDS);
		sut.onBeforeProcessImage();
	}

	@Test
	public void testGenerateWaitDelayTask() {
		// 10 - BUFFER_MS -> 0
		var possibleDelays = Lists.newArrayList(10L, 50L, 100L);
		assertEquals(0, WaitDelayHandler.generateWaitDelayTask(possibleDelays, NO_OP).initialDelay);

		// 30 - BUFFER_MS -> 10
		possibleDelays = Lists.newArrayList(30L, 50L, 50L);
		assertEquals(10, WaitDelayHandler.generateWaitDelayTask(possibleDelays, NO_OP).initialDelay);

		// Empty
		possibleDelays = Lists.newArrayList();
		assertEquals(0, WaitDelayHandler.generateWaitDelayTask(possibleDelays, NO_OP).initialDelay);
	}
}
