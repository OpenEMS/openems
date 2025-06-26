package io.openems.edge.controller.api.common.handler;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.jsonrpc.request.ComponentJsonApiRequest;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.DeleteComponentConfigRequest;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.request.SetChannelValueRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.session.Role;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.jsonapi.EdgeGuards;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.jsonapi.Key;
import io.openems.edge.controller.api.common.ApiWorker;

@Component(service = { ComponentConfigRequestHandler.class, JsonApi.class }, scope = ServiceScope.SINGLETON)
public class ComponentConfigRequestHandler implements JsonApi {

	public static final Key<ApiWorker> API_WORKER_KEY = new Key<>("apiWorker", ApiWorker.class);

	private final ComponentManager componentManager;

	@Activate
	public ComponentConfigRequestHandler(@Reference ComponentManager componentManager) {
		this.componentManager = componentManager;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.delegate(GetEdgeConfigRequest.METHOD, endpoint -> {
			endpoint.setDescription("""
					Handles a GetEdgeConfigRequest.
					Delegates original request to a ComponentJsonApiRequest.
					""");
		}, call -> {
			return new ComponentJsonApiRequest(ComponentManager.SINGLETON_COMPONENT_ID,
					GetEdgeConfigRequest.from(call.getRequest()));
		});

		builder.delegate(CreateComponentConfigRequest.METHOD, endpoint -> {
			endpoint.setDescription("""
					Handles a CreateComponentConfigRequest.
					Delegates original request to a ComponentJsonApiRequest.
					""");
		}, call -> {
			return new ComponentJsonApiRequest(ComponentManager.SINGLETON_COMPONENT_ID,
					CreateComponentConfigRequest.from(call.getRequest()));
		});

		builder.delegate(UpdateComponentConfigRequest.METHOD, endpoint -> {
			endpoint.setDescription("""
					Handles a UpdateComponentConfigRequest.
					Delegates original request to a ComponentJsonApiRequest.
					""");
		}, call -> {
			return new ComponentJsonApiRequest(ComponentManager.SINGLETON_COMPONENT_ID,
					UpdateComponentConfigRequest.from(call.getRequest()));
		});

		builder.delegate(DeleteComponentConfigRequest.METHOD, endpoint -> {
			endpoint.setDescription("""
					Handles a DeleteComponentConfigRequest.
					Delegates original request to a ComponentJsonApiRequest.
					""");
		}, call -> {
			return new ComponentJsonApiRequest(ComponentManager.SINGLETON_COMPONENT_ID,
					DeleteComponentConfigRequest.from(call.getRequest()));
		});

		builder.handleRequest(SetChannelValueRequest.METHOD, endpoint -> {
			endpoint.setDescription("Handles a SetChannelValueRequest") //
					.setGuards(EdgeGuards.roleIsAtleast(Role.ADMIN));
		}, call -> {
			final var apiWorker = call.get(API_WORKER_KEY);
			return apiWorker.handleSetChannelValueRequest(this.componentManager, call.get(EdgeKeys.USER_KEY),
					SetChannelValueRequest.from(call.getRequest())).get();
		});
	}

}