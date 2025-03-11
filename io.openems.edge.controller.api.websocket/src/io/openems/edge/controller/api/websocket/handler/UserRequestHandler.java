package io.openems.edge.controller.api.websocket.handler;

import org.osgi.service.component.annotations.Component;

import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.UpdateUserSettingsRequest;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;

@Component(property = "entry=" + RootRequestHandler.ENTRY_POINT)
public class UserRequestHandler implements JsonApi {

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {

		builder.handleRequest(UpdateUserSettingsRequest.METHOD,
				call -> new GenericJsonrpcResponseSuccess(call.getRequest().getId()));
	}

}
