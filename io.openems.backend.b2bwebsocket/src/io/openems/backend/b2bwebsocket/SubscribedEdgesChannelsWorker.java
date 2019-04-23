package io.openems.backend.b2bwebsocket;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.backend.b2bwebsocket.jsonrpc.notification.EdgesCurrentDataNotification;
import io.openems.backend.b2bwebsocket.jsonrpc.request.SubscribeEdgesChannelsRequest;
import io.openems.backend.metadata.api.BackendUser;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;

public class SubscribedEdgesChannelsWorker {

	protected static final int UPDATE_INTERVAL_IN_SECONDS = 2;

	private final Logger log = LoggerFactory.getLogger(SubscribedEdgesChannelsWorker.class);

	/**
	 * Executor for subscriptions task.
	 */
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	/**
	 * Holds subscribed edges.
	 */
	private final TreeSet<String> edgeIds = new TreeSet<>();

	/**
	 * Holds subscribed channels.
	 */
	private final TreeSet<ChannelAddress> channels = new TreeSet<>();

	/**
	 * Holds the scheduled task for currentData.
	 */
	private Optional<ScheduledFuture<?>> futureOpt = Optional.empty();

	protected final WsData wsData;

	private int lastRequestCount = Integer.MIN_VALUE;

	private final B2bWebsocket parent;

	public SubscribedEdgesChannelsWorker(B2bWebsocket parent, WsData wsData) {
		this.parent = parent;
		this.wsData = wsData;
	}

	/**
	 * Applies a SubscribeChannelsRequest.
	 * 
	 * @param request the SubscribeEdgesChannelsRequest
	 */
	public synchronized void handleSubscribeEdgesChannelsRequest(SubscribeEdgesChannelsRequest request) {
		if (this.lastRequestCount < request.getCount()) {
			this.updateSubscription(request.getEdgeIds(), request.getChannels());
			this.lastRequestCount = request.getCount();
		}
	}

	/**
	 * Updates the Subscription data.
	 * 
	 * @param edgeIds  Set of Edge-IDs
	 * @param channels Set of ChannelAddresses
	 */
	private synchronized void updateSubscription(Set<String> edgeIds, Set<ChannelAddress> channels) {
		// stop current thread
		if (this.futureOpt.isPresent()) {
			this.futureOpt.get().cancel(true);
			this.futureOpt = Optional.empty();
		}

		// clear existing data
		this.edgeIds.clear();
		this.channels.clear();

		// set new channels
		this.edgeIds.addAll(edgeIds);
		this.channels.addAll(channels);

		if (!channels.isEmpty() && !edgeIds.isEmpty()) {
			// registered channels -> create new thread
			this.futureOpt = Optional.of(this.executor.scheduleWithFixedDelay(() -> {
				/*
				 * This task is executed regularly. Sends data to Websocket.
				 */
				WebSocket ws = this.wsData.getWebsocket();
				if (ws == null || !ws.isOpen()) {
					// disconnected; stop worker
					this.dispose();
					return;
				}

				try {
					this.wsData.send(this.getCurrentDataNotification());
				} catch (OpenemsNamedException e) {
					this.log.warn("Unable to send SubscribedChannels: " + e.getMessage());
				}

			}, 0, UPDATE_INTERVAL_IN_SECONDS, TimeUnit.SECONDS));
		}
	}

	public void dispose() {
		// unsubscribe regular task
		if (this.futureOpt.isPresent()) {
			futureOpt.get().cancel(true);
		}
	}

	/**
	 * Gets a JSON-RPC Notification with all subscribed channels data.
	 *
	 * @return the EdgesCurrentDataNotification
	 * @throws OpenemsNamedException on error
	 */
	private EdgesCurrentDataNotification getCurrentDataNotification() throws OpenemsNamedException {
		EdgesCurrentDataNotification result = new EdgesCurrentDataNotification();
		BackendUser user = this.wsData.getUserWithTimeout(5, TimeUnit.SECONDS);

		for (String edgeId : this.edgeIds) {
			// assure read permissions of this User for this Edge.
			user.assertEdgeRoleIsAtLeast("EdgesCurrentDataNotification", edgeId, Role.GUEST);

			for (ChannelAddress channel : this.channels) {
				Optional<JsonElement> value = this.parent.timeData.getChannelValue(edgeId, channel);
				result.addValue(edgeId, channel, value.orElse(JsonNull.INSTANCE));
			}
		}
		return result;
	}

}
