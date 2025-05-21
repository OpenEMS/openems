package io.openems.edge.controller.api.websocket.handler;

import org.osgi.service.component.annotations.Component;

import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.SubscribeChannelsRequest;
import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.type.SubscribeChannelUpdate;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.websocket.OnRequest;

@Component(property = "entry=" + EdgeRpcRequestHandler.ENTRY_POINT)
public class SubscribeChannelsRequestHandler implements JsonApi {

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(SubscribeChannelsRequest.METHOD, call -> {
			final var request = SubscribeChannelsRequest.from(call.getRequest());
			call.get(OnRequest.WS_DATA_KEY).handleSubscribeChannelsRequest(request);

			return new GenericJsonrpcResponseSuccess(request.getId());
		});

		builder.handleRequest(new SubscribeChannelUpdate(), endpoint -> {
			endpoint.setDescription("""
					Subscribes to channel changes (create, update, delete).
					""");
			endpoint.applyRequestBuilder(request -> {
				request.addExample("Subscribe", new SubscribeChannelUpdate.Request(true));
				request.addExample("Unsubscribe", new SubscribeChannelUpdate.Request(false));
			});
		}, call -> {
			final var wsData = call.get(OnRequest.WS_DATA_KEY);
			wsData.setChannelChangesSubscribed(call.getRequest().subscribe());
			return EmptyObject.INSTANCE;
		});
	}

}
