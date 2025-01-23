package io.openems.edge.controller.api.websocket.handler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.service.component.annotations.Component;

import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.notification.EdgeRpcNotification;
import io.openems.common.jsonrpc.notification.SystemLogNotification;
import io.openems.common.jsonrpc.request.SubscribeSystemLogRequest;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.websocket.ControllerApiWebsocket;
import io.openems.edge.controller.api.websocket.OnRequest;
import io.openems.edge.controller.api.websocket.WsData;

@Component(property = { //
		"entry=" + EdgeRpcRequestHandler.ENTRY_POINT, //
		"org.ops4j.pax.logging.appender.name=Controller.Api.Websocket" //
})
public class SubscribeSystemLogRequestHandler implements JsonApi, PaxAppender {

	private final Set<WsData> subscribers = ConcurrentHashMap.newKeySet();

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(SubscribeSystemLogRequest.METHOD, call -> {
			final var request = SubscribeSystemLogRequest.from(call.getRequest());
			final var wsData = call.get(OnRequest.WS_DATA_KEY);

			if (request.isSubscribe()) {
				this.subscribers.add(wsData);
			} else {
				this.subscribers.remove(wsData);
			}

			return new GenericJsonrpcResponseSuccess(request.getId());
		});
	}

	@Override
	public void doAppend(PaxLoggingEvent event) {
		if (this.subscribers.isEmpty()) {
			return;
		}

		final var notification = new EdgeRpcNotification(ControllerApiWebsocket.EDGE_ID,
				SystemLogNotification.fromPaxLoggingEvent(event));

		final var iter = this.subscribers.iterator();
		while (iter.hasNext()) {
			final var wsData = iter.next();

			if (wsData.getWebsocket().isFlushAndClose()) {
				iter.remove();
				continue;
			}
			wsData.send(notification);
		}
	}

}
