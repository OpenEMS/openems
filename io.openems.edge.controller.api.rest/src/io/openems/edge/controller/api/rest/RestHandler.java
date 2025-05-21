package io.openems.edge.controller.api.rest;

import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseError;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.SetChannelValueRequest;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StringUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.user.User;

public class RestHandler extends Handler.Abstract {

	private final Logger log = LoggerFactory.getLogger(RestHandler.class);
	private final AbstractRestApi parent;

	public RestHandler(AbstractRestApi parent) {
		this.parent = parent;
	}

	@Override
	public boolean handle(Request request, Response response, Callback callback) throws Exception {
		try {
			// Extract the raw request URI
			final var target = request.getHttpURI().getDecodedPath();

			// Special handling for favicon.ico requests
			if (target.endsWith("/favicon.ico")) {
				response.setStatus(HttpStatus.NOT_FOUND_404);
				callback.succeeded();
				return true;
			}

			if (target.isEmpty() || "/".equals(target)) {
				throw new OpenemsException("Missing arguments to handle request");
			}

			// Debug log for URIs with square brackets
			if (target.contains("[") || target.contains("]")) {
				if (this.parent.isDebugModeEnabled()) {
					this.parent.logInfo(this.log, "Processing URI with square brackets: " + target);
				}
			}

			// Split path segments preserving brackets
			final var targets = splitPathPreservingBrackets(target.substring(1));

			if (targets.isEmpty()) {
				throw new OpenemsException("Missing arguments to handle request");
			}

			// Authenticate the user
			var user = this.authenticate(request);

			var thisTarget = targets.get(0);
			var remainingTargets = targets.subList(1, targets.size());

			// Dispatch based on the first path token
			switch (thisTarget) {
			case "rest" -> this.handleRest(user, remainingTargets, request, response);
			case "jsonrpc" -> {
				switch (this.parent.getAccessMode()) {
				case READ_ONLY -> throw new OpenemsException("REST-Api is in Read-Only mode");
				case READ_WRITE, WRITE_ONLY -> this.handleJsonRpc(user, request, response);
				}
			}
			default -> throw new OpenemsException("Unknown REST endpoint: " + target);
			}
			callback.succeeded();
			return true;

		} catch (Exception e) {
			if (this.parent.isDebugModeEnabled()) {
				this.parent.logError(this.log, "REST call failed: " + e.getMessage());
			}
			callback.failed(e);
			return false;
		}
	}

	/**
	 * Split a path string into segments while preserving square brackets.
	 *
	 * @param path The path string without leading slash
	 * @return List of path segments
	 */
	public static List<String> splitPathPreservingBrackets(String path) {
		// Count open brackets to avoid splitting inside bracket patterns
		int bracketDepth = 0;
		StringBuilder processedPath = new StringBuilder();

		// Replace slashes inside brackets with a temporary marker
		for (int i = 0; i < path.length(); i++) {
			char c = path.charAt(i);
			if (c == '[') {
				bracketDepth++;
				processedPath.append(c);
			} else if (c == ']') {
				bracketDepth--;
				processedPath.append(c);
			} else if (c == '/' && bracketDepth > 0) {
				// Replace slashes inside brackets with a temporary marker
				processedPath.append('\u001F'); // ASCII unit separator as temporary marker
			} else {
				processedPath.append(c);
			}
		}

		// Split by slashes, then restore internal slashes if needed
		return Arrays.stream(processedPath.toString().split("/")) //
				.map(segment -> segment.replace('\u001F', '/')) //
				.toList();
	}

	private User authenticate(Request request) throws OpenemsNamedException {
		var authHeader = request.getHeaders().get("Authorization");
		if (authHeader != null) {
			var st = new StringTokenizer(authHeader);
			if (st.hasMoreTokens()) {
				var basic = st.nextToken();
				if (basic.equalsIgnoreCase("Basic")) {
					final var credentials = new String(Base64.getDecoder().decode(st.nextToken()),
							StandardCharsets.UTF_8);
					var p = credentials.indexOf(":");
					if (p != -1) {
						var username = credentials.substring(0, p).trim();
						var password = credentials.substring(p + 1).trim();
						// First try to authenticate using username and password.
						var userOpt = this.parent.getUserService().authenticate(username, password);
						if (userOpt.isPresent()) {
							return userOpt.get();
						}
						// Fallback: authenticate using password only.
						userOpt = this.parent.getUserService().authenticate(password);
						if (userOpt.isPresent()) {
							return userOpt.get();
						}
					}
				}
			}
		}
		throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
	}

