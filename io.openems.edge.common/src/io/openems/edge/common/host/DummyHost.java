package io.openems.edge.common.host;

import java.util.concurrent.CompletableFuture;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.user.User;

/**
 * Simulates a {@link Host} for the OpenEMS Component test framework.
 */
public class DummyHost extends AbstractOpenemsComponent implements Host {

	public DummyHost() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Host.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, "_host", "", true);

	}

	/**
	 * Sets and applies the {@link Host.ChannelId#HOSTNAME}.
	 *
	 * @param hostname the Hostname
	 * @return myself
	 */
	public DummyHost withHostname(String hostname) {
		this._setHostname(hostname);
		this.getHostnameChannel().nextProcessImage();
		return this;
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		return null;
	}

}