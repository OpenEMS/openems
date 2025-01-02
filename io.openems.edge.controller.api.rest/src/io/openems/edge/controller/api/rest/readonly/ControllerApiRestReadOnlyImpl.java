package io.openems.edge.controller.api.rest.readonly;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.user.UserService;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.rest.AbstractRestApi;
import io.openems.edge.controller.api.rest.JsonRpcRestHandler;
import io.openems.edge.controller.api.rest.RestApi;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.Rest.ReadOnly", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerApiRestReadOnlyImpl extends AbstractRestApi
		implements ControllerApiRestReadOnly, RestApi, Controller, OpenemsComponent {

	@Reference
	private JsonRpcRestHandler.Factory restHandlerFactory;
	private JsonRpcRestHandler restHandler;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private UserService userService;

	public ControllerApiRestReadOnlyImpl() {
		super("REST-Api Read-Only", //
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				RestApi.ChannelId.values(), //
				ControllerApiRestReadOnly.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.restHandler = this.restHandlerFactory.get();

		super.activate(context, config.id(), config.alias(), config.enabled(), config.debugMode(), 0, /* no timeout */
				config.port(), config.connectionlimit());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.restHandlerFactory.unget(this.restHandler);
	}

	@Override
	protected UserService getUserService() {
		return this.userService;
	}

	@Override
	protected ComponentManager getComponentManager() {
		return this.componentManager;
	}

	@Override
	protected JsonRpcRestHandler getRpcRestHandler() {
		return this.restHandler;
	}

	@Override
	protected AccessMode getAccessMode() {
		return AccessMode.READ_ONLY;
	}
}
