package io.openems.edge.controller.api.websocket;

import com.google.gson.JsonElement;

import com.google.gson.JsonNull;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.notification.CurrentDataNotification;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;

import java.util.Set;
import java.util.TreeSet;

public class SubscribedChannelsWorker extends io.openems.common.websocket.SubscribedChannelsWorker {

    private final WebsocketApi parent;

    private final TreeSet<ChannelAddress> channels = new TreeSet<>();

    public SubscribedChannelsWorker(WebsocketApi parent, WsData wsData) {
        super(wsData);
        this.parent = parent;
    }

    /**
     * Gets a JSON-RPC Notification with all subscribed channels data
     *
     * @return the channel values as notification+
     */
    private CurrentDataNotification getCurrentData() {
        CurrentDataNotification result = new CurrentDataNotification();
        this.channels.forEach(channel ->
                result.add(channel, this.getChannelValue(channel)));
        return result;
    }

    protected JsonElement getChannelValue(ChannelAddress channelAddress) {
        try {
            Channel<?> channel = this.parent.componentManager.getChannel(channelAddress);
            return channel.value().asJson();
        } catch (IllegalArgumentException | OpenemsError.OpenemsNamedException e) {
            return JsonNull.INSTANCE;
        }
    }

    @Override
    protected JsonrpcNotification getJsonRpcNotification() {
        return new EdgeRpcNotification(WebsocketApi.EDGE_ID, this.getCurrentData());
    }

    @Override
    protected void putEdgeIdAndChannelAddress(String edgeId, Set<ChannelAddress> channels) {
        this.channels.addAll(channels);
    }

    @Override
    public void clearAll() {
        this.channels.clear();
    }
}
