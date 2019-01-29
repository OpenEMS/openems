package io.openems.edge.battery.soltaro.multirack;

import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.Unit;

public enum MultiRackChannelId implements io.openems.edge.common.channel.doc.ChannelId {
	STATE_MACHINE(new Doc().level(Level.INFO).text("Current State of State-Machine").options(State.values())), //
	START_STOP(new Doc().options(Enums.StartStop.values())), //
	RACK_1_USAGE(new Doc().options(Enums.RackUsage.values())), //
	RACK_2_USAGE(new Doc().options(Enums.RackUsage.values())), //
	RACK_3_USAGE(new Doc().options(Enums.RackUsage.values())), //
	RACK_4_USAGE(new Doc().options(Enums.RackUsage.values())), //
	RACK_5_USAGE(new Doc().options(Enums.RackUsage.values())), //
	
	SYSTEM_INSULATION_LEVEL_1(new Doc().unit(Unit.OHM)), //
	SYSTEM_INSULATION_LEVEL_2(new Doc().unit(Unit.OHM)), //
	EMS_COMMUNICATION_TIMEOUT(new Doc().unit(Unit.SECONDS)), //
	EMS_ADDRESS(new Doc().unit(Unit.NONE)), //

	RACK_1_POSITIVE_CONTACTOR(new Doc().unit(Unit.NONE)), //
	RACK_2_POSITIVE_CONTACTOR(new Doc().unit(Unit.NONE)), //
	RACK_3_POSITIVE_CONTACTOR(new Doc().unit(Unit.NONE)), //
	RACK_4_POSITIVE_CONTACTOR(new Doc().unit(Unit.NONE)), //
	RACK_5_POSITIVE_CONTACTOR(new Doc().unit(Unit.NONE)), //
	
	CHARGE_INDICATION(new Doc().options(Enums.ChargeIndication.values())), //
	CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
	SYSTEM_RUNNING_STATE(new Doc().options(Enums.RunningState.values())), //
	VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	SYSTEM_INSULATION(new Doc().unit(Unit.OHM)), //
	
	MASTER_ALARM_COMMUNICATION_ERROR_WITH_SUBMASTER(new Doc().level(Level.FAULT).text("Communication error with submaster")),
	MASTER_ALARM_PCS_EMS_COMMUNICATION_FAILURE(new Doc().level(Level.FAULT).text("PCS/EMS communication failure alarm")),
	MASTER_ALARM_PCS_EMS_CONTROL_FAIL(new Doc().level(Level.FAULT).text("PCS/EMS control fail alarm")),
	MASTER_ALARM_LEVEL_1_INSULATION(new Doc().level(Level.WARNING).text("System insulation alarm level 1")),
	MASTER_ALARM_LEVEL_2_INSULATION(new Doc().level(Level.FAULT).text("System insulation alarm level 2")),
	
	
	SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_1(new Doc().level(Level.FAULT).text("Communication to sub master 1 fault")),
	SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_2(new Doc().level(Level.FAULT).text("Communication to sub master 2 fault")),
	SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_3(new Doc().level(Level.FAULT).text("Communication to sub master 3 fault")),
	SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_4(new Doc().level(Level.FAULT).text("Communication to sub master 4 fault")),
	SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_5(new Doc().level(Level.FAULT).text("Communication to sub master 5 fault")),
	
	RACK_1_LEVEL_2_ALARM(new Doc().level(Level.FAULT).text("Rack 1 Level 2 Alarm")), 
	RACK_1_PCS_CONTROL_FAULT(new Doc().level(Level.FAULT).text("Rack 1 PCS control fault")),
	RACK_1_COMMUNICATION_WITH_MASTER_ERROR(new Doc().level(Level.FAULT).text("Rack 1 Communication with master error")),
	RACK_1_DEVICE_ERROR(new Doc().level(Level.FAULT).text("Rack 1 Device error")),
	RACK_1_CYCLE_OVER_CURRENT(new Doc().level(Level.FAULT).text("Rack 1 Cycle over current")),
	RACK_1_VOLTAGE_DIFFERENCE(new Doc().level(Level.FAULT).text("Rack 1 Voltage difference")),
	
	RACK_2_LEVEL_2_ALARM(new Doc().level(Level.FAULT).text("Rack 2 Level 2 Alarm")), 
	RACK_2_PCS_CONTROL_FAULT(new Doc().level(Level.FAULT).text("Rack 2 PCS control fault")),
	RACK_2_COMMUNICATION_WITH_MASTER_ERROR(new Doc().level(Level.FAULT).text("Rack 2 Communication with master error")),
	RACK_2_DEVICE_ERROR(new Doc().level(Level.FAULT).text("Rack 2 Device error")),
	RACK_2_CYCLE_OVER_CURRENT(new Doc().level(Level.FAULT).text("Rack 2 Cycle over current")),
	RACK_2_VOLTAGE_DIFFERENCE(new Doc().level(Level.FAULT).text("Rack 2 Voltage difference")),
	
	RACK_3_LEVEL_2_ALARM(new Doc().level(Level.FAULT).text("Rack 3 Level 2 Alarm")), 
	RACK_3_PCS_CONTROL_FAULT(new Doc().level(Level.FAULT).text("Rack 3 PCS control fault")),
	RACK_3_COMMUNICATION_WITH_MASTER_ERROR(new Doc().level(Level.FAULT).text("Rack 3 Communication with master error")),
	RACK_3_DEVICE_ERROR(new Doc().level(Level.FAULT).text("Rack 3 Device error")),
	RACK_3_CYCLE_OVER_CURRENT(new Doc().level(Level.FAULT).text("Rack 3 Cycle over current")),
	RACK_3_VOLTAGE_DIFFERENCE(new Doc().level(Level.FAULT).text("Rack 3 Voltage difference")),
	
	RACK_4_LEVEL_2_ALARM(new Doc().level(Level.FAULT).text("Rack 4 Level 2 Alarm")), 
	RACK_4_PCS_CONTROL_FAULT(new Doc().level(Level.FAULT).text("Rack 4 PCS control fault")),
	RACK_4_COMMUNICATION_WITH_MASTER_ERROR(new Doc().level(Level.FAULT).text("Rack 4 Communication with master error")),
	RACK_4_DEVICE_ERROR(new Doc().level(Level.FAULT).text("Rack 4 Device error")),
	RACK_4_CYCLE_OVER_CURRENT(new Doc().level(Level.FAULT).text("Rack 4 Cycle over current")),
	RACK_4_VOLTAGE_DIFFERENCE(new Doc().level(Level.FAULT).text("Rack 4 Voltage difference")),
	
	RACK_5_LEVEL_2_ALARM(new Doc().level(Level.FAULT).text("Rack 5 Level 2 Alarm")), 
	RACK_5_PCS_CONTROL_FAULT(new Doc().level(Level.FAULT).text("Rack 5 PCS control fault")),
	RACK_5_COMMUNICATION_WITH_MASTER_ERROR(new Doc().level(Level.FAULT).text("Rack 5 Communication with master error")),
	RACK_5_DEVICE_ERROR(new Doc().level(Level.FAULT).text("Rack 5 Device error")),
	RACK_5_CYCLE_OVER_CURRENT(new Doc().level(Level.FAULT).text("Rack 5 Cycle over current")),
	RACK_5_VOLTAGE_DIFFERENCE(new Doc().level(Level.FAULT).text("Rack 5 Voltage difference")),
	;
	private final Doc doc;

	private MultiRackChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}

