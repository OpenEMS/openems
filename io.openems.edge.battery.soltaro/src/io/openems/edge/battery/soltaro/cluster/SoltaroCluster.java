package io.openems.edge.battery.soltaro.cluster;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.soltaro.cluster.enums.ContactorControl;
import io.openems.edge.battery.soltaro.cluster.enums.RackUsage;
import io.openems.edge.battery.soltaro.cluster.enums.RunningState;
import io.openems.edge.battery.soltaro.cluster.enums.StartStop;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;

public interface SoltaroCluster extends Battery, OpenemsComponent, EventHandler, ModbusSlave {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/*
		 * EnumWriteChannels
		 */
		START_STOP(Doc.of(StartStop.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		RACK_1_USAGE(Doc.of(RackUsage.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		RACK_2_USAGE(Doc.of(RackUsage.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		RACK_3_USAGE(Doc.of(RackUsage.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		RACK_4_USAGE(Doc.of(RackUsage.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		RACK_5_USAGE(Doc.of(RackUsage.values()) //
				.accessMode(AccessMode.READ_WRITE)), //

		RACK_1_POSITIVE_CONTACTOR(Doc.of(ContactorControl.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		RACK_2_POSITIVE_CONTACTOR(Doc.of(ContactorControl.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		RACK_3_POSITIVE_CONTACTOR(Doc.of(ContactorControl.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		RACK_4_POSITIVE_CONTACTOR(Doc.of(ContactorControl.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		RACK_5_POSITIVE_CONTACTOR(Doc.of(ContactorControl.values()) //
				.accessMode(AccessMode.READ_WRITE)), //

		/*
		 * EnumReadChannels
		 */
		SYSTEM_RUNNING_STATE(Doc.of(RunningState.values())), //

		/*
		 * IntegerReadChannels
		 */
		SYSTEM_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		SYSTEM_INSULATION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.OHM)), //

		/*
		 * StateChannels
		 */
		// Sub Master Communication Failure Registers
		SUB_MASTER_1_COMMUNICATION_FAILURE(Doc.of(Level.WARNING) //
				.text("Communication Failure to Sub Master 1")),
		SUB_MASTER_2_COMMUNICATION_FAILURE(Doc.of(Level.WARNING) //
				.text("Communication Failure to Sub Master 2")),
		SUB_MASTER_3_COMMUNICATION_FAILURE(Doc.of(Level.WARNING) //
				.text("Communication Failure to Sub Master 3")),
		SUB_MASTER_4_COMMUNICATION_FAILURE(Doc.of(Level.WARNING) //
				.text("Communication Failure to Sub Master 4")),
		SUB_MASTER_5_COMMUNICATION_FAILURE(Doc.of(Level.WARNING) //
				.text("Communication Failure to Sub Master 5")),

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
	 * Adds a Channel. Called by SingleRack.
	 * 
	 * @param channelId the ChannelId.
	 * @return the Channel
	 */
	public Channel<?> _addChannel(io.openems.edge.common.channel.ChannelId channelId);

	/**
	 * Adds a Channel-Mapping. Called by SingleRack.
	 * 
	 * @param channelId the ChannelId.
	 * @param element   the Modbus-Element
	 * @return the Modbus-Element
	 */
	public AbstractModbusElement<?> _map(io.openems.edge.common.channel.ChannelId channelId,
			AbstractModbusElement<?> element);

	/**
	 * Adds a Channel-Mapping. Called by SingleRack.
	 * 
	 * @param channelId the ChannelId.
	 * @param element   the Modbus-Element
	 * @return the Modbus-Element
	 */
	public AbstractModbusElement<?> _map(io.openems.edge.common.channel.ChannelId channelId,
			AbstractModbusElement<?> element, ElementToChannelConverter converter);

	/**
	 * Adds a Channel-Mapping. Called by SingleRack.
	 * 
	 * @param channelId the ChannelId.
	 * @param element   the Modbus-Element
	 * @return the Modbus-Element
	 */
	public AbstractModbusElement<?> _map(BitsWordElement bitsWordElement);

}