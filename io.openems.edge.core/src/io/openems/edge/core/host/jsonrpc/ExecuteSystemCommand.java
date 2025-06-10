package io.openems.edge.core.host.jsonrpc;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.toJsonArray;

import java.util.Arrays;
import java.util.Optional;

import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommand.Request;
import io.openems.edge.core.host.jsonrpc.ExecuteSystemCommand.Response;

public class ExecuteSystemCommand implements EndpointRequestType<Request, Response> {

	@Override
	public String getMethod() {
		return "executeSystemCommand";
	}

	@Override
	public JsonSerializer<Request> getRequestSerializer() {
		return Request.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Request(//
			String command, //
			boolean runInBackground, //
			int timeoutSeconds, //
			Optional<String> username, //
			Optional<String> password //
	) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link ExecuteSystemCommand.Request}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<ExecuteSystemCommand.Request> serializer() {
			return jsonObjectSerializer(ExecuteSystemCommand.Request.class, //
					json -> new ExecuteSystemCommand.Request(//
							json.getString("command"), //
							json.getOptionalBoolean("runInBackground") //
									.orElse(false), //
							json.getIntOrDefault("timeoutSeconds", 5), //
							json.getOptionalString("username"), //
							json.getOptionalString("password")), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("command", obj.command()) //
							.addProperty("runInBackground", obj.runInBackground()) //
							.addProperty("timeoutSeconds", obj.timeoutSeconds()) //
							.addPropertyIfNotNull("username", obj.username().orElse(null)) //
							.addPropertyIfNotNull("password", obj.password().orElse(null)) //
							.build());
		}

		/**
		 * Creates a {@link ExecuteSystemCommand.Request} which runs in the background
		 * without authentication.
		 * 
		 * @param command the command of the {@link ExecuteSystemCommand.Request}
		 * @return the created {@link ExecuteSystemCommand.Request}
		 */
		public static Request runInBackgroundWithoutAuthentication(String command) {
			return withoutAuthentication(command, true, 5);
		}

		/**
		 * Creates a {@link ExecuteSystemCommand.Request} without authentication.
		 * 
		 * @param command         the command of the
		 *                        {@link ExecuteSystemCommand.Request}
		 * @param runInBackground if the command should run in background
		 * @param timeoutSeconds  the command timeout
		 * @return the created {@link ExecuteSystemCommand.Request}
		 */
		public static Request withoutAuthentication(//
				String command, //
				boolean runInBackground, //
				int timeoutSeconds //
		) {
			return new Request(command, runInBackground, timeoutSeconds, Optional.empty(), Optional.empty());
		}

	}

	public record Response(String[] stdout, String[] stderr, int exitcode) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link ExecuteSystemCommand.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<ExecuteSystemCommand.Response> serializer() {
			return jsonObjectSerializer(ExecuteSystemCommand.Response.class, //
					json -> new ExecuteSystemCommand.Response(//
							json.getArray("stdout", String[]::new, JsonElementPath::getAsString), //
							json.getArray("stderr", String[]::new, JsonElementPath::getAsString), //
							json.getInt("exitcode")),
					obj -> JsonUtils.buildJsonObject() //
							.add("stdout", Arrays.stream(obj.stdout()) //
									.map(JsonPrimitive::new) //
									.collect(toJsonArray())) //
							.add("stderr", Arrays.stream(obj.stderr()) //
									.map(JsonPrimitive::new) //
									.collect(toJsonArray())) //
							.addProperty("exitcode", obj.exitcode()) //
							.build());
		}

	}

}
