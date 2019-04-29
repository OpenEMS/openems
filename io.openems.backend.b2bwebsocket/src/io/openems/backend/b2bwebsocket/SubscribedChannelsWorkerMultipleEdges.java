package io.openems.backend.b2bwebsocket;

import com.google.gson.JsonNull;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.notification.EdgesCurrentDataNotification;

/**
 * This Worker is able to handle several edges which several channels.<br>
 * As soon as a edgeId and a list of channels have been given, this worker send cyclic messages via the given Websocket
 */
public class SubscribedChannelsWorkerMultipleEdges extends io.openems.common.websocket.SubscribedChannelsWorkerMultipleEdges {

    private final B2bWebsocket parent;

    public SubscribedChannelsWorkerMultipleEdges(B2bWebsocket parent, WsData wsData) {
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
}
