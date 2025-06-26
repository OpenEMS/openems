package io.openems.edge.controller.api.backend.handler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.service.component.annotations.Component;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
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

	private final Set<WebsocketClient> subscriber = ConcurrentHashMap.newKeySet();

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(SubscribeSystemLogRequest.METHOD, call -> {
			final var webSocket = call.get(ControllerApiBackendImpl.WEBSOCKET_CLIENT_KEY);
			if (webSocket == null) {
				throw new OpenemsException("Websocket is not defined.");
			}
			final var request = SubscribeSystemLogRequest.from(call.getRequest());
			if (request.isSubscribe()) {
				this.subscriber.add(webSocket);
			} else {
				this.subscriber.remove(webSocket);
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
		final var iterator = this.subscriber.iterator();
		while (iterator.hasNext()) {
			final var ws = iterator.next();
			if (!ws.sendMessage(notification)) {
				iterator.remove();
			}
		}
	}

}