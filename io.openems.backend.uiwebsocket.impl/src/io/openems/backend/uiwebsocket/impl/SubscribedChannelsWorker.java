package io.openems.backend.uiwebsocket.impl;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.notification.CurrentData;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;

public class SubscribedChannelsWorker extends io.openems.common.websocket.SubscribedChannelsWorker {

	private final UiWebsocketImpl parent;

	private String edgeId = null;

	public SubscribedChannelsWorker(UiWebsocketImpl parent, WsData wsData) {
		super(wsData);
		this.parent = parent;
	}

	public void setEdgeId(String edgeId) {
		this.edgeId = edgeId;
	}

	@Override
	protected JsonElement getChannelValue(ChannelAddress channelAddress) {
		if (this.edgeId == null) {
			return JsonNull.INSTANCE;
		}

		Optional<Object> channelCacheOpt = this.parent.timeData.getChannelValue(this.edgeId, channelAddress);
		if (channelCacheOpt.isPresent()) {
			try {
				return JsonUtils.getAsJsonElement(channelCacheOpt.get());
			} catch (NotImplementedException e) {
				return JsonNull.INSTANCE;
			}
		} else {
			return JsonNull.INSTANCE;
		}
	}

	@Override
	protected JsonrpcNotification getJsonRpcNotification(CurrentData currentData) {
		return new EdgeRpcNotification(this.edgeId, currentData);
	}
}
