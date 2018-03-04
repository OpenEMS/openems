package io.openems.core.utilities.websocket;

import java.util.Optional;

import org.java_websocket.WebSocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.api.channel.Channel;
import io.openems.common.exceptions.AccessDeniedException;
import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.common.websocket.CurrentDataWorker;
import io.openems.common.websocket.LogBehaviour;
import io.openems.common.websocket.Notification;
import io.openems.common.websocket.WebSocketUtils;
import io.openems.core.Databus;
import io.openems.core.ThingRepository;

public class EdgeCurrentDataWorker extends CurrentDataWorker {

	private final ThingRepository thingRepository;
	private final Databus databus;

	/**
	 * The access level Role of this worker
	 */
	private final Role role;

	public EdgeCurrentDataWorker(EdgeWebsocketHandler edgeWebsocketHandler, WebSocket websocket, Role role) {
		super(websocket);
		this.role = role;
		this.thingRepository = ThingRepository.getInstance();
		this.databus = Databus.getInstance();
	}

	@Override
	protected Optional<JsonElement> getChannelValue(ChannelAddress channelAddress) {
		Optional<Channel> channelOpt = thingRepository.getChannel(channelAddress);
		if (channelOpt.isPresent()) {
			Channel channel = channelOpt.get();
			try {
				channel.assertReadAllowed(this.role);
			} catch (AccessDeniedException e) {
				WebSocketUtils.sendNotificationOrLogError(this.websocket, new JsonObject(), LogBehaviour.WRITE_TO_LOG,
						Notification.CHANNEL_ACCESS_DENIED, channelAddress, this.role);
				return Optional.empty();
			}
			try {
				return Optional.ofNullable(JsonUtils.getAsJsonElement(databus.getValue(channel).orElse(null)));
			} catch (NotImplementedException e) {
				WebSocketUtils.sendNotificationOrLogError(this.websocket, new JsonObject(), LogBehaviour.WRITE_TO_LOG,
						Notification.VALUE_CONVERSION_FAILED, channelAddress, e.getMessage());
				return Optional.empty();
			}
		} else {
			WebSocketUtils.sendNotificationOrLogError(this.websocket, new JsonObject(), LogBehaviour.WRITE_TO_LOG,
					Notification.CHANNEL_NOT_FOUND, channelAddress);
			return Optional.empty();
		}
	}
}
