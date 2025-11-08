package io.openems.edge.system.fenecon.industrial.jsonrpc;

import static io.openems.common.session.Role.ADMIN;

import java.util.List;

import io.openems.common.jsonrpc.serialization.EmptyObject;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.jsonapi.EdgeGuards;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.ess.api.EssErrorAcknowledgeRequest;

public interface ErrorAcknowledgeRpc {

	/**
	 * Sends a JSON-RPC request to reset given channel values. For instance, this
	 * method can be used to reset error channels.
	 *
	 * @param builder the {@link JsonApiBuilder}.
	 */
	default void executeErrorAcknowledgeRpc(JsonApiBuilder builder) {
		this.executeErrorAcknowledgeRpc(builder, null);
	}

	/**
	 * Sends a JSON-RPC request to reset given channel values, and optionally
	 * executes a callback afterwards.
	 *
	 * @param builder  the {@link JsonApiBuilder}.
	 * @param callback optional callback to execute after resetting channels.
	 */
	default void executeErrorAcknowledgeRpc(JsonApiBuilder builder, Runnable callback) {
		builder.handleRequest(new EssErrorAcknowledgeRequest(), endpoint -> {
			endpoint.setGuards(EdgeGuards.roleIsAtleast(ADMIN));
		}, call -> {
			var channels = this.defineChannelsForErrorAcknowledge();
			channels.forEach(c -> c.setNextValue(null));

			if (callback != null) {
				callback.run();
			}

			return EmptyObject.INSTANCE;
		});
	}

	/**
	 * Defines channels of this component that should be reset on error acknowledge.
	 *
	 * @return the list of channels to reset.
	 */
	List<Channel<?>> defineChannelsForErrorAcknowledge();
}
