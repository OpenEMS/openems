package io.openems.backend.edge.manager;

import static com.google.common.collect.Sets.newHashSet;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import org.java_websocket.WebSocket;

import com.google.common.collect.Maps;
import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.backend.common.edge.EdgeCache;
import io.openems.backend.common.edge.jsonrpc.ConnectedEdges;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcMessage;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;
import io.openems.common.utils.StringUtils;

public class WsData extends io.openems.common.websocket.WsData {

	/**
	 * Authenticated Edges with EdgeCache of remote connections.
	 */
	private final ConcurrentMap<String, EdgeCache> edges = Maps.newConcurrentMap();

	private String id;

	protected WsData(WebSocket ws) {
		super(ws);
	}

	protected void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return this.id;
	}

	/**
	 * Gets the number of connected Edges.
	 * 
	 * @return count
	 */
	public int getNumberOfEdges() {
		return this.edges.size();
	}

	/**
	 * Updates Connected/Disconnected Edges from a
	 * {@link ConnectedEdges.Notification}.
	 * 
	 * @param notification    the {@link ConnectedEdges.Notification}
	 * @param newOnline       callback for newly connected EdgeIds
	 * @param newOffline      callback for newly disconnected EdgeIds
	 * @param metricsConsumer callback for metrics
	 */
	protected void handleConnectedEdgesNotification(ConnectedEdges.Notification notification,
			Consumer<String> newOnline, Consumer<String> newOffline,
			Consumer<TimestampedDataNotification> metricsConsumer) {
		var previousEdgeIds = newHashSet(this.edges.keySet());
		for (var edgeId : notification.getEdgeIds()) {
			// Handle Online Edges
			previousEdgeIds.remove(edgeId);
			if (!this.edges.containsKey(edgeId)) {
				this.updateEdgeStatus(edgeId, true);
				newOnline.accept(edgeId);
			}
		}

		// Handle Offline Edges
		for (var edgeId : previousEdgeIds) {
			this.updateEdgeStatus(edgeId, false);
			newOffline.accept(edgeId);
		}

		// Handle Metrics
		var metrics = notification.getMetrics();
		if (this.id == null || metrics == null || metrics.isEmpty()) {
			return;
		}
		final var now = Instant.now().toEpochMilli();
		final var data = TreeBasedTable.<Long, String, JsonElement>create();
		for (var entry : metrics.entrySet()) {
			data.put(now, this.id + "/" + entry.getKey(), new JsonPrimitive(entry.getValue()));
		}
		metricsConsumer.accept(new TimestampedDataNotification(data));
	}

	/**
	 * Updates the status of an Edge.
	 * 
	 * @param edgeId   the Edge-ID
	 * @param isOnline set online?
	 */
	public void updateEdgeStatus(String edgeId, boolean isOnline) {
		if (isOnline) {
			this.edges.computeIfAbsent(edgeId, ignore -> new EdgeCache());
		} else {
			this.edges.remove(edgeId);
		}
	}

	/**
	 * Is the given Edge-ID connected via this Websocket?.
	 * 
	 * @param edgeId the Edge-ID
	 * @return true if known
	 */
	public boolean containsEdgeId(String edgeId) {
		return (this.edges.containsKey(edgeId));
	}

	/**
	 * Asserts that the Edge-ID is available.
	 *
	 * @param edgeId  the Edge-ID
	 * @param message a identification message on error
	 * @throws OpenemsException on error
	 */
	public void assertEdgeId(String edgeId, JsonrpcMessage message) throws OpenemsException {
		if (this.containsEdgeId(edgeId)) {
			return;
		}
		throw new OpenemsException(
				"Edge-ID is not set. Unable to handle " + StringUtils.toShortString(message.toString(), 100));
	}

	/**
	 * Gets the {@link EdgeCache} for the given Edge-ID.
	 * 
	 * @param edgeId the Edge-ID
	 * @return the {@link EdgeCache} or null
	 */
	public EdgeCache getEdgeCache(String edgeId) {
		return this.edges.get(edgeId);
	}

	/**
	 * Asserts that the {@link EdgeCache} is available.
	 * 
	 * @param edgeId the Edge-ID
	 * @return the {@link EdgeCache}
	 * @throws OpenemsException on error
	 */
	public EdgeCache assertEdgeCache(String edgeId) throws OpenemsException {
		final var edgeCache = this.getEdgeCache(edgeId);
		if (edgeCache == null) {
			throw new OpenemsException("[" + edgeId + "] No EdgeCache available");
		}
		return edgeCache;
	}

	/**
	 * Clears all Edges and returns the last known state.
	 * 
	 * @return previously connected EdgeIds
	 */
	protected Set<String> onClose() {
		var edgeIds = newHashSet(this.edges.keySet());
		this.edges.clear();
		return edgeIds;
	}

	@Override
	public String toLogString() {
		return "EdgeManager.WsData [" + this.edges.size() + "]";
	}
}
