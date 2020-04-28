package io.openems.edge.battery.soltaro.cluster.versionc;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.soltaro.State;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;

public interface ClusterVersionC extends Battery, OpenemsComponent, EventHandler, ModbusSlave {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// EnumReadChannels
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
//		CHARGE_INDICATION(Doc.of(ChargeIndication.values())), //
//		SYSTEM_RUNNING_STATE(Doc.of(Enums.RunningState.values())), //

		// IntegerWriteChannels
//		RESET(Doc.of(OpenemsType.INTEGER) //
//				.unit(Unit.NONE) //
//				.accessMode(AccessMode.READ_WRITE)), //
//		RACK_1_POSITIVE_CONTACTOR(Doc.of(Enums.ContactorControl.values()) //
//				.accessMode(AccessMode.READ_WRITE)), //
//		RACK_2_POSITIVE_CONTACTOR(Doc.of(Enums.ContactorControl.values()) //
//				.accessMode(AccessMode.READ_WRITE)), //
//		RACK_3_POSITIVE_CONTACTOR(Doc.of(Enums.ContactorControl.values()) //
//				.accessMode(AccessMode.READ_WRITE)), //
//		RACK_4_POSITIVE_CONTACTOR(Doc.of(Enums.ContactorControl.values()) //
//				.accessMode(AccessMode.READ_WRITE)), //
//		RACK_5_POSITIVE_CONTACTOR(Doc.of(Enums.ContactorControl.values()) //
//				.accessMode(AccessMode.READ_WRITE)), //
//		SYSTEM_INSULATION_LEVEL_1(Doc.of(OpenemsType.INTEGER) //
//				.unit(Unit.OHM) //
//				.accessMode(AccessMode.READ_WRITE)), //
//		SYSTEM_INSULATION_LEVEL_2(Doc.of(OpenemsType.INTEGER) //
//				.unit(Unit.OHM) //
//				.accessMode(AccessMode.READ_WRITE)), //

		// IntegerReadChannels
		ORIGINAL_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)),
