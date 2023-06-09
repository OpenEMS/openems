package io.openems.edge.battery.soltaro.cluster.versionb;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.soltaro.cluster.SoltaroCluster;
import io.openems.edge.battery.soltaro.common.enums.State;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.startstop.StartStoppable;

public interface BatterySoltaroClusterVersionB
		extends SoltaroCluster, Battery, ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave, StartStoppable {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// EnumReadChannels
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //

		// IntegerWriteChannels
		RESET(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_WRITE)), //
		EMS_ADDRESS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_WRITE)), //
		EMS_COMMUNICATION_TIMEOUT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE)), //
		SYSTEM_INSULATION_LEVEL_1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.OHM) //
				.accessMode(AccessMode.READ_WRITE)), //
		SYSTEM_INSULATION_LEVEL_2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.OHM) //
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

		// StateChannels
		MASTER_ALARM_COMMUNICATION_ERROR_WITH_SUBMASTER(Doc.of(Level.FAULT) //
				.text("Communication error with submaster")),
		MASTER_ALARM_PCS_EMS_COMMUNICATION_FAILURE(Doc.of(Level.FAULT) //
				.text("PCS/EMS communication failure alarm")),
		MASTER_ALARM_PCS_EMS_CONTROL_FAIL(Doc.of(Level.FAULT) //
				.text("PCS/EMS control fail alarm")),
		MASTER_ALARM_LEVEL_1_INSULATION(Doc.of(Level.WARNING) //
				.text("System insulation alarm level 1")),
		MASTER_ALARM_LEVEL_2_INSULATION(Doc.of(Level.FAULT) //
				.text("System insulation alarm level 2")),

		RACK_1_LEVEL_2_ALARM(Doc.of(Level.FAULT) //
				.text("Rack 1 Level 2 Alarm")),
		RACK_1_PCS_CONTROL_FAULT(Doc.of(Level.FAULT) //
				.text("Rack 1 PCS control fault")),
		RACK_1_COMMUNICATION_WITH_MASTER_ERROR(Doc.of(Level.FAULT) //
				.text("Rack 1 Communication with master error")),
		RACK_1_DEVICE_ERROR(Doc.of(Level.FAULT) //
				.text("Rack 1 Device error")),
		RACK_1_CYCLE_OVER_CURRENT(Doc.of(Level.FAULT) //
				.text("Rack 1 Cycle over current")),
		RACK_1_VOLTAGE_DIFFERENCE(Doc.of(Level.FAULT) //
				.text("Rack 1 Voltage difference")),

		RACK_2_LEVEL_2_ALARM(Doc.of(Level.FAULT) //
				.text("Rack 2 Level 2 Alarm")),
		RACK_2_PCS_CONTROL_FAULT(Doc.of(Level.FAULT) //
				.text("Rack 2 PCS control fault")),
		RACK_2_COMMUNICATION_WITH_MASTER_ERROR(Doc.of(Level.FAULT) //
				.text("Rack 2 Communication with master error")),
		RACK_2_DEVICE_ERROR(Doc.of(Level.FAULT) //
				.text("Rack 2 Device error")),
		RACK_2_CYCLE_OVER_CURRENT(Doc.of(Level.FAULT) //
				.text("Rack 1 Cycle over current")),
		RACK_2_VOLTAGE_DIFFERENCE(Doc.of(Level.FAULT) //
				.text("Rack 1 Voltage difference")),

		RACK_3_LEVEL_2_ALARM(Doc.of(Level.FAULT) //
				.text("Rack 3 Level 2 Alarm")),
		RACK_3_PCS_CONTROL_FAULT(Doc.of(Level.FAULT) //
				.text("Rack 3 PCS control fault")),
		RACK_3_COMMUNICATION_WITH_MASTER_ERROR(Doc.of(Level.FAULT) //
				.text("Rack 3 Communication with master error")),
		RACK_3_DEVICE_ERROR(Doc.of(Level.FAULT) //
				.text("Rack 3 Device error")),
		RACK_3_CYCLE_OVER_CURRENT(Doc.of(Level.FAULT) //
				.text("Rack 1 Cycle over current")),
		RACK_3_VOLTAGE_DIFFERENCE(Doc.of(Level.FAULT) //
				.text("Rack 1 Voltage difference")),

		RACK_4_LEVEL_2_ALARM(Doc.of(Level.FAULT) //
				.text("Rack 4 Level 2 Alarm")),
		RACK_4_PCS_CONTROL_FAULT(Doc.of(Level.FAULT) //
				.text("Rack 4 PCS control fault")),
		RACK_4_COMMUNICATION_WITH_MASTER_ERROR(Doc.of(Level.FAULT) //
				.text("Rack 4 Communication with master error")),
		RACK_4_DEVICE_ERROR(Doc.of(Level.FAULT) //
				.text("Rack 4 Device error")),
		RACK_4_CYCLE_OVER_CURRENT(Doc.of(Level.FAULT) //
				.text("Rack 1 Cycle over current")),
		RACK_4_VOLTAGE_DIFFERENCE(Doc.of(Level.FAULT) //
				.text("Rack 1 Voltage difference")),

		RACK_5_LEVEL_2_ALARM(Doc.of(Level.FAULT) //
				.text("Rack 5 Level 2 Alarm")),
		RACK_5_PCS_CONTROL_FAULT(Doc.of(Level.FAULT) //
				.text("Rack 5 PCS control fault")),
		RACK_5_COMMUNICATION_WITH_MASTER_ERROR(Doc.of(Level.FAULT) //
				.text("Rack 5 Communication with master error")),
		RACK_5_DEVICE_ERROR(Doc.of(Level.FAULT) //
				.text("Rack 5 Device error")),
		RACK_5_CYCLE_OVER_CURRENT(Doc.of(Level.FAULT) //
				.text("Rack 1 Cycle over current")),
		RACK_5_VOLTAGE_DIFFERENCE(Doc.of(Level.FAULT) //
				.text("Rack 1 Voltage difference")),;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}
