package io.openems.edge.controller.api.rest;

import io.openems.common.channel.AccessMode;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.user.UserService;

/**
 * Dummy implementation of RestApi for testing.
 */
public class DummyRestApi extends AbstractRestApi {

	/**
	 * Creates a new DummyRestApi.
	 */
	public DummyRestApi() {
		super("DummyRestApi", new io.openems.edge.common.channel.ChannelId[0]);
	}

	@Override
	protected UserService getUserService() {
		return null;
	}

	@Override
	protected ComponentManager getComponentManager() {
		return null;
	}

	@Override
	protected JsonRpcRestHandler getRpcRestHandler() {
		return null;
	}

	@Override
	protected AccessMode getAccessMode() {
		return null;
	}

	@Override
	protected boolean isDebugModeEnabled() {
		return false;
	}
}