package io.openems.backend.b2bwebsocket;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.b2bwebsocket.jsonrpc.notification.EdgesCurrentDataNotification;
import io.openems.backend.b2bwebsocket.jsonrpc.request.SubscribeEdgesChannelsRequest;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;

public class SubscribedEdgesChannelsWorker {

	protected static final int UPDATE_INTERVAL_IN_SECONDS = 2;

	private final Logger log = LoggerFactory.getLogger(SubscribedEdgesChannelsWorker.class);

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
			this.futureOpt = Optional.of(this.parent.executor.scheduleWithFixedDelay(() -> {
				/*
				 * This task is executed regularly. Sends data to Websocket.
				 */
				var ws = this.wsData.getWebsocket();
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

			}, 0, SubscribedEdgesChannelsWorker.UPDATE_INTERVAL_IN_SECONDS, TimeUnit.SECONDS));
		}
	}

	/**
	 * Dispose and deactivate this worker.
	 */
	public void dispose() {
		// unsubscribe regular task
		if (this.futureOpt.isPresent()) {
			this.futureOpt.get().cancel(true);
		}
	}

	/**
	 * Gets a JSON-RPC Notification with all subscribed channels data.
	 *
	 * @return the EdgesCurrentDataNotification
	 * @throws OpenemsNamedException on error
	 */
	private EdgesCurrentDataNotification getCurrentDataNotification() throws OpenemsNamedException {
		var result = new EdgesCurrentDataNotification();
		var user = this.wsData.getUserWithTimeout(5, TimeUnit.SECONDS);

		for (String edgeId : this.edgeIds) {
			// assure read permissions of this User for this Edge.
			user.assertEdgeRoleIsAtLeast("EdgesCurrentDataNotification", edgeId, Role.GUEST);

			var data = this.parent.edgeWebsocket.getChannelValues(edgeId, this.channels);
			for (var entry : data.entrySet()) {
				result.addValue(edgeId, entry.getKey(), entry.getValue());
			}
		}
		return result;
	}

}
