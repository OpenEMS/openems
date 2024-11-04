package io.openems.edge.controller.api.rest.readwrite;

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
		name = "Controller.Api.Rest.ReadWrite", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerApiRestReadWriteImpl extends AbstractRestApi
		implements ControllerApiRestReadWrite, RestApi, Controller, OpenemsComponent {

	@Reference
	private JsonRpcRestHandler.Factory restHandlerFactory;
	private JsonRpcRestHandler restHandler;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private UserService userService;

	public ControllerApiRestReadWriteImpl() {
		super("REST-Api Read-Write", //
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				RestApi.ChannelId.values(), //
				ControllerApiRestReadWrite.ChannelId.values() //
		);
		this.apiWorker.setLogChannel(this.getApiWorkerLogChannel());
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.restHandler = this.restHandlerFactory.get();

		super.activate(context, config.id(), config.alias(), config.enabled(), config.debugMode(), config.apiTimeout(),
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
		return AccessMode.READ_WRITE;
	}
}
