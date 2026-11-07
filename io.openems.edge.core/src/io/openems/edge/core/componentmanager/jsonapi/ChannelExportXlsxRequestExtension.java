package io.openems.edge.core.componentmanager.jsonapi;

import org.osgi.service.component.annotations.Component;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.session.Role;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.jsonapi.ComponentJsonApiExtension;
import io.openems.edge.common.jsonapi.EdgeGuards;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.core.componentmanager.ComponentManagerImpl;
import io.openems.edge.core.componentmanager.jsonrpc.ChannelExportXlsxRequest;
import io.openems.edge.core.componentmanager.jsonrpc.ChannelExportXlsxResponse;

@Component
public class ChannelExportXlsxRequestExtension implements ComponentJsonApiExtension<ComponentManagerImpl> {

	@Override
	public Class<? extends ComponentManagerImpl> extendClass() {
		return ComponentManagerImpl.class;
	}

	@Override
	public void buildJsonApiRoutes(ComponentManagerImpl componentManager, JsonApiBuilder builder) {
		builder.handleRequest(ChannelExportXlsxRequest.METHOD, //
				endpoint -> endpoint.setDescription("""
						Handles a ChannelExportXlsxRequest.
						""") //
						.setGuards(EdgeGuards.roleIsAtleast(Role.ADMIN)),
				t -> this.handleChannelExportXlsxRequest(componentManager, //
						ChannelExportXlsxRequest.from(t.getRequest())));
	}

	/**
	 * Handles a {@link ChannelExportXlsxRequest}.
	 *
	 * @param componentManager the {@link ComponentManager}
	 * @param request          the {@link ChannelExportXlsxRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsError.OpenemsNamedException on error
	 */
	protected ChannelExportXlsxResponse handleChannelExportXlsxRequest(//
			ComponentManager componentManager, //
			ChannelExportXlsxRequest request //
	) throws OpenemsError.OpenemsNamedException {
		var component = componentManager.getComponent(request.getComponentId());
		if (component == null) {
			throw OpenemsError.EDGE_NO_COMPONENT_WITH_ID.exception(request.getComponentId());
		}
		return new ChannelExportXlsxResponse(request.getId(), component);
	}

}
