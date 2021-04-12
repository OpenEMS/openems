package io.openems.edge.controller.api.backend;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class WsData extends io.openems.common.websocket.WsData {

	private final WebsocketClient parent;

	public WsData(WebsocketClient parent) {
		this.parent = parent;
	}

	@Override
	public String toString() {
		return "BackendApi.WsData []";
	}

	@Override
	protected ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
			TimeUnit unit) {
		return this.parent.scheduleWithFixedDelay(command, initialDelay, delay, unit);
	}

}
