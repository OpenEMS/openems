package io.openems.edge.controller.api.rest.readwrite;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.user.UserService;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.rest.AbstractRestApi;
import io.openems.edge.controller.api.rest.RestApi;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.Rest.ReadWrite", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class RestApiReadWriteImpl extends AbstractRestApi
		implements RestApiReadWrite, RestApi, Controller, OpenemsComponent {

	@Reference
	private ComponentManager componentManager;

	@Reference
	private UserService userService;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	public RestApiReadWriteImpl() {
		super("REST-Api Read-Write", //
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				RestApi.ChannelId.values(), //
				RestApiReadWrite.ChannelId.values() //
		);
		this.apiWorker.setLogChannel(this.getApiWorkerLogChannel());
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.debugMode(), config.apiTimeout(),
				config.port(), config.connectionlimit());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected Timedata getTimedata() throws OpenemsException {
		if (this.timedata != null) {
			return this.timedata;
		}
		throw new OpenemsException("There is no Timedata-Service available!");
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
	protected AccessMode getAccessMode() {
		return AccessMode.READ_WRITE;
	}
}
