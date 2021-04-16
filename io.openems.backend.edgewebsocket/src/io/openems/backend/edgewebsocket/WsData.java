package io.openems.backend.edgewebsocket;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Metadata;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.utils.StringUtils;

public class WsData extends io.openems.common.websocket.WsData {

	private final WebsocketServer parent;
	private CompletableFuture<Boolean> isAuthenticated = new CompletableFuture<Boolean>();
	private Optional<String> apikey = Optional.empty();
	private Optional<String> edgeId = Optional.empty();

	public WsData(WebsocketServer parent) {
		this.parent = parent;
	}

	public void setAuthenticated(boolean isAuthenticated) {
		this.isAuthenticated.complete(isAuthenticated);
	}

	public CompletableFuture<Boolean> isAuthenticated() {
		return this.isAuthenticated;
	}

	public void assertAuthenticatedWithTimeout(JsonrpcMessage message, long timeout, TimeUnit unit)
			throws OpenemsException {
		try {
			boolean isAuthenticated = this.isAuthenticated.get(timeout, unit);
			if (!isAuthenticated) {
				throw new OpenemsException(
						"Connection with Edge [" + this.getId() + "] is not authenticated. Unable to handle "
								+ StringUtils.toShortString(message.toString(), 100));
			}
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new OpenemsException(
					"Timeout while evaluating if connection with Edge [" + this.getId() + "] is authenticated.");
		}

	}

	public synchronized void setApikey(String apikey) {
		this.apikey = Optional.ofNullable(apikey);
	}

	public synchronized Optional<String> getApikey() {
		return apikey;
	}

	public synchronized void setEdgeId(String edgeId) {
		this.edgeId = Optional.ofNullable(edgeId);
	}

	public synchronized Optional<String> getEdgeId() {
		return edgeId;
	}

	/**
	 * Gets the Edge.
	 * 
	 * @param metadata the Metadata service
	 * @return the Edge or Optional.Empty if the Edge-ID was not set or it is not
	 *         available from Metadata service
	 */
	public synchronized Optional<Edge> getEdge(Metadata metadata) {
		Optional<String> edgeId = this.getEdgeId();
		if (edgeId.isPresent()) {
			Optional<Edge> edge = metadata.getEdge(edgeId.get());
			return edge;
		}
		return Optional.empty();
	}

	public String assertEdgeId(JsonrpcMessage message) throws OpenemsException {
		if (this.edgeId.isPresent()) {
			return this.edgeId.get();
		} else {
			throw new OpenemsException(
					"EdgeId is not set. Unable to handle " + StringUtils.toShortString(message.toString(), 100));
		}
	}

	@Override
	public String toString() {
		return "EdgeWebsocket.WsData [apikey=" + apikey.orElse("UNKNOWN") + ", edgeId=" + edgeId.orElse("UNKNOWN")
				+ "]";
	}

	/**
	 * Gets an Id of this Edge - either the Edge-ID or the Apikey or "UNKNOWN".
	 * 
	 * @return the Id
	 */
	private String getId() {
		return this.edgeId.orElse(this.apikey.orElse("UNKNOWN"));
	}

	@Override
	protected ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
			TimeUnit unit) {
		return this.parent.scheduleWithFixedDelay(command, initialDelay, delay, unit);
	}
}
