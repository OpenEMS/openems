package io.openems.edge.phoenixcontact.plcnext.loadcircuit;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;

public interface PlcNextLoadCircuit extends ModbusComponent, OpenemsComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		MAX_ACTIVE_POWER_IMPORT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)),
		MAX_ACTIVE_POWER_EXPORT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)),
		MAX_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT_AMPERE_REACTIVE)//
				.persistencePriority(PersistencePriority.HIGH));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public default IntegerReadChannel getMaxActivePowerExportChannel() {
		return this.channel(ChannelId.MAX_ACTIVE_POWER_EXPORT);
	}

	public default Value<Integer> getMaxActivePowerExport() {
		return this.getMaxActivePowerExportChannel().value();
	}

	public default IntegerReadChannel getMaxActivePowerImportChannel() {
		return this.channel(ChannelId.MAX_ACTIVE_POWER_IMPORT);
	}

	public default Value<Integer> getMaxActivePowerImport() {
		return this.getMaxActivePowerImportChannel().value();
	}

	public default IntegerReadChannel getMaxReactivePowerChannel() {
		return this.channel(ChannelId.MAX_REACTIVE_POWER);
	}

	public default Value<Integer> getMaxReactivePower() {
		return this.getMaxReactivePowerChannel().value();
	}

}
