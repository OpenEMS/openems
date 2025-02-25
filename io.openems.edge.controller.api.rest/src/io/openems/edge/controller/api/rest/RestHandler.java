package io.openems.edge.controller.api.rest;

import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RestHandler extends Handler.Abstract {

	private final Logger log = LoggerFactory.getLogger(RestHandler.class);
	private final AbstractRestApi parent;

	public RestHandler(AbstractRestApi parent) {
		this.parent = parent;
	}

	/**
	 * Jetty 12 now requires a handler method with this signature returning a
	 * boolean.
	 */
	@Override
	public boolean handle(Request baseRequest, Response response, Callback callback) throws Exception {
		HttpServletRequest request = (HttpServletRequest) baseRequest;
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		try {
			// Extract the target from the path info.
			String target = request.getPathInfo();
			if (target == null || target.isEmpty() || "/".equals(target)) {
				throw new OpenemsException("Missing arguments to handle request");
			}
			// Remove the leading '/' and split the path.
			List<String> targets = Arrays.asList(target.substring(1).split("/"));
			if (targets.isEmpty()) {
				throw new OpenemsException("Missing arguments to handle request");
			}

			// Authenticate the user.
			User user = this.authenticate(request);

			var thisTarget = targets.get(0);
			var remainingTargets = targets.subList(1, targets.size());

			// Dispatch based on the first path token.
			switch (thisTarget) {
			case "rest":
				this.handleRest(user, remainingTargets, baseRequest, request, httpResponse);
				break;
			case "jsonrpc":
				switch (this.parent.getAccessMode()) {
				case READ_ONLY:
					throw new OpenemsException("REST-Api is in Read-Only mode");
				case READ_WRITE:
				case WRITE_ONLY:
					this.handleJsonRpc(user, baseRequest, request, httpResponse);
					break;
				}
				break;
			default:
				throw new OpenemsException("Unknown REST endpoint: " + target);
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

	private User authenticate(HttpServletRequest request) throws OpenemsNamedException {
		var authHeader = request.getHeader("Authorization");
		if (authHeader != null) {
			var st = new StringTokenizer(authHeader);
			if (st.hasMoreTokens()) {
				var basic = st.nextToken();
				if (basic.equalsIgnoreCase("Basic")) {
					String credentials;
					try {
						credentials = new String(Base64.getDecoder().decode(st.nextToken()), "UTF-8");
					} catch (UnsupportedEncodingException e) {
						throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
					}
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

	private boolean handleRest(User user, List<String> targets, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException, OpenemsNamedException {
		if (targets.isEmpty()) {
			throw new OpenemsException("Missing arguments to handle REST-request");
		}

		var thisTarget = targets.get(0);
		var remainingTargets = targets.subList(1, targets.size());

		return switch (thisTarget) {
		case "channel" -> this.handleChannel(user, remainingTargets, baseRequest, request, response);
		default -> throw new OpenemsException("Unhandled REST target [" + thisTarget + "]");
		};
	}

	private boolean handleChannel(User user, List<String> targets, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws IOException, OpenemsNamedException {
		if (targets.size() != 2) {
			throw new OpenemsException("Missing arguments to handle Channel");
		}

		var channelAddress = new ChannelAddress(targets.get(0), targets.get(1));

		return switch (request.getMethod()) {
		case "GET" -> this.handleGet(user, channelAddress, baseRequest, request, response);
		case "POST" -> switch (this.parent.getAccessMode()) {
		case READ_ONLY -> throw new OpenemsException("REST-Api is in Read-Only mode");
		case READ_WRITE, WRITE_ONLY -> this.handlePost(user, channelAddress, baseRequest, request, response);
		};
		default -> throw new OpenemsException("Unhandled REST Channel request method [" + request.getMethod() + "]");
		};
	}

	private boolean handleGet(User user, ChannelAddress channelAddress, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws OpenemsNamedException {
		user.assertRoleIsAtLeast("HTTP GET", Role.GUEST);

		var components = this.parent.getComponentManager().getEnabledComponents();
		var channels = getChannels(components, channelAddress);

		// If no channel matches, send a 404.
		if (channels.isEmpty()) {
			if (this.parent.isDebugModeEnabled()) {
				this.parent.logWarn(this.log, "REST call by User [" + user.getName() + "]: GET Channel ["
						+ channelAddress.toString() + "] Result [No Match]");
			}
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return false;
		}

		// Build JSON response.
		var channeljson = new JsonArray();
		for (Channel<?> channel : channels) {
			var j = new JsonObject();
			j.addProperty("address", channel.address().toString());
			j.addProperty("type", channel.getType().name());
			var accessMode = channel.channelDoc().getAccessMode();
			j.addProperty("accessMode", accessMode.getAbbreviation());
			j.addProperty("text", channel.channelDoc().getText());
			j.addProperty("unit", channel.channelDoc().getUnit().symbol);
			if (accessMode != AccessMode.WRITE_ONLY) {
				j.add("value", channel.value().asJson());
			}
			channeljson.add(j);
		}

		JsonElement result = channeljson.size() == 1 ? channeljson.get(0) : channeljson;

		if (this.parent.isDebugModeEnabled()) {
			this.parent.logInfo(this.log, "REST call by User [" + user.getName() + "]: GET Channel ["
					+ channelAddress.toString() + "] Result [" + result.toString() + "]");
		}

		return this.sendOkResponse(response, result);
	}

	protected static List<Channel<?>> getChannels(List<OpenemsComponent> components, ChannelAddress channelAddress)
			throws PatternSyntaxException {
		return components.stream().filter(component -> Pattern.matches(channelAddress.getComponentId(), component.id()))
				.flatMap(component -> component.channels().stream())
				.filter(channel -> Pattern.matches(channelAddress.getChannelId(), channel.channelId().id())).toList();
	}

	private void sendErrorResponse(HttpServletResponse response, UUID jsonrpcId, Throwable ex) {
		try {
			response.setContentType("application/json");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			JsonrpcResponseError message = switch (ex) {
			case OpenemsNamedException one -> {
				if (one.getError() == OpenemsError.COMMON_AUTHENTICATION_FAILED) {
					response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				}
				yield new JsonrpcResponseError(jsonrpcId, one);
			}
			default -> new JsonrpcResponseError(jsonrpcId, ex.getMessage());
			};
			response.getWriter().write(message.toString());
		} catch (IOException e) {
			this.parent.logWarn(this.log, "Unable to send Error-Response: " + e.getMessage());
		}
	}

	private boolean sendOkResponse(HttpServletResponse response, JsonElement data) throws OpenemsException {
		try {
			response.setContentType("application/json");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().write(data.toString());
			return true;
		} catch (IOException e) {
			throw new OpenemsException("Unable to send Ok-Response: " + e.getMessage());
		}
	}

	private boolean handlePost(User user, ChannelAddress channelAddress, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response) throws OpenemsNamedException {
		user.assertRoleIsAtLeast("HTTP POST", Role.OWNER);

		// Parse JSON request body using HttpServletRequest.
		var jHttpPost = RestHandler.parseJson(request);

		if (!jHttpPost.has("value")) {
			throw new OpenemsException("Value is missing");
		}
		JsonElement jValue = jHttpPost.get("value");

		if (this.parent.isDebugModeEnabled()) {
			this.parent.logInfo(this.log, "REST call by User [" + user.getName() + "]: POST Channel ["
					+ channelAddress.toString() + "] value [" + jValue + "]");
		}

		// Dispatch the set channel value request.
		this.parent.apiWorker.handleSetChannelValueRequest(this.parent.getComponentManager(), user,
				new SetChannelValueRequest(channelAddress.getComponentId(), channelAddress.getChannelId(), jValue));

		return this.sendOkResponse(response, new JsonObject());
	}

	private static JsonObject parseJson(HttpServletRequest request) throws OpenemsException {
		try (var br = request.getReader()) {
			return parseToJsonObject(br.lines().collect(joining("\n")));
		} catch (Exception e) {
			throw new OpenemsException("Unable to parse: " + e.getMessage());
		}
	}

	private void handleJsonRpc(User user, Request baseRequest, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) throws OpenemsNamedException {
		user.assertRoleIsAtLeast("HTTP POST JSON-RPC", Role.OWNER);

		UUID requestId = new UUID(0L, 0L); // Dummy UUID
		try {
			if (!httpRequest.getMethod().equals("POST")) {
				throw new OpenemsException(
						"Method [" + httpRequest.getMethod() + "] is not supported for JSON-RPC endpoint");
			}

			// Parse JSON from HttpServletRequest.
			var json = RestHandler.parseJson(httpRequest);
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
				var params = JsonUtils.getAsJsonObject(json, "params");
				if (params.has("payload")) {
					var payload = JsonUtils.getAsJsonObject(params, "payload");
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
			if (!(message instanceof JsonrpcRequest request)) {
				throw new OpenemsException("Only JSON-RPC Request is supported here.");
			}
			requestId = request.getId();

			var responseFuture = this.parent.getRpcRestHandler().handleRequest(user, request);

			JsonrpcResponseSuccess rpcResponse;
			try {
				rpcResponse = responseFuture.get();
			} catch (InterruptedException | ExecutionException e) {
				this.sendErrorResponse(httpResponse, request.getId(),
						new OpenemsException("Unable to get Response: " + e.getMessage()));
				return;
			}

			this.sendOkResponse(httpResponse, rpcResponse.toJsonObject());

		} catch (Exception e) {
			this.sendErrorResponse(httpResponse, requestId,
					new OpenemsException("Unable to get Response: " + e.getMessage()));
		}
	}
}
