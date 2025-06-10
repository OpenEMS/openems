package io.openems.edge.controller.api.backend.handler;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.jsonapi.MultipleJsonApiBinder;

@Component(//
		service = { RootRequestHandler.class, JsonApi.class }, //
		scope = ServiceScope.SINGLETON //
)
public class RootRequestHandler implements JsonApi {

	public static final String ENTRY_POINT = "edge.backend.root";

	private final MultipleJsonApiBinder binder = new MultipleJsonApiBinder();
	private final BindingRoutesJsonApiHandler routesHandler;

	@Reference(//
			target = "(entry=" + ENTRY_POINT + ")", //
			policyOption = ReferencePolicyOption.GREEDY, //
			policy = ReferencePolicy.DYNAMIC, //
			cardinality = ReferenceCardinality.MULTIPLE //
	)
	protected void bindHandler(JsonApi handler) {
		this.binder.bindJsonApi(handler);
		this.routesHandler.setBuilder(this.binder.getJsonApiBuilder());
	}

	protected void unbindHandler(JsonApi handler) {
		this.binder.unbindJsonApi(handler);
		this.routesHandler.setBuilder(null);
	}

	@Activate
	public RootRequestHandler(@Reference BindingRoutesJsonApiHandler routesHandler) {
		this.routesHandler = routesHandler;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.addBuilder(this.binder.getJsonApiBuilder());
	}

}
