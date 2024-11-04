package io.openems.edge.controller.api.common.handler;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.jsonapi.JsonApiEndpoint;
import io.openems.edge.common.jsonapi.Subrequest;
import io.openems.edge.common.jsonapi.Tag;

/**
 * This class handles {@link ComponentJsonApiRequest} and delegates the request
 * to their component.
 */
@Component(service = { ComponentRequestHandler.class, JsonApi.class }, scope = ServiceScope.SINGLETON)
public class ComponentRequestHandler implements JsonApi {

	private record BoundComponentJsonApi(//
			long serviceId, //
			JsonApiBuilder apiBuilder, //
			Consumer<JsonApiEndpoint> endpointListener //
	) {

		public ComponentRequestHandler.BoundComponentJsonApi with(Consumer<JsonApiEndpoint> endpointListener) {
			return new BoundComponentJsonApi(this.serviceId, this.apiBuilder, endpointListener);
		}

	}

	private final Logger log = LoggerFactory.getLogger(ComponentRequestHandler.class);

	private final Map<String, BoundComponentJsonApi> jsonApis = new TreeMap<>();

	/**
	 * Binds a {@link ComponentJsonApi}.
	 * 
	 * @param jsonApi the {@link ComponentJsonApi} to bind
	 * @param ref     the OSGi component properties
	 */
	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			bind = "bindJsonApi", unbind = "unbindJsonApi", //
			updated = "updateJsonApi", //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE //
	)
	public void bindJsonApi(ComponentJsonApi jsonApi, Map<String, ?> ref) {
		final var builder = new JsonApiBuilder();

		final var boundComponent = new BoundComponentJsonApi(//
				(Long) ref.get(Constants.SERVICE_ID), //
				builder, //
				t -> t.getDef().getTags().add(new Tag(jsonApi.id())) //
		);

		builder.addEndpointAddedListener(boundComponent.endpointListener());
		jsonApi.buildJsonApiRoutes(builder);
		this.jsonApis.put(jsonApi.id(), boundComponent);
		this.log.info("Added '" + jsonApi.id() + "' to Component Apis.");

	}

	/**
	 * Updates a {@link ComponentJsonApi} on configuration change.
	 * 
	 * @param jsonApi the {@link ComponentJsonApi} to update
	 * @param ref     the updated OSGi component properties
	 */
	public void updateJsonApi(ComponentJsonApi jsonApi, Map<String, ?> ref) {
		final long servicePid = (Long) ref.get(Constants.SERVICE_ID);
		for (var entry : this.jsonApis.entrySet()) {
			if (entry.getValue().serviceId() != servicePid) {
				continue;
			}
			final var previousComponentId = entry.getKey();
			final var prevBinding = this.jsonApis.remove(previousComponentId);
			for (var endpoint : prevBinding.apiBuilder().getEndpoints().values()) {
				endpoint.getDef().getTags().removeIf(t -> t.name().equals(previousComponentId));
				endpoint.getDef().getTags().add(new Tag(jsonApi.id()));
			}
			prevBinding.apiBuilder().removeEndpointAddedListener(prevBinding.endpointListener());
			final var newBinding = prevBinding.with(t -> t.getDef().getTags().add(new Tag(jsonApi.id())));
			prevBinding.apiBuilder().addEndpointAddedListener(newBinding.endpointListener());

			this.jsonApis.put(jsonApi.id(), newBinding);
			break;
		}
		this.log.info("Updated Component Api " + jsonApi.id());
	}

	/**
	 * Unbinds a {@link ComponentJsonApi}.
	 * 
	 * @param jsonApi the {@link ComponentJsonApi} to remove
	 * @param ref     the updated OSGi component properties
	 */
	public void unbindJsonApi(ComponentJsonApi jsonApi, Map<String, ?> ref) {
		this.jsonApis.remove(jsonApi.id());
		this.log.info("Removed '" + jsonApi.id() + "' from Component Apis.");
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.rpc(ComponentJsonApiRequest.METHOD, endpoint -> {
			endpoint.setDescription("Handles a ComponentJsonApiRequest.");

		}, () -> {
			return this.jsonApis.entrySet().stream() //
					.map(jsonApiEntry -> {
						final var subrequest = new Subrequest(JsonUtils.buildJsonObject() //
								.addProperty("componentId", jsonApiEntry.getKey()) //
								.build());

						subrequest.addRpcBuilderFor(jsonApiEntry.getValue().apiBuilder(), "payload");
						return subrequest;
					}).toList();
		}, call -> {
			final var request = ComponentJsonApiRequest.from(call.getRequest());
			final var component = this.jsonApis.get(request.getComponentId());
			if (component == null) {
				throw new RuntimeException("Component with id '" + request.getComponentId() + "' was not found");
			}
			var mapped = call.mapRequest(request.getPayload());
			component.apiBuilder().handle(mapped);

			call.setResponse(mapped.getResponse());
		});
	}

}