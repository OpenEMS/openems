package io.openems.edge.controller.api.rest.route;

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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.common.session.Role;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.rest.MyRestlet;
import io.openems.edge.controller.api.rest.RestApi;

public class ChannelRestlet extends MyRestlet {

	private final RestApi parent;

	public ChannelRestlet(RestApi parent) {
		super();
		this.parent = parent;
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
		Channel<?> channel = null;
		for (OpenemsComponent component : this.parent.getComponents()) {
			if (component.id().equals(thingId)) {
				// get channel
				channel = component.channel(channelId);
				break;
			}
		}
		if (channel == null) {
			// Channel not found
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
		}

		// call handler methods
		if (request.getMethod().equals(Method.GET)) {
			// TODO check read permission
			// assertAllowed(request, channel.readRoles());

			Representation entity = getValue(channel);
			response.setEntity(entity);

		} else if (request.getMethod().equals(Method.POST)) {
			// TODO check write permissions
			// assertAllowed(request, channel.writeRoles());

			JsonParser parser = new JsonParser();
			String httpPost = request.getEntityAsText();
			JsonObject jHttpPost = parser.parse(httpPost).getAsJsonObject();
			setValue(channel, jHttpPost);
		}
	}

	// private void assertAllowed(Request request, Set<Role> channelRoles) throws
	// ResourceException {
	// boolean allowed = false;
	// for (Role role : channelRoles) {
	// if (isAuthenticatedAsRole(request, role)) {
	// allowed = true;
	// break;
	// }
	// }
	// if (!allowed) {
	// throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED);
	// }
	// }

	/**
	 * handle HTTP GET request
	 *
	 * @param thingId
	 * @param channelId
	 * @return
	 */
	private Representation getValue(Channel<?> channel) {
		JsonObject j = new JsonObject();
		// value
		j.add("value", channel.value().asJson());
		// type
		Optional<OpenemsType> typeOpt = channel.channelDoc().getType();
		String type;
		if (typeOpt.isPresent()) {
			type = typeOpt.get().toString().toLowerCase();
		} else {
			type = "UNDEFINED";
		}
		j.addProperty("type", type);
		// writable
		j.addProperty("writable", //
				channel instanceof WriteChannel<?> ? true : false //
		);
		return new StringRepresentation(j.toString(), MediaType.APPLICATION_JSON);
	}

	/**
	 * handle HTTP POST request
	 *
	 * @param thingId
	 * @param channelId
	 * @param jHttpPost
	 */
	private void setValue(Channel<?> channel, JsonObject jHttpPost) {
		// check for writable channel
		if (!(channel instanceof WriteChannel<?>)) {
			throw new ResourceException(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
		}

		// parse value
//		JsonElement jValue;
//		if (jHttpPost.has("value")) {
//			jValue = jHttpPost.get("value");
//		} else {
//			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Value is missing");
//		}

		// TODO set channel value
//		if (channel instanceof ConfigChannel<?>) {
//
//			// is a ConfigChannel
//			ConfigChannel<?> configChannel = (ConfigChannel<?>) channel;
//			try {
//				configChannel.updateValue(jValue, true);
//				log.info("Updated Channel [" + channel.address() + "] to value [" + jValue.toString() + "].");
//			} catch (NotImplementedException e) {
//				throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Conversion not implemented");
//			}
//
//		} else if (channel instanceof WriteChannel<?>) {
//			/*
//			 * WriteChannel
//			 */
//			WriteChannel<?> writeChannel = (WriteChannel<?>) channel;
//			WriteObject writeObject = new WriteJsonObject(jValue).onFirstSuccess(() -> {
//				Notification.EDGE_CHANNEL_UPDATE_SUCCESS.writeToLog(log, "set " + channel.address() + " => " + jValue);
//			}).onFirstError((e) -> {
//				Notification.EDGE_CHANNEL_UPDATE_FAILED.writeToLog(log, "set " + channel.address() + " => " + jValue);
//			}).onTimeout(() -> {
//				Notification.EDGE_CHANNEL_UPDATE_TIMEOUT.writeToLog(log, "set " + channel.address() + " => " + jValue);
//			});
//			this.apiWorker.addValue(writeChannel, writeObject);
//		}
	}
}
