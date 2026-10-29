package io.openems.edge.common.jsonrpc.response;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.types.SemanticVersion;
import io.openems.edge.common.jsonrpc.base.GetSystemUpdateState;
import io.openems.edge.common.jsonrpc.base.GetSystemUpdateState.Response.SystemUpdateState;
import io.openems.edge.common.jsonrpc.base.GetSystemUpdateState.UpdateState;
import io.openems.edge.common.jsonrpc.request.GetSystemUpdateStateRequest;

/**
 * JSON-RPC Response to {@link GetSystemUpdateStateRequest}.
 *
 * <p>
 * The state types and their JSON serialization are defined in
 * {@link GetSystemUpdateState}.
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

	private final SystemUpdateState state;

	/**
	 * Builds a {@link GetSystemUpdateStateResponse} for the running state.
	 *
	 * @param id          the request ID
	 * @param updateState the {@link UpdateState}
	 * @return the {@link GetSystemUpdateStateResponse}
	 */
	public static GetSystemUpdateStateResponse isRunning(UUID id, UpdateState updateState) {
		return new GetSystemUpdateStateResponse(id, SystemUpdateState.running(updateState));
	}

	/**
	 * Builds a {@link GetSystemUpdateStateResponse} for the unknown, updated or
	 * available state.
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
			return new GetSystemUpdateStateResponse(id, SystemUpdateState.unknownState());
		}
		if (current.isAtLeast(latest)) {
			return new GetSystemUpdateStateResponse(id, SystemUpdateState.updated(current));
		}
		return new GetSystemUpdateStateResponse(id, SystemUpdateState.available(current, latest));
	}

	private GetSystemUpdateStateResponse(UUID id, SystemUpdateState state) {
		super(id);
		this.state = state;
	}

	@Override
	public JsonObject getResult() {
		return SystemUpdateState.serializer().serialize(this.state).getAsJsonObject();
	}

}