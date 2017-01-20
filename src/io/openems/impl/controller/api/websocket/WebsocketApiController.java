package io.openems.impl.controller.api.websocket;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;

@ThingInfo("Websocket-API (z. B. für Weboberfläche)")
public class WebsocketApiController extends Controller implements ChannelChangeListener {

	private volatile WebsocketServer ws = null;

	@ConfigInfo(title = "Sets the websocket port", type = Integer.class)
	public final ConfigChannel<Integer> port = new ConfigChannel<Integer>("port", this).defaultValue(8085)
			.addChangeListener(this);

	private final AtomicReference<Optional<Long>> manualP = new AtomicReference<Optional<Long>>(Optional.empty());
	private final AtomicReference<Optional<Long>> manualQ = new AtomicReference<Optional<Long>>(Optional.empty());
	private String lastMessage = null;

	public WebsocketApiController() {
		super();
	}

	public WebsocketApiController(String thingId) {
		super(thingId);
	}

	@Override
	public void run() {
		// Start Websocket-Api server
		if (ws == null && port.valueOptional().isPresent()) {
			try {
				ws = new WebsocketServer(this, port.valueOptional().get());
				ws.start();
				log.info("Websocket-Api started on port [" + port.valueOptional().orElse(0) + "].");
			} catch (Exception e) {
				log.error(e.getMessage() + ": " + e.getCause());
			}
		}
		// TODO needs awareness for which essDevice to set...
		Optional<Long> p = manualP.get();
		Optional<Long> q = manualQ.get();
		if (p.isPresent() && q.isPresent()) {
			// TODO get the channel via ThingRepository!
			/*
			 * if (esss.valueOptional().isPresent()) {
			 * for (Ess ess : esss.value()) {
			 * String message = "P=" + p.get() + ",Q=" + q.get();
			 * if (!message.equals(lastMessage)) {
			 * ws.broadcastNotification(NotificationType.INFO,
			 * "Leistungsvorgabe an [" + ess.id() + "] gesendet: " + message);
			 * }
			 * lastMessage = message;
			 * ess.setPower(p.get(), q.get());
			 * }
			 * }
			 */
		}
	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
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

	protected void setManualPQ(long p, long q) {
		this.manualP.set(Optional.of(p));
		this.manualQ.set(Optional.of(q));
	}

	protected void resetManualPQ() {
		this.manualP.set(Optional.empty());
		this.manualQ.set(Optional.empty());
	}
}