	private boolean handleRest(User user, List<String> targets, Request request, Response response)
			throws IOException, OpenemsNamedException {
		if (targets.isEmpty()) {
			throw new OpenemsException("Missing arguments to handle REST-request");
		}

		var thisTarget = targets.get(0);
		var remainingTargets = targets.subList(1, targets.size());

		return switch (thisTarget) {
		case "channel" //
			-> this.handleChannel(user, remainingTargets, request, response);
		default //
			-> throw new OpenemsException("Unhandled REST target [" + thisTarget + "]");
		};
	}

	private boolean handleChannel(User user, List<String> targets, Request request, Response response)
			throws OpenemsNamedException {
		// Get componentId and channelId from either path or query parameters
		String componentId;
		String componentRegex = null;
		String channelRegex = null;

		// Extract query parameters from the request URI
		Map<String, String> queryParams = this.parseQueryParams(request.getHttpURI().getQuery());

		// Check if we have component/channel regex in query parameters
		if (queryParams.containsKey("componentRegex")) {
			componentRegex = queryParams.get("componentRegex");
		}
		if (queryParams.containsKey("channelRegex")) {
			channelRegex = queryParams.get("channelRegex");
		}

		// Check if we have exact component/channel in query parameters
		if (queryParams.containsKey("component")) {
			componentId = queryParams.get("component");
		} else if (targets.size() >= 1) {
			componentId = targets.get(0);
		} else {
			throw new OpenemsException("Missing component ID. Provide it in path or as 'component' query parameter.");
		}

		String channelId;

		if (queryParams.containsKey("channel")) {
			channelId = queryParams.get("channel");
		} else if (targets.size() >= 2) {
			channelId = targets.get(1);
		} else {
			throw new OpenemsException("Missing channel ID. Provide it in path or as 'channel' query parameter.");
		}

		if (this.parent.isDebugModeEnabled()) {
			this.parent.logInfo(this.log,
					"Processing channel request - componentId: [" + componentId + "], channelId: [" + channelId + "]"
							+ (componentRegex != null ? ", componentRegex: [" + componentRegex + "]" : "")
							+ (channelRegex != null ? ", channelRegex: [" + channelRegex + "]" : ""));
		}

		var channelAddress = new ChannelAddress(componentId, channelId);

		return switch (request.getMethod()) {
		case "GET" //
			-> this.handleGet(user, channelAddress, componentRegex, channelRegex, response);
		case "POST" //
			-> switch (this.parent.getAccessMode()) {
			case READ_ONLY //
				-> throw new OpenemsException("REST-Api is in Read-Only mode");
			case READ_WRITE, WRITE_ONLY //
				-> this.handlePost(user, channelAddress, componentRegex, channelRegex, request, response);
			};
		default //
			-> throw new OpenemsException("Unhandled REST Channel request method [" + request.getMethod() + "]");
		};
	}

	/**
	 * Parse query parameters from query string.
	 * 
	 * @param queryString The query string part of the URI
	 * @return Map of query parameter names to values
	 */
	private Map<String, String> parseQueryParams(String queryString) {
		Map<String, String> params = new HashMap<>();
		if (queryString == null || queryString.isEmpty()) {
			return params;
		}

		String[] pairs = queryString.split("&");
		for (String pair : pairs) {
			int idx = pair.indexOf("=");
			if (idx > 0) {
				String key = pair.substring(0, idx);
				String value = pair.substring(idx + 1);
				params.put(key, value);
			}
		}
		return params;
	}

	private boolean handleGet(User user, ChannelAddress channelAddress, String componentRegex, String channelRegex,
			Response response) throws OpenemsNamedException {
		user.assertRoleIsAtLeast("HTTP GET", Role.GUEST);
		var components = this.parent.getComponentManager().getEnabledComponents();

		if (this.parent.isDebugModeEnabled()) {
			this.parent.logInfo(this.log,
					"Looking for channels matching [" + channelAddress.toString() + "]"
							+ (componentRegex != null ? " with componentRegex [" + componentRegex + "]" : "")
							+ (channelRegex != null ? " with channelRegex [" + channelRegex + "]" : ""));
		}

		// Get channels with proper handling of regex patterns
		List<Channel<?>> channels;
		try {
			channels = this.getChannels(components, channelAddress, componentRegex, channelRegex);
		} catch (PatternSyntaxException e) {
			if (this.parent.isDebugModeEnabled()) {
				this.parent.logError(this.log, "Invalid regex pattern: " + e.getMessage());
			}
			response.setStatus(HttpStatus.BAD_REQUEST_400);
			return true;
		}

		// If no channel matches, send a 404.
		if (channels.isEmpty()) {
			if (this.parent.isDebugModeEnabled()) {
				this.parent.logWarn(this.log, "REST call by User [" + user.getName() + "]: GET Channel ["
						+ channelAddress.toString() + "] Result [No Match]");
			}
			response.setStatus(HttpStatus.NOT_FOUND_404);
			return false;
		}

		final var channeljson = channels.stream() //
				.map(channel -> JsonUtils.buildJsonObject() //
						.addProperty("address", channel.address().toString()) //
						.addProperty("type", channel.getType().name()) //
						.addProperty("accessMode", channel.channelDoc().getAccessMode().getAbbreviation()) //
						.addProperty("text", channel.channelDoc().getText()) //
						.addProperty("unit", channel.channelDoc().getUnit().symbol) //
						.onlyIf(channel.channelDoc().getAccessMode() != AccessMode.WRITE_ONLY,
								b -> b.add("value", channel.value().asJson())) //
						.build()) //
				.collect(toJsonArray());

		var result = channeljson.size() == 1 //
				? channeljson.get(0) //
				: channeljson;

		if (this.parent.isDebugModeEnabled()) {
			this.parent.logInfo(this.log, "REST call by User [" + user.getName() + "]: GET " //
					+ "Channel [" + channelAddress + "] " //
					+ "Result [" + result.toString() + "]");
		}

		return this.sendOkResponse(response, result);
	}

