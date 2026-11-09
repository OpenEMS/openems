package io.openems.edge.core.componentmanager.jsonapi;

import java.util.Map;
import java.util.stream.Stream;

import org.osgi.service.component.annotations.Component;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.jsonapi.ComponentJsonApiExtension;
import io.openems.edge.common.jsonapi.EdgeGuards;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.core.componentmanager.ComponentManagerImpl;
import io.openems.edge.core.componentmanager.jsonrpc.GetAllComponentFactories;
import io.openems.edge.core.componentmanager.jsonrpc.GetPropertiesOfFactory;

@Component
public class ComponentFactoryExtension implements ComponentJsonApiExtension<ComponentManagerImpl> {

	@Override
	public Class<ComponentManagerImpl> extendClass() {
		return ComponentManagerImpl.class;
	}

	@Override
	public void buildJsonApiRoutes(ComponentManagerImpl service, JsonApiBuilder builder) {
		builder.handleRequest(new GetAllComponentFactories(), //
				endpoint -> endpoint.setDescription("""
						Handles a GetAllComponentFactories.
						""") //
						.setGuards(EdgeGuards.roleIsAtleast(Role.ADMIN)), //
				call -> {
					final var edgeConfig = service.getEdgeConfig();

					return new GetAllComponentFactories.Response(edgeConfig.getFactories().entrySet().stream()
							.collect(JsonUtils.toJsonObject(Map.Entry::getKey, i -> i.getValue().toJson())));
				});
		builder.handleRequest(new GetPropertiesOfFactory(), //
				endpoint -> endpoint.setDescription("""
						Handles a GetPropertiesOfFactory.
						""") //
						.setGuards(EdgeGuards.roleIsAtleast(Role.ADMIN)), //
				call -> {
					final var factoryId = call.getRequest().factoryId();

					final var edgeConfig = service.getEdgeConfig();
					final var factory = edgeConfig.getFactories().get(factoryId);

					if (factory == null) {
						throw new OpenemsException("Factory with id " + factoryId + " could not be found.");
					}

					return new GetPropertiesOfFactory.Response(factory.toJson(), Stream.of(factory.getProperties()) //
							.map(EdgeConfig.Factory.Property::toJson) //
							.collect(JsonUtils.toJsonArray()));
				});
	}

}