//		SYSTEM_CURRENT(Doc.of(OpenemsType.INTEGER) //
//				.unit(Unit.MILLIAMPERE)), //
//		SYSTEM_INSULATION(Doc.of(OpenemsType.INTEGER) //
//				.unit(Unit.OHM)), //

		/*
		 * StateChannels
		 */
		// Master BMS Alarm Registers
		MASTER_EMS_COMMUNICATION_FAILURE(Doc.of(Level.FAULT) //
				.text("Master EMS Communication Failure")),
		MASTER_PCS_CONTROL_FAILURE(Doc.of(Level.FAULT) //
				.text("Master PCS Control Failure")),
		MASTER_PCS_COMMUNICATION_FAILURE(Doc.of(Level.FAULT) //
				.text("Master PCS Communication Failure")),
		// Rack #1 cannot be paralleled to DC Bus reasons
		RACK_1_LEVEL_2_ALARM(Doc.of(Level.FAULT) //
				.text("Rack 1 Level 2 Alarm")),
		RACK_1_PCS_CONTROL_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 1 PCS Control Failure")),
		RACK_1_COMMUNICATION_TO_MASTER_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 1 Communication to Master BMS Failure")),
		RACK_1_HARDWARE_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 1 Hardware Failure")),
		RACK_1_OVER_CURRENT(Doc.of(Level.FAULT) //
				.text("Rack 1 Too big circulating Current among clusters (>4A)")),
		RACK_1_VOLTAGE_DIFFERENCE(Doc.of(Level.FAULT) //
				.text("Rack 1 Too big boltage difference among clusters (>50V)")),
		// Rack #2 cannot be paralleled to DC Bus reasons
		RACK_2_LEVEL_2_ALARM(Doc.of(Level.FAULT) //
				.text("Rack 2 Level 2 Alarm")),
		RACK_2_PCS_CONTROL_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 2 PCS Control Failure")),
		RACK_2_COMMUNICATION_TO_MASTER_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 2 Communication to Master BMS Failure")),
		RACK_2_HARDWARE_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 2 Hardware Failure")),
		RACK_2_OVER_CURRENT(Doc.of(Level.FAULT) //
				.text("Rack 2 Too big circulating Current among clusters (>4A)")),
		RACK_2_VOLTAGE_DIFFERENCE(Doc.of(Level.FAULT) //
				.text("Rack 2 Too big boltage difference among clusters (>50V)")),
		// Rack #3 cannot be paralleled to DC Bus reasons
		RACK_3_LEVEL_2_ALARM(Doc.of(Level.FAULT) //
				.text("Rack 3 Level 2 Alarm")),
		RACK_3_PCS_CONTROL_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 3 PCS Control Failure")),
		RACK_3_COMMUNICATION_TO_MASTER_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 3 Communication to Master BMS Failure")),
		RACK_3_HARDWARE_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 3 Hardware Failure")),
		RACK_3_OVER_CURRENT(Doc.of(Level.FAULT) //
				.text("Rack 3 Too big circulating Current among clusters (>4A)")),
		RACK_3_VOLTAGE_DIFFERENCE(Doc.of(Level.FAULT) //
				.text("Rack 3 Too big boltage difference among clusters (>50V)")),
		// Rack #4 cannot be paralleled to DC Bus reasons
		RACK_4_LEVEL_2_ALARM(Doc.of(Level.FAULT) //
				.text("Rack 4 Level 2 Alarm")),
		RACK_4_PCS_CONTROL_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 4 PCS Control Failure")),
		RACK_4_COMMUNICATION_TO_MASTER_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 4 Communication to Master BMS Failure")),
		RACK_4_HARDWARE_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 4 Hardware Failure")),
		RACK_4_OVER_CURRENT(Doc.of(Level.FAULT) //
				.text("Rack 4 Too big circulating Current among clusters (>4A)")),
		RACK_4_VOLTAGE_DIFFERENCE(Doc.of(Level.FAULT) //
				.text("Rack 4 Too big boltage difference among clusters (>50V)")),
		// Rack #5 cannot be paralleled to DC Bus reasons
		RACK_5_LEVEL_2_ALARM(Doc.of(Level.FAULT) //
				.text("Rack 5 Level 2 Alarm")),
		RACK_5_PCS_CONTROL_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 5 PCS Control Failure")),
		RACK_5_COMMUNICATION_TO_MASTER_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 5 Communication to Master BMS Failure")),
		RACK_5_HARDWARE_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 5 Hardware Failure")),
		RACK_5_OVER_CURRENT(Doc.of(Level.FAULT) //
				.text("Rack 5 Too big circulating Current among clusters (>4A)")),
		RACK_5_VOLTAGE_DIFFERENCE(Doc.of(Level.FAULT) //
				.text("Rack 5 Too big boltage difference among clusters (>50V)")),
//		MASTER_ALARM_COMMUNICATION_ERROR_WITH_SUBMASTER(Doc.of(Level.FAULT) //
//				.text("Communication error with submaster")),
//		MASTER_ALARM_PCS_EMS_COMMUNICATION_FAILURE(Doc.of(Level.FAULT) //
//				.text("PCS/EMS communication failure alarm")),
//		MASTER_ALARM_PCS_EMS_CONTROL_FAIL(Doc.of(Level.FAULT) //
//				.text("PCS/EMS control fail alarm")),
//		MASTER_ALARM_LEVEL_1_INSULATION(Doc.of(Level.WARNING) //
//				.text("System insulation alarm level 1")),
//		MASTER_ALARM_LEVEL_2_INSULATION(Doc.of(Level.FAULT) //
//				.text("System insulation alarm level 2")),
//		SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_1(Doc.of(Level.OK) //
//				.text("Communication to sub master 1 fault")),
//		SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_2(Doc.of(Level.OK) //
//				.text("Communication to sub master 2 fault")),
//		SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_3(Doc.of(Level.OK) //
//				.text("Communication to sub master 3 fault")),
//		SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_4(Doc.of(Level.OK) //
//				.text("Communication to sub master 4 fault")),

		//
