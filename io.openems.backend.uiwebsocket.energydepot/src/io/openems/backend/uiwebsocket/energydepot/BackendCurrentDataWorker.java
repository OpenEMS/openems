package io.openems.backend.uiwebsocket.energydepot;

import java.util.Optional;

import org.java_websocket.WebSocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.CurrentDataWorker;

public class BackendCurrentDataWorker extends CurrentDataWorker {

	private final UiWebsocketServer parent;
	private final int edgeId;

	public BackendCurrentDataWorker(UiWebsocketServer parent, WebSocket websocket, int edgeId) {
		super(websocket);
		this.parent = parent;
		this.edgeId = edgeId;
	}

	@Override
	protected JsonElement getChannelValue(ChannelAddress channelAddress) {
		Optional<Object> channelCacheOpt = this.parent.parent.timeDataService.getChannelValue(this.edgeId,
				channelAddress);
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
}
