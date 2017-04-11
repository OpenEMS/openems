/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.impl.controller.api.websocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.AsymmetricEssNature;
import io.openems.api.device.nature.ess.SymmetricEssNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsException;
import io.openems.api.thing.Thing;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.websocket.Notification;
import io.openems.core.utilities.websocket.NotificationType;

class PQ {
	public final long p;
	public final long q;
	public boolean firstRun = true;

	public PQ(long p, long q) {
		this.p = p;
		this.q = q;
	}
}

@ThingInfo(title = "Websocket-API", description = "Required by OpenEMS-UI.")
public class WebsocketApiController extends Controller implements ChannelChangeListener {

	/*
	 * Constructors
	 */
	public WebsocketApiController() {
		super();
		this.thingRepository = ThingRepository.getInstance();
	}

	public WebsocketApiController(String thingId) {
		super(thingId);
		this.thingRepository = ThingRepository.getInstance();
	}

	/*
	 * Config
	 */
	@ConfigInfo(title = "Port", description = "Sets the port of the Websocket-Api Server.", type = Integer.class, defaultValue = "8085")
	public final ConfigChannel<Integer> port = new ConfigChannel<Integer>("port", this).addChangeListener(this);

	/*
	 * Fields
	 */
	private final ConcurrentHashMap<String, PQ> manualpq = new ConcurrentHashMap<>();
	private final ThingRepository thingRepository;
	private volatile WebsocketServer websocketServer = null;
	private HashMap<String, String> lastMessages = new HashMap<>();

	/*
	 * Methods
	 */
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

		manualpq.forEach((thingId, pq) -> {
			try {
				Thing thing = thingRepository.getThing(thingId);
				if (thing == null) {
					throw new OpenemsException("Ess[" + thingId + "] is not registered.");
				} else if (thing instanceof AsymmetricEssNature) {
					AsymmetricEssNature e = (AsymmetricEssNature) thing;
					long p = pq.p / 3;
					long q = pq.q / 3;
					e.setActivePowerL1().pushWrite(p);
					e.setActivePowerL2().pushWrite(p);
					e.setActivePowerL3().pushWrite(p);
					e.setReactivePowerL1().pushWrite(q);
					e.setReactivePowerL2().pushWrite(q);
					e.setReactivePowerL3().pushWrite(q);
					if (pq.firstRun) {
						Notification.send(NotificationType.INFO, "Started manual PQ for Ess[" + thingId
								+ "]. Asymmetric output on each phase: p[+" + p + "], q[" + q + "]");
					}
				} else if (thing instanceof SymmetricEssNature) {
					SymmetricEssNature e = (SymmetricEssNature) thing;
					e.setActivePower().pushWrite(pq.p);
					e.setReactivePower().pushWrite(pq.q);
					if (pq.firstRun) {
						Notification.send(NotificationType.INFO, "Started manual PQ for Ess[" + thingId
								+ "]. Symmetric output: p[+" + pq.p + "], q[" + pq.q + "]");
					}
				} else {
					throw new OpenemsException("Ess[" + thingId + "] is not an Ess.");
				}
			} catch (OpenemsException e) {
				Notification.send(NotificationType.ERROR, e.getMessage());
			} finally {
				pq.firstRun = false;
			}
		});
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

	public void setManualPQ(String ess, long p, long q) {
		this.manualpq.put(ess, new PQ(p, q));
	}

	public void resetManualPQ(String ess) {
		this.manualpq.remove(ess);
	}
}