//		RACK_2_LEVEL_2_ALARM(Doc.of(Level.FAULT) //
//				.text("Rack 2 Level 2 Alarm")),
//		RACK_2_PCS_CONTROL_FAULT(Doc.of(Level.FAULT) //
//				.text("Rack 2 PCS control fault")),
//		RACK_2_COMMUNICATION_WITH_MASTER_ERROR(Doc.of(Level.FAULT) //
//				.text("Rack 2 Communication with master error")),
//		RACK_2_DEVICE_ERROR(Doc.of(Level.FAULT) //
//				.text("Rack 2 Device error")),
//		RACK_2_CYCLE_OVER_CURRENT(Doc.of(Level.FAULT) //
//				.text("Rack 1 Cycle over current")),
//		RACK_2_VOLTAGE_DIFFERENCE(Doc.of(Level.FAULT) //
//				.text("Rack 1 Voltage difference")),
		//
//		RACK_3_LEVEL_2_ALARM(Doc.of(Level.FAULT) //
//				.text("Rack 3 Level 2 Alarm")),
//		RACK_3_PCS_CONTROL_FAULT(Doc.of(Level.FAULT) //
//				.text("Rack 3 PCS control fault")),
//		RACK_3_COMMUNICATION_WITH_MASTER_ERROR(Doc.of(Level.FAULT) //
//				.text("Rack 3 Communication with master error")),
//		RACK_3_DEVICE_ERROR(Doc.of(Level.FAULT) //
//				.text("Rack 3 Device error")),
//		RACK_3_CYCLE_OVER_CURRENT(Doc.of(Level.FAULT) //
//				.text("Rack 1 Cycle over current")),
//		RACK_3_VOLTAGE_DIFFERENCE(Doc.of(Level.FAULT) //
//				.text("Rack 1 Voltage difference")),
		//
//		RACK_4_LEVEL_2_ALARM(Doc.of(Level.FAULT) //
//				.text("Rack 4 Level 2 Alarm")),
//		RACK_4_PCS_CONTROL_FAULT(Doc.of(Level.FAULT) //
//				.text("Rack 4 PCS control fault")),
//		RACK_4_COMMUNICATION_WITH_MASTER_ERROR(Doc.of(Level.FAULT) //
//				.text("Rack 4 Communication with master error")),
//		RACK_4_DEVICE_ERROR(Doc.of(Level.FAULT) //
//				.text("Rack 4 Device error")),
//		RACK_4_CYCLE_OVER_CURRENT(Doc.of(Level.FAULT) //
//				.text("Rack 1 Cycle over current")),
//		RACK_4_VOLTAGE_DIFFERENCE(Doc.of(Level.FAULT) //
//				.text("Rack 1 Voltage difference")),
		//
//		RACK_5_LEVEL_2_ALARM(Doc.of(Level.FAULT) //
//				.text("Rack 5 Level 2 Alarm")),
//		RACK_5_PCS_CONTROL_FAULT(Doc.of(Level.FAULT) //
//				.text("Rack 5 PCS control fault")),
//		RACK_5_COMMUNICATION_WITH_MASTER_ERROR(Doc.of(Level.FAULT) //
//				.text("Rack 5 Communication with master error")),
//		RACK_5_DEVICE_ERROR(Doc.of(Level.FAULT) //
//				.text("Rack 5 Device error")),
//		RACK_5_CYCLE_OVER_CURRENT(Doc.of(Level.FAULT) //
//				.text("Rack 1 Cycle over current")),
//		RACK_5_VOLTAGE_DIFFERENCE(Doc.of(Level.FAULT) //
//				.text("Rack 1 Voltage difference"))
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

}
