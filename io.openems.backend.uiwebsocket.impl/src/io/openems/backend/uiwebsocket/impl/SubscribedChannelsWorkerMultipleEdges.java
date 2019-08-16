package io.openems.backend.uiwebsocket.impl;

import com.google.gson.JsonNull;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.notification.EdgesCurrentDataNotification;
import io.openems.common.types.ChannelAddress;

import java.util.Set;

/**
 * This Worker is able to handle several edges which several channels.<br>
 * As soon as a edgeId and a list of channels have been given, this worker send cyclic messages via the given Websocket
 */
public class SubscribedChannelsWorkerMultipleEdges extends io.openems.common.websocket.SubscribedChannelsWorkerMultipleEdges {

    private final UiWebsocketImpl parent;


    public SubscribedChannelsWorkerMultipleEdges(UiWebsocketImpl parent, WsData wsData) {
        super(wsData);
        this.parent = parent;
    }

    /**
     * Fetches all current data from the timedata for all subscribed edges and returns the
     * result as {@link JsonrpcNotification}
     *
     * @return the notificiation
     */
    @Override
    protected JsonrpcNotification getJsonRpcNotification() {
        EdgesCurrentDataNotification notification = new EdgesCurrentDataNotification();
        this.edgeIdToChannel.forEach(
                (edgeId, channelAddresses) -> channelAddresses.forEach(
                        address -> notification.addValue(
                                edgeId, address,
                                this.parent.timeData.getChannelValue(edgeId, address).orElse(JsonNull.INSTANCE))));

        return notification;
    }

    @Override
    protected void putEdgeIdAndChannelAddress(String edgeId, Set<ChannelAddress> channels) {
        this.edgeIdToChannel.putIfAbsent(edgeId, channels);
    }

}
