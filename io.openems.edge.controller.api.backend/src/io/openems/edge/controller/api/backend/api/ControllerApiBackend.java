package io.openems.edge.controller.api.backend.api;

import java.util.concurrent.CompletableFuture;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.user.User;
import io.openems.edge.controller.api.Controller;

public interface ControllerApiBackend extends Controller, OpenemsComponent, EventHandler {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		API_WORKER_LOG(Doc.of(OpenemsType.STRING) //
				.text("Logs Write-Commands via ApiWorker")), //
		UNABLE_TO_SEND(Doc.of(Level.WARNING)
				// Make sure this is always persisted, as it is required for resending
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		LAST_SUCCESSFUL_RESEND(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_SECONDS)
				// Make sure this is always persisted, as it is required for resending
				.persistencePriority(PersistencePriority.VERY_HIGH) //
				.text("Latest timestamp of successfully resent data")), //
		WRONG_APIKEY_CONFIGURATION(Doc.of(Level.WARNING) //
				.text("FEMS Apikey-Configurations do not match")), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#API_WORKER_LOG}.
	 *
	 * @return the Channel
	 */
	public default StringReadChannel getApiWorkerLogChannel() {
		return this.channel(ChannelId.API_WORKER_LOG);
	}

	/**
	 * Gets the Channel for {@link ChannelId#UNABLE_TO_SEND}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getUnableToSendChannel() {
		return this.channel(ChannelId.UNABLE_TO_SEND);
	}

	/**
	 * Gets the Channel for {@link ChannelId#LAST_SUCCESSFUL_RESEND}.
	 * 
	 * @return the Channel
	 */
	public default LongReadChannel getLastSuccessFulResendChannel() {
		return this.channel(ChannelId.LAST_SUCCESSFUL_RESEND);
	}

	/**
	 * Gets if the edge is currently connected to the backend.
	 * 
	 * @return true if it is connected
	 */
	public boolean isConnected();

	/**
	 * Sends the request to the connected backend.
	 * 
	 * @param user    the user
	 * @param request the request to send
	 * @return the result future
	 * @throws OpenemsNamedException on error
	 */
	public CompletableFuture<? extends JsonrpcResponseSuccess> sendRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException;

}
