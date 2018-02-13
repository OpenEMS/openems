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
import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.thingstate.ThingStateChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.core.utilities.api.ApiWorker;

@ThingInfo(title = "Websocket-API", description = "Required by OpenEMS-UI.")
public class WebsocketApiController extends Controller implements ChannelChangeListener {

	private final ApiWorker apiWorker = new ApiWorker();
	private ThingStateChannel thingState = new ThingStateChannel(this);

	/*
	 * Constructors
	 */
	public WebsocketApiController() {
		super();
	}

	public WebsocketApiController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Port", description = "Sets the port of the Websocket-Api Server.", type = Integer.class, defaultValue = "8085")
	public final ConfigChannel<Integer> port = new ConfigChannel<Integer>("port", this).addChangeListener(this);

	@ChannelInfo(title = "ChannelTimeout", description = "Sets the timeout for updates to channels.", type = Integer.class, defaultValue = ""
			+ ApiWorker.DEFAULT_TIMEOUT_SECONDS)
	public final ConfigChannel<Integer> channelTimeout = new ConfigChannel<Integer>("channelTimeout", this)
	.addChangeListener((Channel channel, Optional<?> newValue, Optional<?> oldValue) -> {
		if(newValue.isPresent() && Integer.parseInt(newValue.get().toString()) >= 0) {
			apiWorker.setTimeoutSeconds(Integer.parseInt(newValue.get().toString()));
		} else {
			apiWorker.setTimeoutSeconds(ApiWorker.DEFAULT_TIMEOUT_SECONDS);
		}
	});

	/*
	 * Fields
	 */
	private volatile WebsocketApiServer websocketApiServer = null;

	/*
	 * Methods
	 */
	@Override
	public void run() {
		// Start Websocket-Api server
		if (websocketApiServer == null && port.valueOptional().isPresent()) {
			try {
				websocketApiServer = new WebsocketApiServer(apiWorker, port.valueOptional().get());
				websocketApiServer.start();
				log.info("Websocket-Api started on port [" + port.valueOptional().orElse(0) + "].");
			} catch (Exception e) {
				log.error(e.getMessage() + ": " + e.getCause());
			}
		}
		// call AapiWorker
		this.apiWorker.run();
	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		if (channel.equals(port)) {
			if (this.websocketApiServer != null) {
				try {
					this.websocketApiServer.stop();
				} catch (IOException | InterruptedException e) {
					log.error("Error closing websocket on port [" + oldValue + "]: " + e.getMessage());
				}
			}
			this.websocketApiServer = null;
		}
	}

	/**
	 * Send a log entry to all connected websockets
	 *
	 * @param string
	 * @param timestamp
	 *
	 * @param jMessage
	 */
	public void broadcastLog(long timestamp, String level, String source, String message) {
		this.websocketApiServer.broadcastLog(timestamp, level, source, message);
	}

	@Override
	public ThingStateChannel getStateChannel() {
		return this.thingState;
	}
}
