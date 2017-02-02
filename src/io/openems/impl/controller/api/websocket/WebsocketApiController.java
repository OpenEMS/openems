package io.openems.impl.controller.api.websocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsException;
import io.openems.core.ThingRepository;

@ThingInfo("Websocket-API (z. B. für Weboberfläche)")
public class WebsocketApiController extends Controller implements ChannelChangeListener {

	private volatile WebsocketServer websocketServer = null;

	@ConfigInfo(title = "Sets the websocket port", type = Integer.class)
	public final ConfigChannel<Integer> port = new ConfigChannel<Integer>("port", this).defaultValue(8085)
			.addChangeListener(this);

	private final AtomicReference<Optional<Long>> manualP = new AtomicReference<Optional<Long>>(Optional.empty());
	private final AtomicReference<Optional<Long>> manualQ = new AtomicReference<Optional<Long>>(Optional.empty());
	private HashMap<String, String> lastMessages = new HashMap<>();
	private final ThingRepository thingRepository;

	public WebsocketApiController() {
		super();
		this.thingRepository = ThingRepository.getInstance();
	}

	public WebsocketApiController(String thingId) {
		super(thingId);
		this.thingRepository = ThingRepository.getInstance();
	}

	@Override
	public void run() {
		// Start Websocket-Api server
		if (websocketServer == null && port.valueOptional().isPresent()) {
			try {
				websocketServer = new WebsocketServer(this, port.valueOptional().get());
				websocketServer.start();
				log.info("Websocket-Api started on port [" + port.valueOptional().orElse(0) + "].");
			} catch (Exception e) {
				log.error(e.getMessage() + ": " + e.getCause());
			}
		}

		try {
			Optional<Long> pOptional = manualP.get();
			Optional<Long> qOptional = manualQ.get();
			if (pOptional.isPresent() && qOptional.isPresent()) {
				long p = pOptional.get();
				long q = qOptional.get();
				String message = "P=" + p + ",Q=" + q;
				for (DeviceNature nature : thingRepository.getDeviceNatures()) {
					if (nature instanceof EssNature) {
						if (nature instanceof AsymmetricEssNature) {
							AsymmetricEssNature e = (AsymmetricEssNature) nature;
							e.setActivePowerL1().pushWrite(p / 3);
							e.setActivePowerL2().pushWrite(p / 3);
							e.setActivePowerL3().pushWrite(p / 3);
							e.setReactivePowerL1().pushWrite(q / 3);
							e.setReactivePowerL2().pushWrite(q / 3);
							e.setReactivePowerL3().pushWrite(q / 3);
						} else if (nature instanceof SymmetricEssNature) {
							SymmetricEssNature e = (SymmetricEssNature) nature;
							e.setActivePower().pushWrite(p);
							e.setReactivePower().pushWrite(q);
						} else {
							throw new OpenemsException("Undefined EssNature.");
						}
						String lastMessage = lastMessages.get(nature.id());
						if (lastMessage == null || !lastMessage.equals(message)) {
							// TODO
							// ws.broadcastNotification(NotificationType.INFO,
							// "Leistungsvorgabe an [" + nature.id() + "] gesendet: " + message);
							// lastMessages.put(nature.id(), message);
						}
					}
				}
			}
		} catch (OpenemsException e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		if (channel.equals(port)) {
			if (this.websocketServer != null) {
				try {
					this.websocketServer.stop();
				} catch (IOException | InterruptedException e) {
					log.error("Error closing websocket on port [" + oldValue + "]: " + e.getMessage());
				}
			}
			this.websocketServer = null;
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