	/**
	 * Gets all channels for a given address, handling both exact matches and regex
	 * patterns. This version gracefully handles square brackets in patterns.
	 *
	 * @param components     The list of components to search
	 * @param channelAddress The channel address to match (used when no regex
	 *                       provided)
	 * @param componentRegex Optional regex pattern for component ID
	 * @param channelRegex   Optional regex pattern for channel ID
	 * @return A list of matching channels
	 * @throws PatternSyntaxException if the pattern is invalid
	 */
	protected List<Channel<?>> getChannels(List<OpenemsComponent> components, ChannelAddress channelAddress,
			String componentRegex, String channelRegex) throws PatternSyntaxException {

		// Use provided regex patterns if available, otherwise use the exact path
		// components
		final String componentPattern = componentRegex != null ? componentRegex : channelAddress.getComponentId();
		final String channelPattern = channelRegex != null ? channelRegex : channelAddress.getChannelId();

		if (this.parent.isDebugModeEnabled()) {
			this.parent.logInfo(this.log, "Using patterns - componentPattern: [" + componentPattern
					+ "], channelPattern: [" + channelPattern + "]");
		}

		return components.stream() //
				.filter(component -> Pattern.matches(componentPattern, component.id())) //
				.flatMap(component -> component.channels().stream()) //
				.filter(channel -> Pattern.matches(channelPattern, channel.channelId().id())) //
				.toList();
	}

	/**
	 * Legacy method for backward compatibility with tests.
	 *
	 * @param components     the list of OpenemsComponents to be processed
	 * @param channelAddress the channel address used to filter the channels
	 * @return a list of channels corresponding to the given channel address, or an
	 *         empty list if none found
	 * @throws PatternSyntaxException if the channel address pattern syntax is
	 *                                invalid
	 */
	protected List<Channel<?>> getChannels(List<OpenemsComponent> components, ChannelAddress channelAddress)
			throws PatternSyntaxException {
		return this.getChannels(components, channelAddress, null, null);
	}

	private void sendErrorResponse(Response response, UUID jsonrpcId, Throwable ex) {
		response.getHeaders().put("Content-Type", "application/json");
		response.setStatus(HttpStatus.BAD_REQUEST_400);
		final JsonrpcResponseError message;
		if (ex instanceof OpenemsNamedException one) {
			if (one.getError() == OpenemsError.COMMON_AUTHENTICATION_FAILED) {
				response.setStatus(HttpStatus.UNAUTHORIZED_401);
			}
			message = new JsonrpcResponseError(jsonrpcId, one);
		} else {
			message = new JsonrpcResponseError(jsonrpcId, ex.getMessage());
		}
		var content = StandardCharsets.UTF_8.encode(message.toString());
		response.write(true, content, Callback.NOOP);
	}

	/**
	 * Sends an OK response with the given data.
	 *
	 * @param response The HTTP response
	 * @param data     The data to send
	 * @return true if the response was sent successfully
	 * @throws OpenemsException if an error occurs
	 */
	private boolean sendOkResponse(Response response, JsonElement data) throws OpenemsException {
		response.getHeaders().put("Content-Type", "application/json");
		response.setStatus(HttpStatus.OK_200);
		var content = StandardCharsets.UTF_8.encode(data.toString());
		response.write(true, content, Callback.NOOP);
		return true;
	}

