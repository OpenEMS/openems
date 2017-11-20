/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016 FENECON GmbH and contributors
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
package io.openems.impl.controller.api.rest.route;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.exception.NotImplementedException;
import io.openems.common.session.Role;
import io.openems.common.websocket.Notification;
import io.openems.core.ThingRepository;
import io.openems.core.utilities.api.ApiWorker;
import io.openems.core.utilities.api.WriteJsonObject;
import io.openems.core.utilities.api.WriteObject;
import io.openems.impl.controller.api.rest.OpenemsRestlet;

public class ChannelRestlet extends OpenemsRestlet {

	private final Logger log = LoggerFactory.getLogger(ChannelRestlet.class);
	private final ThingRepository thingRepository;
	private final ApiWorker apiWorker;

	public ChannelRestlet(ApiWorker apiWorker) {
		super();
		this.apiWorker = apiWorker;
		thingRepository = ThingRepository.getInstance();
	}

	@Override
	public void handle(Request request, Response response) {
		super.handle(request, response);

		// check general permission
		if (isAuthenticatedAsRole(request, Role.GUEST)) {
			// pfff... it's only a "GUEST"! Deny anything but GET requests
			if (!request.getMethod().equals(Method.GET)) {
				throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED);
			}
		}

		// get request attributes
		Map<String, Object> attributes = request.getAttributes();
		String thingId = (String) attributes.get("thing");
		String channelId = (String) attributes.get("channel");

		// get channel
		Channel channel;
		Optional<Channel> channelOptional = thingRepository.getChannel(thingId, channelId);
		if (channelOptional.isPresent()) {
			// get channel value
			channel = channelOptional.get();
		} else {
			// Channel not found
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
		}

		// call handler methods
		if (request.getMethod().equals(Method.GET)) {
			// check read permission
			assertAllowed(request, channel.readRoles());

			Representation entity = getValue(channel);
			response.setEntity(entity);

		} else if (request.getMethod().equals(Method.POST)) {
			// check write permissions
			assertAllowed(request, channel.writeRoles());

			JsonParser parser = new JsonParser();
			String httpPost = request.getEntityAsText();
			JsonObject jHttpPost = parser.parse(httpPost).getAsJsonObject();
			setValue(channel, jHttpPost);
		}
	}

	private void assertAllowed(Request request, Set<Role> channelRoles) throws ResourceException {
		boolean allowed = false;
		for (Role role : channelRoles) {
			if (isAuthenticatedAsRole(request, role)) {
				allowed = true;
				break;
			}
		}
		if (!allowed) {
			throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED);
		}
	}

	/**
	 * handle HTTP GET request
	 *
	 * @param thingId
	 * @param channelId
	 * @return
	 */
	private Representation getValue(Channel channel) {
		try {
			return new StringRepresentation(channel.toJsonObject().toString(), MediaType.APPLICATION_JSON);
		} catch (NotImplementedException e) {
			throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED, e);
		}
	}

	/**
	 * handle HTTP POST request
	 *
	 * @param thingId
	 * @param channelId
	 * @param jHttpPost
	 */
	private void setValue(Channel channel, JsonObject jHttpPost) {
		// check for writable channel
		if (!(channel instanceof WriteChannel<?>)) {
			throw new ResourceException(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
		}

		// parse value
		JsonElement jValue;
		if (jHttpPost.has("value")) {
			jValue = jHttpPost.get("value");
		} else {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Value is missing");
		}

		// set channel value
		if (channel instanceof ConfigChannel<?>) {

			// is a ConfigChannel
			ConfigChannel<?> configChannel = (ConfigChannel<?>) channel;
			try {
				configChannel.updateValue(jValue, true);
				log.info("Updated Channel [" + channel.address() + "] to value [" + jValue.toString() + "].");
			} catch (NotImplementedException e) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Conversion not implemented");
			}

		} else if (channel instanceof WriteChannel<?>) {
			/*
			 * WriteChannel
			 */
			WriteChannel<?> writeChannel = (WriteChannel<?>) channel;
			WriteObject writeObject = new WriteJsonObject(jValue).onFirstSuccess(() -> {
				Notification.EDGE_CHANNEL_UPDATE_SUCCESS.writeToLog(log, "set " + channel.address() + " => " + jValue);
			}).onFirstError((e) -> {
				Notification.EDGE_CHANNEL_UPDATE_FAILED.writeToLog(log, "set " + channel.address() + " => " + jValue);
			}).onTimeout(() -> {
				Notification.EDGE_CHANNEL_UPDATE_TIMEOUT.writeToLog(log, "set " + channel.address() + " => " + jValue);
			});
			this.apiWorker.addValue(writeChannel, writeObject);
		}
	}
}
