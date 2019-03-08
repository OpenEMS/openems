package io.openems.edge.controller.api.websocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.notification.CurrentDataNotification;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;

public class SubscribedChannelsWorker extends io.openems.common.websocket.SubscribedChannelsWorker {

	private final WebsocketApi parent;

	public SubscribedChannelsWorker(WebsocketApi parent, WsData wsData) {
		super(wsData);
		this.parent = parent;
	}

	@Override
	protected JsonElement getChannelValue(ChannelAddress channelAddress) {
		try {
			Channel<?> channel = this.parent.componentManager.getChannel(channelAddress);
			return channel.value().asJson();
		} catch (IllegalArgumentException | OpenemsNamedException e) {
			return JsonNull.INSTANCE;
		}
	}

	@Override
	protected JsonrpcNotification getJsonRpcNotification(CurrentDataNotification currentData) {
		return new EdgeRpcNotification(WebsocketApi.EDGE_ID, currentData);
	}
}
