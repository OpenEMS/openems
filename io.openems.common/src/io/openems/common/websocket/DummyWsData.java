package io.openems.common.websocket;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class DummyWsData extends WsData {

	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1,
			new ThreadFactoryBuilder().setNameFormat("DummyWsData-%d").build());

	@Override
	public String toString() {
		return "DummyWsData[]";
	}

	@Override
	protected ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
			TimeUnit unit) {
		return this.executor.scheduleWithFixedDelay(command, initialDelay, delay, unit);
	}

}