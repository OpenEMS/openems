package io.openems.edge.controller.api.rest.handler;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.controller.api.common.handler.QueryRequestHandler;

@Component(property = "entry=" + RootRequestHandler.ENTRY_POINT)
public class BindingQueryRequestHandler implements JsonApi {

	private final QueryRequestHandler queryRequestHandler;

	@Activate
	public BindingQueryRequestHandler(@Reference QueryRequestHandler handler) {
		this.queryRequestHandler = handler;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		this.queryRequestHandler.buildJsonApiRoutes(builder);
	}

}
