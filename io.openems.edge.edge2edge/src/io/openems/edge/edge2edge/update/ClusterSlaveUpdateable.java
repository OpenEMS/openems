package io.openems.edge.edge2edge.update;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.TextProvider;
import io.openems.edge.common.jsonrpc.base.ExecuteSystemUpdate;
import io.openems.edge.common.jsonrpc.base.GetSystemUpdateState;
import io.openems.edge.common.update.Updateable;
import io.openems.edge.common.update.jsonrpc.GetUpdateState;
import io.openems.edge.edge2edge.websocket.bridge.Edge2EdgeWebsocketBridge;

public class ClusterSlaveUpdateable implements Updateable {

	public static final int ESTIMATED_UPDATE_MINUTES = 5;
	private static final int TIMEOUT_MILLISECONDS = 10000;
	private static final int REBOOT_TIMEOUT_MINUTES = 4;
	private static final Duration REBOOT_DURATION = Duration.ofMinutes(REBOOT_TIMEOUT_MINUTES);
	private static final Duration ERROR_RETENTION_DURATION = Duration.ofMinutes(REBOOT_TIMEOUT_MINUTES);
	private final Logger log = LoggerFactory.getLogger(ClusterSlaveUpdateable.class);
	private final Edge2EdgeWebsocketBridge bridge;
	private final String name;
	private GetUpdateState.UpdateState lastUpdateState = new GetUpdateState.UpdateState.Unknown();
	private Instant connectionLostSince = null;
	private Instant errorMsgSetAt = null;
	private String errorMsgAfterExecuting = null;
	private final Clock clock;

	public ClusterSlaveUpdateable(Edge2EdgeWebsocketBridge bridge, Clock clock) {
		this.bridge = bridge;
		Objects.requireNonNull(this.bridge);
		this.name = this.bridge.getRemoteHostname() //
				.orElse(this.getBridgeAliasOrId());
		this.clock = clock;
	}

