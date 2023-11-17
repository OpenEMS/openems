package io.openems.edge.common.host;

import java.util.concurrent.CompletableFuture;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.common.user.User;

/**
 * Simulates a {@link Host} for the OpenEMS Component test framework.
 */
public class DummyHost extends AbstractDummyOpenemsComponent<DummyHost> implements Host {

	public DummyHost() {
		super("_host", //
				OpenemsComponent.ChannelId.values(), //
				Host.ChannelId.values());
	}

	@Override
	protected DummyHost self() {
		return this;
	}

	/**
	 * Set {@link Host.ChannelId#HOSTNAME}.
	 *
	 * @param value the value
	 * @return myself
	 */
	public DummyHost withHostname(String value) {
		TestUtils.withValue(this, Host.ChannelId.HOSTNAME, value);
		return this;
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		return null;
	}

}