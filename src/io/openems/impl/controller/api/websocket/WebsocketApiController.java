package io.openems.impl.controller.api.websocket;

import java.io.IOException;
import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;

public class WebsocketApiController extends Controller implements ChannelChangeListener {

	private volatile WebsocketServer ws = null;

	public final ConfigChannel<Integer> port = new ConfigChannel<Integer>("port", this, Integer.class)
			.defaultValue(8085).changeListener(this);

	@Override public void run() {
		// Start Websocket-Api server
		if (ws == null && port.valueOptional().isPresent()) {
			try {
				ws = new WebsocketServer(port.valueOptional().get());
				ws.start();
				log.info("Websocket-Api started on port [" + port.valueOptional().orElse(0) + "].");
			} catch (Exception e) {
				log.error(e.getMessage() + ": " + e.getCause());
			}
		}
	}

	@Override public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		if (channel.equals(port)) {
			if (this.ws != null) {
				try {
					this.ws.stop();
				} catch (IOException | InterruptedException e) {
					log.error("Error closing websocket on port [" + oldValue + "]: " + e.getMessage());
				}
			}
			this.ws = null;
		}
	}
}
