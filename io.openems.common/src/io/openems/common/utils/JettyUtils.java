package io.openems.common.utils;

import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.StringTokenizer;
import java.util.UUID;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcResponseError;

public class JettyUtils {

	private JettyUtils() {
	}

	public static record Credentials(String username, String password) {
	}

	/**
	 * Parses login credentials from an Authorization header.
	 * 
	 * @param request the {@link Request}
	 * @return a {@link Credentials} object or null
	 */
	public static Credentials parseCredentials(Request request) {
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
						return new Credentials(username, password);
					}
				}
			}
		}
		return null;
	}

	/**
	 * Parses a request body to a {@link JsonObject}.
	 *
	 * @param request the {@link Request}
	 * @return The parsed {@link JsonObject}
	 * @throws OpenemsException if parsing fails
	 */
	public static JsonObject parseJson(Request request) throws OpenemsException {
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(Content.Source.asInputStream(request), StandardCharsets.UTF_8))) {
			return parseToJsonObject(br.lines().collect(joining("\n")));

		} catch (Exception e) {
			throw new OpenemsException("Unable to parse: " + e.getMessage());
		}
	}

	/**
	 * Sends an OK Response with the given data.
	 *
	 * @param response the {@link Response}
	 * @param data     the data to send
	 * @return true if the response was sent successfully
	 * @throws OpenemsException if an error occurs
	 */
	public static boolean sendOkResponse(Response response, JsonElement data) throws OpenemsException {
		response.getHeaders().put("Content-Type", "application/json");
		response.setStatus(HttpStatus.OK_200);
		var content = StandardCharsets.UTF_8.encode(data.toString());
		response.write(true, content, Callback.NOOP);
		return true;
	}

	/**
	 * Sends an Error Response from an Exception.
	 * 
	 * @param response  the {@link Response}
	 * @param jsonrpcId the JsonRpc Message-ID
	 * @param ex        the {@link Throwable}
	 */
	public static void sendErrorResponse(Response response, UUID jsonrpcId, Throwable ex) {
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

}
