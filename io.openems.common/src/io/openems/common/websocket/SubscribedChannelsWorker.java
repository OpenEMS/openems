package io.openems.common.websocket;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.types.ChannelAddress;

public abstract class SubscribedChannelsWorker {

	protected final static int UPDATE_INTERVAL_IN_SECONDS = 2;

	private final Logger log = LoggerFactory.getLogger(SubscribedChannelsWorker.class);

	/**
	 * Executor for subscriptions task
	 */
	private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	/**
	 * Holds the scheduled task for currentData
	 */
	private ScheduledFuture<?> future;

	protected final WsData wsData;

	private int lastRequestCount = Integer.MIN_VALUE;

	public SubscribedChannelsWorker(WsData wsData) {
		this.wsData = wsData;
	}

	/**
	 * Applies a SubscribeChannelsRequest.
	 * @param requestCount the count of the request
	 * @param permittedChannels the permitted channels
	 */
	public synchronized void handleSubscribeChannelsRequest(int requestCount, Set<ChannelAddress> permittedChannels, String edgeId) {
		if (this.lastRequestCount < requestCount) {
			this.setChannels(permittedChannels, edgeId);
			this.lastRequestCount = requestCount;
		}
	}

	/**
	 * This method sets the given channel and maps those to the edge.
	 *
	 * @param channels Set of ChannelAddresses
	 */
	protected synchronized void setChannels(Set<ChannelAddress> channels, String edgeId) {

		// there is an edge id given -> add the given channels to the edge
		// stop current thread
		if (this.future != null) {
			this.future.cancel(true);
			this.future = null;
		}

		if (channels == null || channels.isEmpty()) {
			// no channels given -> nothing to do
			return;
		}

		this.putEdgeIdAndChannelAddress(edgeId, channels);

		// registered channels -> create new thread
		this.future = this.executor.scheduleWithFixedDelay(this::doCyclicWork, 0, UPDATE_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);

		// reset the edge id for making sure that a new assignment does not get mixed with an old one
	}

	public abstract void clearAll();

	/**
	 * This method gets called cyclic from the executor and fetches the current information of the channels and sends
	 * the result to the UI via {@link OnNotification}
	 */
	private void doCyclicWork() {
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
			this.wsData.send(this.getJsonRpcNotification());
		} catch (OpenemsException e) {
			this.log.warn("Unable to send SubscribedChannels: " + e.getMessage());
		}
	}

	public void dispose() {
		// unsubscribe regular task
		if (this.future != null) {
			future.cancel(true);
		}
	}

	protected abstract JsonrpcNotification getJsonRpcNotification();

	protected abstract void putEdgeIdAndChannelAddress(String edgeId, Set<ChannelAddress> channels);

}
