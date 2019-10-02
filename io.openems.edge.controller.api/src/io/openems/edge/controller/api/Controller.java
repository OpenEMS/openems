package io.openems.edge.controller.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;

@ProviderType
public interface Controller extends OpenemsComponent {

	/**
	 * Executes the Controller logic.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	public void run() throws OpenemsNamedException;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		RUN_FAILED(Doc.of(Level.FAULT).text("Running the Controller failed"));

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
	 * Gets the "RunFailed" State-Channel.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getRunFailed() {
		return this.channel(ChannelId.RUN_FAILED);
	}

	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(OpenemsComponent.class, accessMode, 80) //
				.channel(0, ChannelId.RUN_FAILED, ModbusType.UINT16) //
				.build();
	}
}