	@VisibleForTesting
	public ClusterSlaveUpdateable(String name, Clock clock) {
		this.bridge = null;
		this.clock = clock;
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public UpdateableMetaInfo getMetaInfo(Language language) {
		var bridgeAlias = this.getBridgeAliasOrId();
		final var metaInfoName = TextProvider //
				.byTranslation(ClusterSlaveUpdateable.class, "ClusterSlaveUpdateable.Metainfo.name") //
				.formatWithArguments(this.name) //
				.getText(language);
		final var metaInfoDescription = this.name.equals(bridgeAlias) //
				? metaInfoName //
				: TextProvider //
						.byTranslation(ClusterSlaveUpdateable.class, "ClusterSlaveUpdateable.Metainfo.description") //
						.formatWithArguments(this.name, bridgeAlias) //
						.getText(language);
		return new Updateable.UpdateableMetaInfo(metaInfoName, metaInfoDescription, Role.ADMIN);
	}

	@Override
	public void executeUpdate() {
		this.bridge.sendRequest(new ComponentJsonApiRequest("_host", //
				GenericJsonrpcRequest.createRequest(//
						new ExecuteSystemUpdate(), //
						new ExecuteSystemUpdate.Request(false))))
				.whenComplete((response, error) -> {
					if (error == null || this.isSlaveDisconnected(error)) {
						return;
					}
					this.errorMsgAfterExecuting = String.format("Unable to execute update on slave %s: %s", this.name,
							error.getMessage());
					this.errorMsgSetAt = this.clock.instant();
					this.log.error(this.errorMsgAfterExecuting);
				});
	}

	@Override
	public GetUpdateState.UpdateState getUpdateState(Language lang) {
		try {
			final var currentState = this.fetchSlaveSystemUpdateState();
			if (this.connectionLostSince != null) {
				this.connectionLostSince = null;
			}
			final GetUpdateState.UpdateState result;

			if (currentState.running() != null) {
				final var running = currentState.running().updateState();
				result = new GetUpdateState.UpdateState.Running(//
						running.getPercentCompleted().get(), //
						ESTIMATED_UPDATE_MINUTES, //
						running.getLogs() //
				);
			} else if (currentState.updated() != null) {
				final var updated = currentState.updated();
				result = new GetUpdateState.UpdateState.Updated(updated.version().toString());
			} else if (this.hasFreshErrorMsgAfterExecuting()) {
				result = new GetUpdateState.UpdateState.Error(this.errorMsgAfterExecuting);
			} else if (currentState.available() != null) {
				final var available = currentState.available();
				result = new GetUpdateState.UpdateState.Available(//
						available.currentVersion().toString(), //
						available.latestVersion().toString() //
				);
			} else {
				result = new GetUpdateState.UpdateState.Unknown();
			}

			this.lastUpdateState = result;
			return result;

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			this.log.error("Interrupted while getting update state from slave {}: {}", this.name, e.getMessage());
			return this.getConnectionProblemError(lang);
		} catch (Exception e) {
			if (this.isSlaveRebooting()) {
				this.log.info("Slave {} is restarting after updating", this.name);
				return this.lastUpdateState;
			}
			this.log.error("Failed to get update state from slave {}: {}", this.name, e.getMessage());
			return this.getConnectionProblemError(lang);
		}
	}

	private GetSystemUpdateState.Response.SystemUpdateState fetchSlaveSystemUpdateState()
			throws InterruptedException, TimeoutException, IllegalStateException, ExecutionException {
		final var response = this.bridge.sendRequest(new ComponentJsonApiRequest("_host",
				GenericJsonrpcRequest.createRequest(new GetSystemUpdateState(), EmptyObject.INSTANCE)));

		var result = response //
				.get(TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS) //
				.getResult();

		final var payload = result.getAsJsonObject("payload");
		if (payload == null) {
			throw new IllegalStateException("Remote response is missing 'payload': " + result);
		}

		final var updateState = payload.get("result");
		if (updateState == null || !updateState.isJsonObject()) {
			throw new IllegalStateException("Remote response is missing 'payload.result': " + payload);
		}

		return GetSystemUpdateState.Response.serializer().deserialize(updateState).updateState();
	}

	private boolean isSlaveRebooting() {
		if (!(this.lastUpdateState instanceof GetUpdateState.UpdateState.Running)) {
			return false;
		}
		final var now = this.clock.instant();
		if (this.connectionLostSince == null) {
			this.connectionLostSince = now;
		}

		return Duration.between(this.connectionLostSince, now).compareTo(REBOOT_DURATION) <= 0;
	}

	private GetUpdateState.UpdateState.Error getConnectionProblemError(Language lang) {
		var errorMsg = TextProvider
				.byTranslation(ClusterSlaveUpdateable.class, "ClusterSlaveUpdateable.ConnectionProblem").getText(lang);

		var error = new GetUpdateState.UpdateState.Error(errorMsg);
		this.lastUpdateState = error;
		return error;
	}

	private boolean isSlaveDisconnected(Throwable t) {
		if (t == null) {
			return false;
		}

		if (t instanceof ExecutionException || t instanceof CompletionException) {
			t = t.getCause();
			if (t == null) {
				return false;
			}
		}

		if (t instanceof OpenemsError.OpenemsNamedException namedException) {
			var error = namedException.getError();
			return error == OpenemsError.BACKEND_EDGE_NOT_CONNECTED;
		}
		return false;
	}

	private boolean hasFreshErrorMsgAfterExecuting() {

		if (this.errorMsgSetAt == null || this.errorMsgAfterExecuting == null) {
			return false;
		}

		if (Duration.between(this.errorMsgSetAt, this.clock.instant()).compareTo(ERROR_RETENTION_DURATION) > 0) {
			this.clearErrorMsgAfterExecuting();
			return false;
		}
		return true;
	}

	private void clearErrorMsgAfterExecuting() {
		this.errorMsgAfterExecuting = null;
		this.errorMsgSetAt = null;
	}

	private String getBridgeAliasOrId() {
		var bridgeAlias = this.bridge.alias();
		return bridgeAlias == null || bridgeAlias.isBlank() //
				? this.bridge.id() //
				: bridgeAlias;
	}
}
