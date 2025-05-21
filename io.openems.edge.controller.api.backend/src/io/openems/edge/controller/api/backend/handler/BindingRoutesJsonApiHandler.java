package io.openems.edge.controller.api.backend.handler;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;

import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.common.handler.RoutesJsonApiHandler;

@Component(//
		property = { "entry=" + AuthenticatedRequestHandler.ENTRY_POINT }, //
		service = { BindingRoutesJsonApiHandler.class, JsonApi.class } //
)
public class BindingRoutesJsonApiHandler implements JsonApi {

	private final RoutesJsonApiHandler jsonApiHandler;

	@Activate
	public BindingRoutesJsonApiHandler(//
			@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) RoutesJsonApiHandler jsonApiHandler //
	) {
		this.jsonApiHandler = jsonApiHandler;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		this.jsonApiHandler.buildJsonApiRoutes(builder);
	}

	public JsonApiBuilder getBuilder() {
		return this.jsonApiHandler.getBuilder();
	}

	public void setBuilder(JsonApiBuilder builder) {
		this.jsonApiHandler.setBuilder(builder);
	}

}