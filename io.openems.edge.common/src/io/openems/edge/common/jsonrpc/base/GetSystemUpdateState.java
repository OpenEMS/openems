
package io.openems.edge.common.jsonrpc.base;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.emptyObjectSerializer;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonSerializer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonrpc.response.ExecuteSystemCommandResponse;

/**
 * Gets the System Update State.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getSystemUpdateState",
 *   "params": {
 *   }
 * }
 * </pre>
 */
public class GetSystemUpdateState implements EndpointRequestType<EmptyObject, GetSystemUpdateState.Response> {

	@Override
	public String getMethod() {
		return "getSystemUpdateState";
	}

	@Override
	public JsonSerializer<EmptyObject> getRequestSerializer() {
		return EmptyObject.serializer();
	}

	@Override
	public JsonSerializer<Response> getResponseSerializer() {
		return Response.serializer();
	}

	public record Response(SystemUpdateState updateState) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Response> serializer() {
			return jsonSerializer(Response.class, //
					json -> new Response(SystemUpdateState.serializer().deserializePath(json)), //
					obj -> SystemUpdateState.serializer().serialize(obj.updateState()));
		}

		public record SystemUpdateState(//
				Running running, //
				Available available, //
				Updated updated, //
				Unknown unknown //
		) {

			/**
			 * Creates a SystemUpdateState which indicates that a update is available.
			 * 
			 * @param currentVersion the current version
			 * @param latestVersion  the latest version to update to
			 * @return the created {@link SystemUpdateState}
			 */
			public static SystemUpdateState available(//
					SemanticVersion currentVersion, //
					SemanticVersion latestVersion //
			) {
				return new SystemUpdateState(null, new Available(currentVersion, latestVersion), null, null);
			}

			/**
			 * Returns a {@link JsonSerializer} for a {@link Response}.
			 * 
			 * @return the created {@link JsonSerializer}
			 */
			public static JsonSerializer<SystemUpdateState> serializer() {
				return jsonObjectSerializer(SystemUpdateState.class, //
						json -> {
							final var updated = json.getNullableJsonElementPath("updated")
									.getAsObjectOrNull(Updated.serializer());
							final var available = json.getNullableJsonElementPath("available")
									.getAsObjectOrNull(Available.serializer());
							final var running = json.getNullableJsonElementPath("running")
									.getAsObjectOrNull(Running.serializer());
							final var unknown = json.getNullableJsonElementPath("unknown")
									.getAsObjectOrNull(Unknown.serializer());

							return new SystemUpdateState(running, available, updated, unknown);
						}, obj -> {
							return JsonUtils.buildJsonObject() //
									.onlyIf(obj.updated() != null, t -> {
										t.add("updated", Updated.serializer().serialize(obj.updated()));
									}) //
									.onlyIf(obj.available() != null, t -> {
										t.add("available", Available.serializer().serialize(obj.available()));
									}) //
									.onlyIf(obj.running() != null, t -> {
										t.add("running", Running.serializer().serialize(obj.running()));
									}) //
									.onlyIf(obj.unknown() != null, t -> {
										t.add("unknown", Unknown.serializer().serialize(obj.unknown()));
									}) //
									.build();
						});
			}

			public record Unknown() {

				/**
				 * Returns a {@link JsonSerializer} for a {@link Unknown}.
				 * 
				 * @return the created {@link JsonSerializer}
				 */
				public static JsonSerializer<Unknown> serializer() {
					return emptyObjectSerializer(Unknown::new);
				}

			}

			public record Updated(SemanticVersion version) {

				/**
				 * Returns a {@link JsonSerializer} for a {@link Updated}.
				 * 
				 * @return the created {@link JsonSerializer}
				 */
				public static JsonSerializer<Updated> serializer() {
					return jsonObjectSerializer(Updated.class, //
							json -> new Updated(//
									json.getSemanticVersion("version")), //
							obj -> JsonUtils.buildJsonObject() //
									.addProperty("version", obj.version().toString()) //
									.build());
				}

			}

			public record Available(//
					SemanticVersion currentVersion, //
					SemanticVersion latestVersion //
			) {

				/**
				 * Returns a {@link JsonSerializer} for a {@link Available}.
				 * 
				 * @return the created {@link JsonSerializer}
				 */
				public static JsonSerializer<Available> serializer() {
					return jsonObjectSerializer(Available.class, //
							json -> new Available(//
									json.getSemanticVersion("currentVersion"), //
									json.getSemanticVersion("latestVersion")),
							obj -> JsonUtils.buildJsonObject() //
									.addProperty("currentVersion", obj.currentVersion().toString()) //
									.addProperty("latestVersion", obj.latestVersion().toString()) //
									.build());
				}

			}

			public record Running(UpdateState updateState) {

				/**
				 * Returns a {@link JsonSerializer} for a {@link Running}.
				 * 
				 * @return the created {@link JsonSerializer}
				 */
				public static JsonSerializer<Running> serializer() {
					return jsonObjectSerializer(Running.class, //
							json -> new Running(new UpdateState(//
									json.getInt("percentCompleted"), //
									json.getJsonArray("logs").asList().stream() //
											.map(JsonElement::getAsString) //
											.toList() //
							)), obj -> JsonUtils.buildJsonObject() //
									.addProperty("percentCompleted", obj.updateState().getPercentCompleted().get()) //
									.add("logs", obj.updateState().getLogs().stream() //
											.map(JsonPrimitive::new) //
											.collect(JsonUtils.toJsonArray())) //
									.build() //
					);
				}

			}

		}

	}

	public static class UpdateState {
		private final Logger log = LoggerFactory.getLogger(UpdateState.class);

		private final AtomicBoolean isRunning = new AtomicBoolean(false);
		private final AtomicInteger percentCompleted = new AtomicInteger(0);
		private final List<String> logs = new ArrayList<>();
		private boolean debugMode = false;

		public UpdateState() {
			this.reset();
		}

		public UpdateState(int percentCompleted, List<String> logs) {
			this.percentCompleted.set(percentCompleted);
			synchronized (this.log) {
				this.logs.clear();
				this.logs.addAll(logs);
			}
		}

		public void setRunning(boolean isRunning) {
			this.isRunning.set(isRunning);
		}

		public boolean isRunning() {
			return this.isRunning.get();
		}

		public AtomicInteger getPercentCompleted() {
			return this.percentCompleted;
		}

		public void setPercentCompleted(int percentCompleted) {
			this.percentCompleted.set(percentCompleted);
		}

		public List<String> getLogs() {
			return this.logs;
		}

		public synchronized void setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
		}

		/**
		 * Adds a line to the log.
		 *
		 * @param line the line
		 */
		public void addLog(String line) {
			synchronized (this.log) {
				this.log.info("System-Update: " + line);
				if (this.debugMode) {
					this.logs.add(line);
				}
			}
		}

		/**
		 * Adds a {@link Exception} to the log.
		 *
		 * @param e the {@link Exception}
		 */
		public void addLog(Exception e) {
			var sw = new StringWriter();
			var pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			this.addLog(pw.toString());
		}

		/**
		 * Adds a {@link ExecuteSystemCommandResponse} with a label to the log.
		 *
		 * @param label    the label
		 * @param response the {@link ExecuteSystemCommandResponse}
		 */
		public void addLog(String label, ExecuteSystemCommand.Response response) {
			synchronized (this.log) {
				var stdout = response.stdout();
				if (stdout.length > 0) {
					this.addLog(label + ": STDOUT");
					for (var line : stdout) {
						this.addLog(label + ": " + line);
					}
				}
				var stderr = response.stderr();
				if (stderr.length > 0) {
					this.addLog(label + ": STDERR");
					for (var line : stderr) {
						this.addLog(label + ": " + line);
					}
				}
				if (response.exitcode() == 0) {
					this.addLog(label + ": FINISHED SUCCESSFULLY");
				} else {
					this.addLog(label + ": FINISHED WITH ERROR CODE [" + response.exitcode() + "]");
				}
			}
		}

		protected JsonObject toJsonObject() {
			var logs = JsonUtils.buildJsonArray();
			synchronized (this.log) {
				for (String log : this.logs) {
					logs.add(log);
				}
			}
			return JsonUtils.buildJsonObject() //
					.add("running", JsonUtils.buildJsonObject() //
							.addProperty("percentCompleted", this.percentCompleted.get()) //
							.add("logs", logs.build()) //
							.build()) //
					.build();
		}

		/**
		 * Resets the {@link UpdateState} object.
		 */
		public synchronized void reset() {
			this.isRunning.set(false);
			this.percentCompleted.set(0);
			synchronized (this.log) {
				this.logs.clear();
			}
		}
	}

}
