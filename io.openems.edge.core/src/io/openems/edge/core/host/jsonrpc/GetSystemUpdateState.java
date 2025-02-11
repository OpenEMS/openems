
package io.openems.edge.core.host.jsonrpc;

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

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.serialization.EndpointRequestType;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.host.SystemUpdateHandler;
import io.openems.edge.core.host.jsonrpc.GetSystemUpdateState.Response;

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
public class GetSystemUpdateState implements EndpointRequestType<EmptyObject, Response> {

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
		 * Returns a {@link JsonSerializer} for a {@link GetSystemUpdateState.Response}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<GetSystemUpdateState.Response> serializer() {
			return jsonSerializer(GetSystemUpdateState.Response.class, //
					json -> new GetSystemUpdateState.Response(SystemUpdateState.serializer().deserializePath(json)), //
					obj -> SystemUpdateState.serializer().serialize(obj.updateState()));
		}

		public record SystemUpdateState(//
				Running running, //
				Available available, //
				Updated updated, //
				Unknown unknown //
		) {

			/**
			 * Creates a SystemUpdateState which indicates a running state.
			 * 
			 * @param updateState the current state
			 * @return the created {@link SystemUpdateState}
			 */
			public static SystemUpdateState isRunning(UpdateState updateState) {
				return new SystemUpdateState(new SystemUpdateState.Running(updateState), null, null, null);
			}

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
				return new SystemUpdateState(null, new SystemUpdateState.Available(currentVersion, latestVersion), null,
						null);
			}

			/**
			 * Returns a {@link JsonSerializer} for a {@link GetSystemUpdateState.Response}.
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
				 * Returns a {@link JsonSerializer} for a
				 * {@link GetSystemUpdateState.Response.SystemUpdateState.Unknown}.
				 * 
				 * @return the created {@link JsonSerializer}
				 */
				public static JsonSerializer<SystemUpdateState.Unknown> serializer() {
					return emptyObjectSerializer(SystemUpdateState.Unknown::new);
				}

			}

			public record Updated(SemanticVersion version) {

				/**
				 * Returns a {@link JsonSerializer} for a
				 * {@link GetSystemUpdateState.Response.SystemUpdateState.Updated}.
				 * 
				 * @return the created {@link JsonSerializer}
				 */
				public static JsonSerializer<SystemUpdateState.Updated> serializer() {
					return jsonObjectSerializer(SystemUpdateState.Updated.class, //
							json -> new GetSystemUpdateState.Response.SystemUpdateState.Updated(//
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
				 * Returns a {@link JsonSerializer} for a
				 * {@link GetSystemUpdateState.Response.SystemUpdateState.Available}.
				 * 
				 * @return the created {@link JsonSerializer}
				 */
				public static JsonSerializer<SystemUpdateState.Available> serializer() {
					return jsonObjectSerializer(SystemUpdateState.Available.class, //
							json -> new SystemUpdateState.Available(//
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
				 * Returns a {@link JsonSerializer} for a
				 * {@link GetSystemUpdateState.Response.SystemUpdateState.Running}.
				 * 
				 * @return the created {@link JsonSerializer}
				 */
				public static JsonSerializer<SystemUpdateState.Running> serializer() {
					return jsonObjectSerializer(SystemUpdateState.Running.class, //
							json -> new SystemUpdateState.Running(new UpdateState()),
							obj -> obj.updateState.toJsonObject());
				}

			}

		}

	}

	public static class UpdateState {
		private final Logger log = LoggerFactory.getLogger(SystemUpdateHandler.class);

		private final AtomicBoolean isRunning = new AtomicBoolean(false);
		private final AtomicInteger percentCompleted = new AtomicInteger(0);
		private final List<String> logs = new ArrayList<>();
		private boolean debugMode = false;

		public UpdateState() {
			this.reset();
		}

		public void setRunning(boolean isRunning) {
			this.isRunning.set(isRunning);
		}

		public boolean isRunning() {
			return this.isRunning.get();
		}

		public void setPercentCompleted(int percentCompleted) {
			this.percentCompleted.set(percentCompleted);
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
