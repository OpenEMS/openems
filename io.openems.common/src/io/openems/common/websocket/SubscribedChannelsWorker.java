package io.openems.common.websocket;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.notification.CurrentDataNotification;
import io.openems.common.jsonrpc.request.SubscribeChannelsRequest;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;

public abstract class SubscribedChannelsWorker {

	protected static final int UPDATE_INTERVAL_IN_SECONDS = 2;

	private final Logger log = LoggerFactory.getLogger(SubscribedChannelsWorker.class);

	/**
	 * Holds subscribed channels.
	 */
	private final TreeSet<ChannelAddress> channels = new TreeSet<>();

	/**
	 * Holds the scheduled task for currentData.
	 */
	private Optional<ScheduledFuture<?>> futureOpt = Optional.empty();

	protected final WsData parent;

	private int lastRequestCount = Integer.MIN_VALUE;

	public SubscribedChannelsWorker(WsData wsData) {
		this.parent = wsData;
	}

	/**
	 * Applies a SubscribeChannelsRequest.
	 *
	 * @param role    the Role - no specific level required
	 * @param request the SubscribeChannelsRequest
	 */
	public synchronized void handleSubscribeChannelsRequest(Role role, SubscribeChannelsRequest request) {
		if (this.lastRequestCount < request.getCount()) {
			this.setChannels(request.getChannels());
			this.lastRequestCount = request.getCount();
		}
	}

	/**
	 * Sets the subscribed Channels.
	 *
	 * @param channels Set of ChannelAddresses
	 */
	private synchronized void setChannels(Set<ChannelAddress> channels) {
		// stop current thread
		if (this.futureOpt.isPresent()) {
			this.futureOpt.get().cancel(true);
			this.futureOpt = Optional.empty();
		}

		// clear existing channels
		this.channels.clear();

		// set new channels
		this.channels.addAll(channels);

		if (!channels.isEmpty()) {
			// registered channels -> create new thread
			this.futureOpt = Optional.of(this.parent.scheduleWithFixedDelay(() -> {
				/*
				 * This task is executed regularly. Sends data to Websocket.
				 */
				var ws = this.parent.getWebsocket();
				if (ws == null || !ws.isOpen()) {
					// disconnected; stop worker
					this.dispose();
					return;
				}

				try {
					this.parent.send(this.getJsonRpcNotification(this.getCurrentData()));
				} catch (OpenemsException e) {
					this.log.warn("Unable to send SubscribedChannels: " + e.getMessage());
				}

			}, 0, SubscribedChannelsWorker.UPDATE_INTERVAL_IN_SECONDS, TimeUnit.SECONDS));
		}
	}

	/**
	 * Dispose and deactivate the {@link SubscribedChannelsWorker}.
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
	 * @return the {@link CurrentDataNotification}
	 */
	private CurrentDataNotification getCurrentData() {
		var result = new CurrentDataNotification();
		for (ChannelAddress channel : this.channels) {
			var value = this.getChannelValue(channel);
			result.add(channel, value);
		}
		return result;
	}

	protected abstract JsonElement getChannelValue(ChannelAddress channelAddress);

	protected abstract JsonrpcNotification getJsonRpcNotification(CurrentDataNotification currentData);
}
