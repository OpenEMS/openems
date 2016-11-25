package io.openems.impl.controller.api.rest;

import java.util.Optional;

import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
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

public class ChannelCurrentResource extends ServerResource {

	private static Logger log = LoggerFactory.getLogger(ChannelCurrentResource.class);

	private final ThingRepository thingRepository;

	public ChannelCurrentResource() {
		this.thingRepository = ThingRepository.getInstance();
	}

	@Get("json") public Representation getValue() {
		String thingId = (String) this.getRequestAttributes().get("thing");
		String channelId = (String) this.getRequestAttributes().get("channel");
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

	@Post("json") public void setValue(String httpPost) {
		// get channel
		String thingId = (String) this.getRequestAttributes().get("thing");
		String channelId = (String) this.getRequestAttributes().get("channel");
		Optional<Channel> channelOptional = thingRepository.getChannel(thingId, channelId);
		if (!channelOptional.isPresent() || !(channelOptional.get() instanceof WriteChannel<?>)) {
			throw new ResourceException(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
		}
		WriteChannel<?> channel = (WriteChannel<?>) channelOptional.get();
		// parse value
		JsonParser parser = new JsonParser();
		JsonElement jHttpPost = parser.parse(httpPost);
		JsonElement jValue = null;
		if (jHttpPost.isJsonObject()) {
			JsonObject jObject = jHttpPost.getAsJsonObject();
			if (jObject.has("value")) {
				jValue = jObject.get("value");
			}
		}
		if (jValue == null) {
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
