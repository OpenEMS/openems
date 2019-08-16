package io.openems.common.websocket;

import com.google.gson.JsonNull;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.notification.EdgesCurrentDataNotification;
import io.openems.common.types.ChannelAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * This Worker is able to handle several edges which several channels.<br>
 * As soon as a edgeId and a list of channels have been given, this worker send cyclic messages via the given Websocket
 */
public abstract class SubscribedChannelsWorkerMultipleEdges extends SubscribedChannelsWorker {

    /**
     * Mapping between registered edges with their channels
     */
    protected final Map<String, Set<ChannelAddress>> edgeIdToChannel = new HashMap<>();
    private final Logger log = LoggerFactory.getLogger(SubscribedChannelsWorkerMultipleEdges.class);

    /**
     * Executor for subscriptions task
     */
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    /**
     * Holds the scheduled task for currentData
     */
    private ScheduledFuture<?> future;

    public SubscribedChannelsWorkerMultipleEdges(WsData wsData) {
        super(wsData);
    }

    @Override
    protected void putEdgeIdAndChannelAddress(String edgeId, Set<ChannelAddress> channels) {
        this.edgeIdToChannel.putIfAbsent(edgeId, channels);
    }

    @Override
    public void clearAll() {
        this.edgeIdToChannel.clear();
    }
}
