package io.openems.edge.bridge.modbus.api.worker.internal;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.Test;

import com.google.common.collect.Lists;

public class WaitDelayHandlerTest {

	private static Runnable NO_OP = () -> {
	};
	private static Consumer<Long> CYCLE_DELAY = (value) -> {
	};

	@Test
	public void test() {
		var ticker = new FakeTicker();
		var sut = new WaitDelayHandler(ticker, NO_OP, CYCLE_DELAY);

		sut.onFinished();
		ticker.advance(100, TimeUnit.MILLISECONDS);
		sut.onBeforeProcessImage(false);

		sut.onFinished();
		ticker.advance(100, TimeUnit.MILLISECONDS);
		sut.onBeforeProcessImage(false);
	}

	@Test
	public void testGenerateWaitDelayTask() {
		// 12 - BUFFER_MS -> 0
		var possibleDelays = Lists.newArrayList(5L, 20L, 100L);
		assertEquals(0, WaitDelayHandler.generateWaitDelayTask(possibleDelays, NO_OP).initialDelay);

		// 40 - BUFFER_MS -> 20
		possibleDelays = Lists.newArrayList(30L, 50L, 50L);
		assertEquals(20, WaitDelayHandler.generateWaitDelayTask(possibleDelays, NO_OP).initialDelay);

		// Empty
		possibleDelays = Lists.newArrayList();
		assertEquals(0, WaitDelayHandler.generateWaitDelayTask(possibleDelays, NO_OP).initialDelay);
	}
}
