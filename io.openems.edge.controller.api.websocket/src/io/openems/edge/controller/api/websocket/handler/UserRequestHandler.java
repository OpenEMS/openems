package io.openems.edge.controller.api.websocket.handler;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.UpdateUserLanguageRequest;
import io.openems.common.jsonrpc.request.UpdateUserSettingsRequest;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.user.UserService;

@Component(property = "entry=" + RootRequestHandler.ENTRY_POINT)
public class UserRequestHandler implements JsonApi {

	@Reference
	private UserService userService;

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {

		builder.handleRequest(UpdateUserSettingsRequest.METHOD,
				call -> new GenericJsonrpcResponseSuccess(call.getRequest().getId()));

		builder.handleRequest(UpdateUserLanguageRequest.METHOD, call -> {
			final var request = UpdateUserLanguageRequest.from(call.getRequest());
			this.userService.updateLanguage(request.getLanguage());

			return new GenericJsonrpcResponseSuccess(call.getRequest().getId());
		});
	}

}
