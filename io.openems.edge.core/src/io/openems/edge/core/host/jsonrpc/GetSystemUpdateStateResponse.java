package io.openems.edge.core.host.jsonrpc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.host.SystemUpdateHandler;

/**
 * JSON-RPC Response to {@link GetSystemUpdateStateRequest}.
 *
 * <p>
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     // State is unknown (e.g. internet connection limited by firewall)
 *     "unknown"?: {
 *     }
 *     // Latest version is already installed
 *     "updated"?: {
 *       "version": "XXXX"
 *     }
 *     // Update is available
 *     "available"?: {
 *       "currentVersion": "XXXX",
 *       "latestVersion": "XXXX"
 *     },
 *     // Update is currently running
 *     "running"?: {
 *       "percentCompleted": number,
 *       "logs": string[]
 *     }
 *   }
 * }
 * </pre>
 */
public class GetSystemUpdateStateResponse extends JsonrpcResponseSuccess {

	private static interface SystemUpdateState {
		public JsonObject toJsonObject();
	}

	private static class Unknown implements SystemUpdateState {
		public Unknown() {
		}

		@Override
		public JsonObject toJsonObject() {
			return JsonUtils.buildJsonObject() //
					.add("unknown", new JsonObject()) //
					.build();
		}
	}

	private static class Updated implements SystemUpdateState {
		private final SemanticVersion version;

		public Updated(SemanticVersion version) {
			this.version = version;
		}

		@Override
		public JsonObject toJsonObject() {
			return JsonUtils.buildJsonObject() //
					.add("updated", JsonUtils.buildJsonObject() //
							.addProperty("version", this.version.toString()) //
							.build()) //
					.build();
		}
	}

	private static class Available implements SystemUpdateState {
		private final SemanticVersion currentVersion;
		private final SemanticVersion latestVersion;

		public Available(SemanticVersion currentVersion, SemanticVersion latestVersion) {
			this.currentVersion = currentVersion;
			this.latestVersion = latestVersion;
		}

		@Override
		public JsonObject toJsonObject() {
			return JsonUtils.buildJsonObject() //
					.add("available", JsonUtils.buildJsonObject() //
							.addProperty("currentVersion", this.currentVersion.toString()) //
							.addProperty("latestVersion", this.latestVersion.toString()) //
							.build()) //
					.build();
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
		public void addLog(String label, ExecuteSystemCommandResponse response) {
			synchronized (this.log) {
				var stdout = response.scr.stdout();
				if (stdout.length > 0) {
					this.addLog(label + ": STDOUT");
					for (var line : stdout) {
						this.addLog(label + ": " + line);
					}
				}
				var stderr = response.scr.stderr();
				if (stderr.length > 0) {
					this.addLog(label + ": STDERR");
					for (var line : stderr) {
						this.addLog(label + ": " + line);
					}
				}
				if (response.scr.exitcode() == 0) {
					this.addLog(label + ": FINISHED SUCCESSFULLY");
				} else {
					this.addLog(label + ": FINISHED WITH ERROR CODE [" + response.scr.exitcode() + "]");
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

	private static class Running implements SystemUpdateState {
		private final UpdateState updateState;

		public Running(UpdateState updateState) {
			this.updateState = updateState;
		}

		@Override
		public JsonObject toJsonObject() {
			return this.updateState.toJsonObject();
		}
	}

	private final SystemUpdateState state;

	/**
	 * Builds a {@link GetSystemUpdateStateResponse} for {@link Running} state.
	 *
	 * @param id          the request ID
	 * @param updateState the {@link UpdateState}
	 * @return the {@link GetSystemUpdateStateResponse}
	 */
	public static GetSystemUpdateStateResponse isRunning(UUID id, UpdateState updateState) {
		return new GetSystemUpdateStateResponse(id, new Running(updateState));
	}

	/**
	 * Builds a {@link GetSystemUpdateStateResponse} for {@link Unknown},
	 * {@link Updated} or {@link Available} state.
	 *
	 * @param id             the request ID
	 * @param currentVersion the current version
	 * @param latestVersion  the latest version
	 * @return the {@link GetSystemUpdateStateResponse}
	 */
	public static GetSystemUpdateStateResponse from(UUID id, String currentVersion, String latestVersion) {
		final SemanticVersion current;
		final SemanticVersion latest;
		try {
			current = SemanticVersion.fromString(currentVersion);
			latest = SemanticVersion.fromString(latestVersion);
		} catch (NumberFormatException e) {
			return new GetSystemUpdateStateResponse(id, new Unknown());
		}
		if (current.isAtLeast(latest)) {
			return new GetSystemUpdateStateResponse(id, new Updated(current));
		}
		return new GetSystemUpdateStateResponse(id, new Available(current, latest));
	}

	private GetSystemUpdateStateResponse(UUID id, SystemUpdateState state) {
		super(id);
		this.state = state;
	}

	@Override
	public JsonObject getResult() {
		return this.state.toJsonObject();
	}

}
