package io.openems.backend.edgewebsocket.impl;

import java.util.Optional;

import io.openems.backend.metadata.api.Edge;
import io.openems.backend.metadata.api.Metadata;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.utils.StringUtils;

public class WsData extends io.openems.common.websocket.WsData {

	private boolean isAuthenticated = false;
	private Optional<String> apikey = Optional.empty();
	private Optional<String> edgeId = Optional.empty();

	public WsData() {
	}

	public void setAuthenticated(boolean isAuthenticated) {
		this.isAuthenticated = isAuthenticated;
	}

	public boolean isAuthenticated() {
		return isAuthenticated;
	}

	public void assertAuthentication(JsonrpcMessage message) throws OpenemsException {
		if (!this.isAuthenticated()) {
			throw new OpenemsException("Connection is not authenticated. Unable to handle "
					+ StringUtils.toShortString(message.toString(), 100));
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
}
