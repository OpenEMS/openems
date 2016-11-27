package io.openems.impl.controller.api.rest;

import java.util.Map;
import java.util.Optional;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.ResourceException;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.exception.NotImplementedException;
import io.openems.api.security.OpenemsRole;
import io.openems.core.ThingRepository;

public class ChannelRestlet extends OpenemsRestlet {

	private final ThingRepository thingRepository;

	public ChannelRestlet() {
		super();
		thingRepository = ThingRepository.getInstance();
	}

	@Override public void handle(Request request, Response response) {
		super.handle(request, response);

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

		// check permission
		if (!channel.roles().isEmpty()) {
			boolean allowed = false;
			for (OpenemsRole role : channel.roles()) {
				if (isInRole(request, role)) {
					allowed = true;
					break;
				}
			}
			if (!allowed) {
				throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED);
			}
		}

		// call handler methods
		if (request.getMethod().equals(Method.GET)) {
			Representation entity = getValue(channel);
			response.setEntity(entity);

		} else if (request.getMethod().equals(Method.POST)) {
			JsonParser parser = new JsonParser();
			String httpPost = request.getEntityAsText();
			JsonObject jHttpPost = parser.parse(httpPost).getAsJsonObject();
			setValue(request, channel, jHttpPost);
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
	private void setValue(Request request, Channel channel, JsonObject jHttpPost) {
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
				log.info("Updated Channel " + jValue.toString());
			} catch (NotImplementedException e) {
				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Conversion not implemented");
			}

		} else {
			// is a WriteChannel
		}
	}
}
