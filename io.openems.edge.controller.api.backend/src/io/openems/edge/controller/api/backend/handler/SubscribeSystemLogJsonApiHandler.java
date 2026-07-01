package io.openems.edge.controller.api.backend.handler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.service.component.annotations.Component;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.notification.SystemLogNotification;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.common.jsonrpc.response.AuthenticatedRpcResponse;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.backend.ControllerApiBackendImpl;
import io.openems.edge.controller.api.backend.WebsocketClient;

@Component(property = { //
		"entry=" + AuthenticatedRequestHandler.ENTRY_POINT, //
		"org.ops4j.pax.logging.appender.name=Controller.Api.Backend", //
})
public class SubscribeSystemLogJsonApiHandler implements JsonApi, PaxAppender {

	record Subscriber(WebsocketClient webSocket, Function<JsonrpcNotification, JsonrpcNotification> wrap) {

	}

	private final Set<Subscriber> subscriber = ConcurrentHashMap.newKeySet();

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(SubscribeSystemLogRequest.METHOD, call -> {
			final var webSocket = call.get(ControllerApiBackendImpl.WEBSOCKET_CLIENT_KEY);
			if (webSocket == null) {
				throw new OpenemsException("Websocket is not defined.");
			}
			final var request = SubscribeSystemLogRequest.from(call.getRequest());
			final var wrapper = call.get(ControllerApiBackendImpl.NOTIFICATION_WRAPPER_KEY);
			if (request.isSubscribe()) {
				this.subscribe(webSocket, wrapper);
			} else {
				this.unsubscribe(webSocket);
			}

			return new AuthenticatedRpcResponse(call.getRequest().getId(),
					new GenericJsonrpcResponseSuccess(request.getId()));
		});
	}

	@Override
	public void doAppend(PaxLoggingEvent event) {
		if (this.subscriber.isEmpty()) {
			return;
		}

		final var notification = SystemLogNotification.fromPaxLoggingEvent(event);
		this.sendSystemLogNotification(notification);
	}

	void subscribe(WebsocketClient webSocket, Function<JsonrpcNotification, JsonrpcNotification> wrapper) {
		this.unsubscribe(webSocket);
		this.subscriber.add(new Subscriber(webSocket, wrapper == null ? Function.identity() : wrapper));
	}

	void unsubscribe(WebsocketClient webSocket) {
		this.subscriber.removeIf(subscriber -> subscriber.webSocket() == webSocket);
	}

	void sendSystemLogNotification(SystemLogNotification notification) {
		final var iterator = this.subscriber.iterator();
		while (iterator.hasNext()) {
			final var subscriber = iterator.next();
			if (!subscriber.webSocket().sendMessage(subscriber.wrap().apply(notification))) {
				iterator.remove();
			}
		}
	}

}
