package io.openems.edge.core.componentmanager.jsonapi;

import org.osgi.service.component.annotations.Component;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.common.jsonrpc.type.CreateComponentConfig;
import io.openems.common.jsonrpc.type.DeleteComponentConfig;
import io.openems.common.jsonrpc.type.UpdateComponentConfig;
import io.openems.common.session.Role;
import io.openems.edge.common.jsonapi.ComponentJsonApiExtension;
import io.openems.edge.common.jsonapi.EdgeGuards;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.core.componentmanager.ComponentManagerImpl;

@Component
public class ComponentConfigRequestExtension implements ComponentJsonApiExtension<ComponentManagerImpl> {

	@Override
	public Class<? extends ComponentManagerImpl> extendClass() {
		return ComponentManagerImpl.class;
	}

	@Override
	public void buildJsonApiRoutes(ComponentManagerImpl componentManager, JsonApiBuilder builder) {
		builder.handleRequest(new CreateComponentConfig(), //
				endpoint -> endpoint.setDescription("""
						Handles a CreateComponentConfigRequest.
						""") //
						.setGuards(EdgeGuards.roleIsAtleastFromBackend(Role.INSTALLER), //
								EdgeGuards.roleIsAtleastNotFromBackend(Role.ADMIN)),
				t -> {
					componentManager.handleCreateComponentConfigRequest(t.get(EdgeKeys.USER_KEY), t.getRequest());

					return EmptyObject.INSTANCE;
				});

		builder.handleRequest(new UpdateComponentConfig(), //
				endpoint -> endpoint.setDescription("""
						Handles a UpdateComponentConfigRequest.
						""") //
						.setGuards(EdgeGuards.roleIsAtleast(Role.OWNER)),
				t -> {
					componentManager.handleUpdateComponentConfigRequest(t.get(EdgeKeys.USER_KEY), t.getRequest());

					return EmptyObject.INSTANCE;
				});

		builder.handleRequest(new DeleteComponentConfig(), //
				endpoint -> endpoint.setDescription("""
						Handles a DeleteComponentConfigRequest.
						""") //
						.setGuards(EdgeGuards.roleIsAtleastFromBackend(Role.INSTALLER), //
								EdgeGuards.roleIsAtleastNotFromBackend(Role.ADMIN)),
				t -> {
					componentManager.handleDeleteComponentConfigRequest(t.get(EdgeKeys.USER_KEY), t.getRequest());

					return EmptyObject.INSTANCE;
				});

	}

}
