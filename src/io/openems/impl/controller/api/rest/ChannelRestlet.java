package io.openems.impl.controller.api.rest;

import java.util.Map;
import java.util.Optional;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
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
import io.openems.core.ThingRepository;

public class ChannelRestlet extends Restlet {
	private static Logger log = LoggerFactory.getLogger(ChannelRestlet.class);

	private final ThingRepository thingRepository;

	public ChannelRestlet() {
		super();
		thingRepository = ThingRepository.getInstance();
	}

	@Override public void handle(Request request, Response response) {
		super.handle(request, response);

		Map<String, Object> attributes = request.getAttributes();
		String thingId = (String) attributes.get("thing");
		String channelId = (String) attributes.get("channel");

		if (request.getMethod() == Method.GET) {
			Representation entity = getValue(thingId, channelId);
			response.setEntity(entity);

		} else if (request.getMethod() == Method.POST) {
			JsonParser parser = new JsonParser();
			String httpPost = request.getEntityAsText();
			JsonObject jHttpPost = parser.parse(httpPost).getAsJsonObject();
			setValue(thingId, channelId, jHttpPost);
		}
	}

	/**
	 * handle HTTP GET request
	 *
	 * @param thingId
	 * @param channelId
	 * @return
	 */
	private Representation getValue(String thingId, String channelId) {
		Optional<Channel> channelOptional = thingRepository.getChannel(thingId, channelId);
		if (channelOptional.isPresent()) {
			// get channel value
			Channel channel = channelOptional.get();
			try {
				return new StringRepresentation(channel.toJsonObject().toString(), MediaType.APPLICATION_JSON);
			} catch (NotImplementedException e) {
				throw new ResourceException(Status.SERVER_ERROR_NOT_IMPLEMENTED, e);
			}
		} else {
			// Channel not found
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
		}
	}

	/**
	 * handle HTTP POST request
	 *
	 * @param thingId
	 * @param channelId
	 * @param jHttpPost
	 */
	private void setValue(String thingId, String channelId, JsonObject jHttpPost) {
		// get channel
		Optional<Channel> channelOptional = thingRepository.getChannel(thingId, channelId);
		if (!channelOptional.isPresent() || !(channelOptional.get() instanceof WriteChannel<?>)) {
			throw new ResourceException(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
		}
		WriteChannel<?> channel = (WriteChannel<?>) channelOptional.get();
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
