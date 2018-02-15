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
package io.openems.impl.controller.api.rest;

import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsException;
import io.openems.core.utilities.api.ApiWorker;

@ThingInfo(title = "REST-Api", description = "Use for external access to OpenEMS.")
public class RestApiController extends Controller {

	private final ApiWorker apiWorker = new ApiWorker();
	private ThingStateChannels thingState = new ThingStateChannels(this);

	/*
	 * Constructors
	 */
	public RestApiController() {
		super();
	}

	public RestApiController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Port", description = "Sets the port of the REST-Api Server.", type = Integer.class, defaultValue = "8084")
	public final ConfigChannel<Integer> port = new ConfigChannel<Integer>("port", this);

	@ChannelInfo(title = "ChannelTimeout", description = "Sets the timeout for updates to channels.", type = Integer.class, defaultValue = ""
			+ ApiWorker.DEFAULT_TIMEOUT_SECONDS)
	public final ConfigChannel<Integer> channelTimeout = new ConfigChannel<Integer>("channelTimeout", this)
	.addChangeListener((Channel channel, Optional<?> newValue, Optional<?> oldValue) -> {
		if (newValue.isPresent() && Integer.parseInt(newValue.get().toString()) >= 0) {
			apiWorker.setTimeoutSeconds(Integer.parseInt(newValue.get().toString()));
		} else {
			apiWorker.setTimeoutSeconds(ApiWorker.DEFAULT_TIMEOUT_SECONDS);
		}
	});

	/*
	 * Methods
	 */
	@Override
	public void run() {
		// Start REST-Api server
		try {
			ComponentSingleton.getComponent(this.port, this.apiWorker);
		} catch (OpenemsException e) {
			log.error(e.getMessage() + ": " + e.getCause());
		}
		// API Worker
		this.apiWorker.run();
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}
}