	private boolean handlePost(User user, ChannelAddress channelAddress, String componentRegex, String channelRegex,
			Request request, Response response) throws OpenemsNamedException {
		user.assertRoleIsAtLeast("HTTP POST", Role.OWNER);

		// Parse JSON request body
		var jHttpPost = RestHandler.parseJson(request);

		if (!jHttpPost.has("value")) {
			throw new OpenemsException("Value is missing");
		}
		var jValue = jHttpPost.get("value");

		// Check if we have regex patterns in the request body that override query
		// parameters
		if (jHttpPost.has("componentRegex")) {
			componentRegex = jHttpPost.get("componentRegex").getAsString();
		}

		if (jHttpPost.has("channelRegex")) {
			channelRegex = jHttpPost.get("channelRegex").getAsString();
		}

		if (this.parent.isDebugModeEnabled()) {
			this.parent.logInfo(this.log, "REST call by User [" + user.getName() + "]: POST " //
					+ "Channel [" + channelAddress.toString() + "] " //
					+ "value [" + jValue + "]"
					+ (componentRegex != null ? ", componentRegex: [" + componentRegex + "]" : "")
					+ (channelRegex != null ? ", channelRegex: [" + channelRegex + "]" : ""));
		}

		if (componentRegex != null || channelRegex != null) {
			// Using regex patterns
			var components = this.parent.getComponentManager().getEnabledComponents();

			try {
				// Get all matching channels
				var channels = this.getChannels(components, channelAddress, componentRegex, channelRegex);

				if (channels.isEmpty()) {
					throw new OpenemsException("No channels match the provided patterns");
				}

				// Set value for all matching channels
				for (var channel : channels) {
					this.parent.apiWorker.handleSetChannelValueRequest(this.parent.getComponentManager(), user,
							new SetChannelValueRequest(channel.address().getComponentId(),
									channel.address().getChannelId(), jValue));
				}
			} catch (PatternSyntaxException e) {
				throw new OpenemsException("Invalid regex pattern: " + e.getMessage());
			}
		} else {
			// Standard single channel update
			this.parent.apiWorker.handleSetChannelValueRequest(this.parent.getComponentManager(), user,
					new SetChannelValueRequest(channelAddress.getComponentId(), channelAddress.getChannelId(), jValue));
		}

		return this.sendOkResponse(response, new JsonObject());
	}

	/**
	 * Parses a request body as JSON.
	 *
	 * @param request The HTTP request
	 * @return The parsed JSON object
	 * @throws OpenemsException if parsing fails
	 */
	private static JsonObject parseJson(Request request) throws OpenemsException {
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(Content.Source.asInputStream(request), StandardCharsets.UTF_8))) {
			return parseToJsonObject(br.lines().collect(joining("\n")));

		} catch (Exception e) {
			throw new OpenemsException("Unable to parse: " + e.getMessage());
		}
	}

	private void handleJsonRpc(User user, Request request, Response response) throws OpenemsNamedException {
		user.assertRoleIsAtLeast("HTTP POST JSON-RPC", Role.OWNER);

		var requestId = new UUID(0L, 0L); // Dummy UUID
		try {
			if (!"POST".equalsIgnoreCase(request.getMethod())) {
				throw new OpenemsException(
						"Method [" + request.getMethod() + "] is not supported for JSON-RPC endpoint");
			}
			var json = RestHandler.parseJson(request);
			if (this.parent.isDebugModeEnabled()) {
				this.parent.logInfo(this.log,
						"REST/JsonRpc call by User [" + user.getName() + "]: " + StringUtils.toShortString(json, 100));
			}

			if (!json.has("jsonrpc")) {
				json.addProperty("jsonrpc", "2.0");
			}
			if (!json.has("id")) {
				json.addProperty("id", UUID.randomUUID().toString());
			}
			if (json.has("params")) {
				var params = getAsJsonObject(json, "params");
				if (params.has("payload")) {
					var payload = getAsJsonObject(params, "payload");
					if (!payload.has("jsonrpc")) {
						payload.addProperty("jsonrpc", "2.0");
					}
					if (!payload.has("id")) {
						payload.addProperty("id", UUID.randomUUID().toString());
					}
					params.add("payload", payload);
				}
				json.add("params", params);
			}

			var message = JsonrpcMessage.from(json);
			if (!(message instanceof JsonrpcRequest requestMessage)) {
				throw new OpenemsException("Only JSON-RPC Request is supported here.");
			}
			requestId = requestMessage.getId();
			var responseFuture = this.parent.getRpcRestHandler().handleRequest(user, requestMessage);
			final JsonrpcResponseSuccess rpcResponse;
			try {
				rpcResponse = responseFuture.get();
			} catch (InterruptedException | ExecutionException e) {
				this.sendErrorResponse(response, requestMessage.getId(),
						new OpenemsException("Unable to get Response: " + e.getMessage()));
				return;
			}
			this.sendOkResponse(response, rpcResponse.toJsonObject());

		} catch (Exception e) {
			this.sendErrorResponse(response, requestId,
					new OpenemsException("Unable to get Response: " + e.getMessage()));
		}
	}
}
