package io.openems.edge.controller.api.backend.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.notification.ScheduleNotification;
import io.openems.common.jsonrpc.request.SubscribeScheduleRequest;
import io.openems.common.jsonrpc.response.AuthenticatedRpcResponse;
import io.openems.common.jsonrpc.type.ScheduleUpdate;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.backend.ControllerApiBackendImpl;
import io.openems.edge.controller.api.backend.WebsocketClient;
import io.openems.edge.energy.api.EnergyScheduler;

@Component(property = { //
		"entry=" + AuthenticatedRequestHandler.ENTRY_POINT //
})
public class SubscribeScheduleJsonApiHandler implements JsonApi {

	@Reference
	private EnergyScheduler energyScheduler;

	private final Map<WebsocketClient, Consumer<ScheduleUpdate>> subscribers = new ConcurrentHashMap<>();

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(SubscribeScheduleRequest.METHOD, call -> {
			final var webSocket = call.get(ControllerApiBackendImpl.WEBSOCKET_CLIENT_KEY);
			if (webSocket == null) {
				throw new OpenemsException("Websocket is not defined.");
			}

			final var request = SubscribeScheduleRequest.from(call.getRequest());

			if (request.isSubscribe()) {
				this.subscribe(webSocket);
			} else {
				this.unsubscribe(webSocket);
			}

			return new AuthenticatedRpcResponse(//
					call.getRequest().getId(), //
					new GenericJsonrpcResponseSuccess(request.getId()));
		});
	}

	private void subscribe(WebsocketClient webSocket) {
		final Consumer<ScheduleUpdate> consumer = //
				scheduleUpdate -> this.sendScheduleNotification(webSocket, scheduleUpdate);

		final var previousConsumer = this.subscribers.put(webSocket, consumer);

		if (previousConsumer != null) {
			this.energyScheduler.unsubscribeSchedule(previousConsumer);
		}

		this.energyScheduler.subscribeSchedule(consumer);
	}

	private void unsubscribe(WebsocketClient webSocket) {
		final var consumer = this.subscribers.remove(webSocket);

		if (consumer != null) {
			this.energyScheduler.unsubscribeSchedule(consumer);
		}
	}

	private void sendScheduleNotification(//
			WebsocketClient webSocket, //
			ScheduleUpdate scheduleUpdate) {
		if (!webSocket.sendMessage(new ScheduleNotification(scheduleUpdate))) {
			this.unsubscribe(webSocket);
		}
	}
}
