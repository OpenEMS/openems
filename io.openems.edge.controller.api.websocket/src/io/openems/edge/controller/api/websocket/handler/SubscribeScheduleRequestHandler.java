package io.openems.edge.controller.api.websocket.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.notification.ScheduleNotification;
import io.openems.common.jsonrpc.request.SubscribeScheduleRequest;
import io.openems.common.jsonrpc.type.ScheduleUpdate;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.websocket.OnRequest;
import io.openems.edge.controller.api.websocket.WsData;
import io.openems.edge.energy.api.EnergyScheduler;

@Component(property = { //
		"entry=" + EdgeRpcRequestHandler.ENTRY_POINT //
})
public class SubscribeScheduleRequestHandler implements JsonApi {

	@Reference
	private EnergyScheduler energyScheduler;

	private final Map<WsData, Consumer<ScheduleUpdate>> subscribers = new ConcurrentHashMap<>();

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(SubscribeScheduleRequest.METHOD, call -> {
			final var request = SubscribeScheduleRequest.from(call.getRequest());
			final var wsData = call.get(OnRequest.WS_DATA_KEY);

			if (request.isSubscribe()) {
				this.subscribe(wsData);
			} else {
				this.unsubscribe(wsData);
			}

			return new GenericJsonrpcResponseSuccess(request.getId());
		});
	}

	private void subscribe(WsData wsData) {
		final Consumer<ScheduleUpdate> consumer = //
				scheduleUpdate -> this.sendScheduleNotification(wsData, scheduleUpdate);

		final var previousConsumer = this.subscribers.put(wsData, consumer);

		if (previousConsumer != null) {
			this.energyScheduler.unsubscribeSchedule(previousConsumer);
		}

		this.energyScheduler.subscribeSchedule(consumer);
	}

	private void unsubscribe(WsData wsData) {
		final var consumer = this.subscribers.remove(wsData);

		if (consumer != null) {
			this.energyScheduler.unsubscribeSchedule(consumer);
		}
	}

	private void sendScheduleNotification(//
			WsData wsData, //
			ScheduleUpdate scheduleUpdate) {
		if (!wsData.send(new ScheduleNotification(scheduleUpdate))) {
			this.unsubscribe(wsData);
		}
	}
}
