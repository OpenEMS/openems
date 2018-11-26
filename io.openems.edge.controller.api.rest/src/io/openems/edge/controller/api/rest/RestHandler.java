package io.openems.edge.controller.api.rest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestHandler extends AbstractHandler {

	private final Logger log = LoggerFactory.getLogger(RestHandler.class);

	private final RestApi parent;

	public RestHandler(RestApi parent) {
		this.parent = parent;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		try {
			List<String> targets = Arrays.asList(//
					target.substring(1) // remove leading '/'
							.split("/"));

			if (targets.isEmpty()) {
				throw new OpenemsException("Missing arguments to handle request");
			}

			String thisTarget = targets.get(0);
			List<String> remainingTargets = targets.subList(1, targets.size());

			switch (thisTarget) {
			case "rest":
				this.handleRest(remainingTargets, baseRequest, request, response);
				break;
			}
		} catch (OpenemsException e) {
			throw new IOException(e.getMessage());
		}
	}

	private void handleRest(List<String> targets, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws OpenemsException, IOException {
		if (targets.isEmpty()) {
			throw new OpenemsException("Missing arguments to handle REST-request");
		}

		String thisTarget = targets.get(0);
		List<String> remainingTargets = targets.subList(1, targets.size());

		switch (thisTarget) {
		case "channel":
			this.handleChannel(remainingTargets, baseRequest, request, response);
			break;
		}
	}

	private void handleChannel(List<String> targets, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException, OpenemsException {
		if (targets.size() != 2) {
			throw new OpenemsException("Missing arguments to handle Channel");
		}

		// TODO check general permission
		// if (isAuthenticatedAsRole(request, Role.GUEST)) {
		// // pfff... it's only a "GUEST"! Deny anything but GET requests
		// if (!request.getMethod().equals(Method.GET)) {
		// throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED);
		// }
		// }

		// get request attributes
		String thingId = targets.get(0);
		String channelId = targets.get(1);

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
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		// call handler methods
		switch (request.getMethod()) {
		case "GET":
			this.handleGet(channel, baseRequest, request, response);
			break;
		case "POST":
			this.handlePost(channel, baseRequest, request, response);
			break;
		}
	}

	/**
	 * Handles HTTP GET request.
	 *
	 * @param channel     the affected channel
	 * @param baseRequest the HTTP POST base-request
	 * @param request     the HTTP POST request
	 * @param response    the result to be returned
	 * @throws OpenemsException on error
	 */
	private void handleGet(Channel<?> channel, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws OpenemsException {
		// TODO check read permission
		// assertAllowed(request, channel.readRoles());

		JsonObject j = new JsonObject();
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

		this.sendOkResponse(baseRequest, response, j);
	}

	private void sendOkResponse(Request baseRequest, HttpServletResponse response, JsonObject data)
			throws OpenemsException {
		try {
			response.setContentType("application/json");
			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
			response.getWriter().write(data.toString());
		} catch (IOException e) {
			throw new OpenemsException("Unable to send Ok-Response: " + e.getMessage());
		}
	}

	/**
	 * Handles HTTP POST request.
	 *
	 * @param readChannel the affected channel
	 * @param baseRequest the HTTP POST base-request
	 * @param request     the HTTP POST request
	 * @param response    the result to be returned
	 * @throws OpenemsException on error
	 */
	private void handlePost(Channel<?> readChannel, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws OpenemsException {
		// check for writable channel
		if (!(readChannel instanceof WriteChannel<?>)) {
			throw new OpenemsException("[" + readChannel + "] is not a Write Channel");
		}
		WriteChannel<?> channel = (WriteChannel<?>) readChannel;

		// parse json
		JsonParser parser = new JsonParser();
		JsonObject jHttpPost;
		try {
			jHttpPost = parser.parse(new BufferedReader(new InputStreamReader(baseRequest.getInputStream())).lines()
					.collect(Collectors.joining("\n"))).getAsJsonObject();
		} catch (Exception e) {
			throw new OpenemsException("Unable to parse: " + e.getMessage());
		}

		// parse value
		JsonElement jValue;
		if (jHttpPost.has("value")) {
			jValue = jHttpPost.get("value");
		} else {
			throw new OpenemsException("Value is missing");
		}

		// set channel value
		try {
			if (jValue.isJsonNull()) {
				channel.setNextWriteValue(null);
			} else {
				channel.setNextWriteValueFromObject(jValue.toString());
			}
			log.info("Updated Channel [" + channel.address() + "] to value [" + jValue.toString() + "].");
		} catch (OpenemsException e) {
			e.printStackTrace();
			throw new OpenemsException("Unable to set value: " + e.getMessage());
		}

		this.sendOkResponse(baseRequest, response, new JsonObject());
	}
}
