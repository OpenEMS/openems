package io.openems.edge.controller.api.modbus.readwrite.rtu;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface ControllerApiModbusRtuReadWrite extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		API_WORKER_LOG(Doc.of(OpenemsType.STRING) //
				.text("Logs Write-Commands via ApiWorker")), //
		DEBUG_SET_ACTIVE_POWER_EQUALS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		DEBUG_SET_ACTIVE_POWER_GREATER_OR_EQUALS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		DEBUG_SET_REACTIVE_POWER_EQUALS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		DEBUG_SET_REACTIVE_POWER_GREATER_OR_EQUALS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //
		DEBUG_SET_REACTIVE_POWER_LESS_OR_EQUALS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE)), //

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

}