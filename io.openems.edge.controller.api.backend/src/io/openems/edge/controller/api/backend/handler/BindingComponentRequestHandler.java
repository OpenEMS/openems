package io.openems.edge.controller.api.backend.handler;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.common.handler.ComponentRequestHandler;

@Component(property = { "entry=" + AuthenticatedRequestHandler.ENTRY_POINT })
public class BindingComponentRequestHandler implements JsonApi {

	private final ComponentRequestHandler componentRequestHandler;

	@Activate
	public BindingComponentRequestHandler(//
			@Reference ComponentRequestHandler componentRequestHandler //
	) {
		this.componentRequestHandler = componentRequestHandler;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		this.componentRequestHandler.buildJsonApiRoutes(builder);
	}

}