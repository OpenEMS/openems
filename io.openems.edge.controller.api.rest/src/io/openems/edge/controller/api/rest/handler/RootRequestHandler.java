package io.openems.edge.controller.api.rest.handler;

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

	public static final String ENTRY_POINT = "edge.rest.root";

	private final MultipleJsonApiBinder binder = new MultipleJsonApiBinder();

	/**
	 * Binds a {@link JsonApi2}.
	 * 
	 * @param jsonApi the {@link JsonApi2} to bind
	 */
	@Reference(//
			target = "(entry=" + ENTRY_POINT + ")", //
			cardinality = ReferenceCardinality.MULTIPLE, //
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY //
	)
	public void bindJsonApi(JsonApi jsonApi) {
		this.binder.bindJsonApi(jsonApi);
	}

	/**
	 * Unbinds a {@link JsonApi2}.
	 * 
	 * @param jsonApi the {@link JsonApi2} to unbind
	 */
	public void unbindJsonApi(JsonApi jsonApi) {
		this.binder.unbindJsonApi(jsonApi);
	}

	@Activate
	public RootRequestHandler(@Reference BindingRoutesJsonApiHandler routesJsonApiHandler) {
		routesJsonApiHandler.setBuilder(this.binder.getJsonApiBuilder());
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.addBuilder(this.binder.getJsonApiBuilder());
	}

}
