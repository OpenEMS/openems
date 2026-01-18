package io.openems.backend.edge.server;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;

public class WsData extends io.openems.common.websocket.WsData {

	private static final List<String> DEBUG_EDGE_IDS = List.of();

	/**
	 * Edge-ID is set only if the connection was authenticated (i.e. apikey was
	 * correct).
	 */
	private String edgeId = null;
	private final CompletableFuture<Void> isAuthenticated = new CompletableFuture<>();

	public WsData(WebSocket ws) {
		super(ws);
	}

	protected synchronized void setEdgeId(String edgeId) {
		this.edgeId = edgeId;
		super.setDebug(DEBUG_EDGE_IDS.contains(edgeId));
		this.isAuthenticated.complete(null);
	}

	/**
	 * Gets the Edge-ID.
	 * 
	 * @return the Edge-ID; possibly null
	 */
	public synchronized String getEdgeId() {
		return this.edgeId;
	}

	/**
	 * Gets the Edge-ID, but waits to avoid race conditions during OnOpen
	 * authentication.
	 * 
	 * @param timeout the timeout length
	 * @param unit    the {@link TimeUnit} of the timeout
	 * @return the Edge-ID; possibly null
	 */
	public synchronized String getEdgeIdWithTimeout(long timeout, TimeUnit unit) {
		try {
			this.isAuthenticated.get(timeout, unit);
		} catch (Exception e) {
			// ignore
		}
		return this.edgeId;
	}

	/**
	 * Gets the Edge-ID or "UNKNOWN".
	 * 
	 * @return never null
	 */
	public synchronized String getEdgeIdString() {
		return this.edgeId != null //
				? this.edgeId //
				: "UNKNOWN";
	}

	@Override
	protected String toLogString() {
		return "BackendEdgeServerWsData [" + this.getEdgeIdString() + "]";
	}

}
