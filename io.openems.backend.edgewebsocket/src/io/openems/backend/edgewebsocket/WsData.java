package io.openems.backend.edgewebsocket;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.openems.backend.common.edgewebsocket.EdgeCache;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.utils.StringUtils;

public class WsData extends io.openems.common.websocket.WsData {

	/**
	 * Edge-ID is set only if the connection was authenticated (i.e. apikey was
	 * correct).
	 */
	private Optional<String> edgeId = Optional.empty();

	private final CompletableFuture<Void> isAuthenticated = new CompletableFuture<>();
	public final EdgeCache edgeCache = new EdgeCache();

	/**
	 * Asserts that the Edge-ID is available (i.e. properly authenticated).
	 *
	 * @param message a identification message on error
	 * @return the Edge-ID
	 * @throws OpenemsException on error
	 */
	public String assertEdgeId(JsonrpcMessage message) throws OpenemsException {
		var edgeIdOpt = this.edgeId;
		if (edgeIdOpt.isPresent()) {
			return edgeIdOpt.get();
		}
		throw new OpenemsException(
				"Edge-ID is not set. Unable to handle " + StringUtils.toShortString(message.toString(), 100));
	}

	/**
	 * Asserts that the Edge-ID is available (i.e. properly authenticated) within a
	 * timeout.
	 *
	 * @param message a identification message on error
	 * @param timeout the timeout length
	 * @param unit    the {@link TimeUnit} of the timeout
	 * @return the Edge-ID
	 * @throws OpenemsNamedException on error
	 */
	public String assertEdgeIdWithTimeout(JsonrpcMessage message, long timeout, TimeUnit unit) throws OpenemsException {
		try {
			this.isAuthenticated.get(timeout, unit);
			return this.edgeId.get();

		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new OpenemsException(
					"Timeout while evaluating if connection with Edge [" + this.edgeId + "] is authenticated.");
		}
	}

	public synchronized void setEdgeId(String edgeId) {
		this.edgeId = Optional.of(edgeId);
		this.isAuthenticated.complete(null);
	}

	public synchronized Optional<String> getEdgeId() {
		return this.edgeId;
	}

	@Override
	public String toString() {
		return "EdgeWebsocket.WsData [" //
				+ "edgeId=" + this.edgeId.orElse("UNKNOWN") //
				+ "]";
	}

}
