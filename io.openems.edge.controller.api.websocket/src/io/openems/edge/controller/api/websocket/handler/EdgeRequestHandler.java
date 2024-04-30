package io.openems.edge.controller.api.websocket.handler;

import java.util.List;

import org.osgi.service.component.annotations.Component;

import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.GetEdgeRequest;
import io.openems.common.jsonrpc.request.GetEdgesRequest;
import io.openems.common.jsonrpc.request.SubscribeEdgesRequest;
import io.openems.common.jsonrpc.response.GetEdgeResponse;
import io.openems.common.jsonrpc.response.GetEdgesResponse;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.websocket.Utils;

@Component(property = "entry=" + RootRequestHandler.ENTRY_POINT)
public class EdgeRequestHandler implements JsonApi {

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(GetEdgesRequest.METHOD, call -> {
			final var user = call.get(EdgeKeys.USER_KEY);
			return new GetEdgesResponse(call.getRequest().getId(),
					List.of(Utils.getEdgeMetadata(user.getGlobalRole())));
		});

		builder.handleRequest(GetEdgeRequest.METHOD, call -> {
			final var user = call.get(EdgeKeys.USER_KEY);
			return new GetEdgeResponse(call.getRequest().getId(), Utils.getEdgeMetadata(user.getGlobalRole()));
		});

		builder.handleRequest(SubscribeEdgesRequest.METHOD, call -> new GenericJsonrpcResponseSuccess(call.getRequest().getId()));
	}

}
