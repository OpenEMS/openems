package io.openems.edge.controller.api.modbus.readwrite;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.Unit.CUMULATED_SECONDS;
import static io.openems.common.types.OpenemsType.LONG;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.common.Status;

public interface ControllerApiModbusTcpReadWrite extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		
		OVERRIDE_STATUS(Doc.of(Status.values()) //
				.persistencePriority(PersistencePriority.HIGH)), //
		
		CUMULATED_ACTIVE_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS) //
				.persistencePriority(HIGH)), //
		
		CUMULATED_INACTIVE_TIME(Doc.of(LONG)//
				.unit(CUMULATED_SECONDS) //
				.persistencePriority(HIGH)), //
		
		API_WORKER_LOG(Doc.of(OpenemsType.STRING) //
				.text("Logs Write-Commands via ApiWorker")); //

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
	 * Gets the Channel for {@link ChannelId#OVERRIDE_STATUS}.
	 *
	 * @return the Channel
	 */
	public default Channel<Status> getOverrideStatusChannel() {
		return this.channel(ChannelId.OVERRIDE_STATUS);
	}

	/**
	 * Gets the Status. See {@link ChannelId#OVERRIDE_STATUS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Status getOverrideStatus() {
		return this.getOverrideStatusChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#OVERRIDE_STATUS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setOverrideStatus(Status value) {
		this.getOverrideStatusChannel().setNextValue(value);
	}

}
