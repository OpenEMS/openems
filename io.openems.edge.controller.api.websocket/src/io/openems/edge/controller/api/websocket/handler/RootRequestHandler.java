package io.openems.edge.controller.api.websocket.handler;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.jsonapi.MultipleJsonApiBinder;

@Component(service = { RootRequestHandler.class, JsonApi.class })
public class RootRequestHandler implements JsonApi {

	public static final String ENTRY_POINT = "edge.websocket.root";

	private final MultipleJsonApiBinder apiBinder = new MultipleJsonApiBinder();
	private final BindingRoutesJsonApiHandler routesJsonApiHandler;

	@Reference(//
			cardinality = ReferenceCardinality.MULTIPLE, //
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			target = "(entry=" + ENTRY_POINT + ")" //
	)
	protected void bindJsonApi(JsonApi jsonApi) {
		this.apiBinder.bindJsonApi(jsonApi);
		this.routesJsonApiHandler.setBuilder(this.apiBinder.getJsonApiBuilder());
	}

	protected void unbindJsonApi(JsonApi jsonApi) {
		this.apiBinder.unbindJsonApi(jsonApi);
		this.routesJsonApiHandler.setBuilder(null);
	}

	@Activate
	public RootRequestHandler(@Reference BindingRoutesJsonApiHandler routesJsonApiHandler) {
		this.routesJsonApiHandler = routesJsonApiHandler;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.addBuilder(this.apiBinder.getJsonApiBuilder());
	}

}
