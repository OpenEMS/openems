package io.openems.backend.uiwebsocket.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.notification.CurrentDataNotification;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.types.ChannelAddress;

public class SubscribedChannelsWorker extends io.openems.common.websocket.SubscribedChannelsWorker {

	private final UiWebsocketImpl parent;
	private final String edgeId;

	public SubscribedChannelsWorker(UiWebsocketImpl parent, String edgeId, WsData wsData) {
		super(wsData);
		this.parent = parent;
		this.edgeId = edgeId;
	}

	@Override
	protected JsonElement getChannelValue(ChannelAddress channelAddress) {
		if (this.edgeId == null) {
			return JsonNull.INSTANCE;
		}

		var channelCacheValueOpt = this.parent.timeData.getChannelValue(this.edgeId, channelAddress);
		return channelCacheValueOpt.orElse(JsonNull.INSTANCE);
	}

	@Override
	protected JsonrpcNotification getJsonRpcNotification(CurrentDataNotification currentData) {
		return new EdgeRpcNotification(this.edgeId, currentData);
	}
}
