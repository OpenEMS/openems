package io.openems.edge.ess.mr.gridcon.enums;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.ess.mr.gridcon.enums.ErrorDoc.Acknowledge;
import io.openems.edge.ess.mr.gridcon.enums.ErrorDoc.ReactionLevel;

/**
 * This enum holds every possible error channel id for a gridcon.
 */
public enum ErrorCodeChannelId1 implements ChannelId {
	STATE_MERKUR_PORT0_DSC_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.NO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x060000) //
			.text("DSC 10 removed !Restart!  ")),
	STATE_MERKUR_PORT1_DSC_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.NO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x060001) //
			.text("DSC 9 removed !Restart!  ")),
	STATE_MERKUR_PORT2_DSC_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.NO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x060002) //
			.text("DSC 8 removed !Restart!  ")),
	STATE_MERKUR_PORT3_DSC_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.NO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x060003) //
			.text("DSC 7 removed !Restart!  ")),
	STATE_MERKUR_PORT4_DSC_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.NO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x060004) //
			.text("DSC 6 removed !Restart!  ")),
	STATE_MERKUR_PORT5_DSC_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.NO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x060005) //
			.text("DSC 5 removed !Restart!  ")),
	STATE_MERKUR_PORT6_DSC_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.NO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x060006) //
			.text("DSC 4 removed !Restart!  ")),
	STATE_MERKUR_PORT7_DSC_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.NO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x060007) //
			.text("DSC 3 removed !Restart!  ")),
	STATE_MERKUR_PORT8_DSC_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.NO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x060008) //
			.text("DSC 2 removed !Restart!  ")),
	STATE_MERKUR_PORT9_DSC_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.NO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x060009) //
			.text("DSC 1 removed !Restart! ")),
	STATE_INPUT_SLOT_Blackfin_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(true)
			// after this error it was even not possible to acknowledge it with MR-Tool, so
			// a hard reset has been necessary
			.code(0x06000A) //
			.text("InputSlot Timeout")),
	STATE_COM_SLOT_Sharc_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x06000B) //
			.text("CommunicationSlot Timeout")),
	STATE_STATUS_SLOT_Sharc_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x06000C) //
			.text("StatusSlot Timeout")),
	STATE_WHILE_LOOP_Sharc_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x06000D) //
			.text("While Loop Timeout")),
	STATE_IRQ_TIMEOUT_DSC_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x06000E) //
			.text("Interrupt Request Timeout")),
	STATE_SH_BUFFER_SPORT_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x06000F) //
			.text("Sharc Buffer Overflow")),
	STATE_SH_BUFFER_SPORT_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x06000F) //
			.text("Sharc Buffer Overflow")),
	STATE_BF_BUFFER_SPORT_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x060010) //
			.text("Blackfin Buffer Overflow")),
	STATE_S_INACTIVE_CCU_Master_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x060011) //
			.text("Master Inactive @ Slave")),
	STATE_S_INVALID_CCU_Master_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x060012) //
			.text("Master Packet Invalid @ Slave")),
	STATE_AUTOSTART_ENABLED_StateMachine_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x060013) //
			.text("Autostart enabled")),
	STATE_LCU_PRESSURE_LOSS_Fan_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x060014) //
			.text("LCU pressure loss trip")),
	STATE_LCU_TEMPERTURE_HIGH_Fan_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x060015) //
			.text("LCU temperature high")),
	STATE_DIGITAL_SYSTEMLOCK_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x060016) //
			.text("Digital Input : System Locked")),
	STATE_PROCX_EXTERN_Pro_CX_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x060017) //
			.text("Pro-CX: External error")),
	STATE_PROCX_OVERVOLTAGE_Pro_CX_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x060018) //
			.text("Pro-CX: Overvoltage")),
	STATE_PROCX_OVERCOMPENSATED_Pro_CX_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x060019) //
			.text("Pro-CX: Overcompensated")),
	STATE_PROCX_UNDERCOMPENSATED_Pro_CX_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x06001A) //
			.text("Pro-CX: Undercompensated")),
	STATE_PROCX_HARMONICS_Pro_CX_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x06001B) //
			.text("Pro-CX: Harmonics")),
	STATE_PROCX_TEMPERATURE_Pro_CX_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x06001C) //
			.text("Pro-CX: Temperature high")),
	STATE_PROCX_UNDERCURRENT_Pro_CX_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x06001D) //
			.text("Pro-CX: Undercurrent")),
	STATE_PROCX_UNDERVOLTAGE_Pro_CX_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x06001E) //
			.text("Pro-CX: Undervoltage")),
	STATE_PROCX_COMMUNICATION_Pro_CX_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x06001F) //
			.text("Pro-CX Communication Error")),
	STATE_DERATING_RMS_CURRENT_CONTROL_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.CFG_DERATING) //
			.needsHardReset(false) //
			.code(0x060020) //
			.text("Derating: RMS current")),
	STATE_DERATING_PEAK_GRIDCURRENT_CONTROL_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.CFG_DERATING) //
			.needsHardReset(false) //
			.code(0x060021) //
			.text("Derating: Output peak current")),
	STATE_DERATING_PEAK_MODULECURRENT_CONTROL_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.CFG_DERATING) //
			.needsHardReset(false) //
			.code(0x060022) //
			.text("Derating: IGBT peak current")),
	STATE_DERATING_MODULATIONSINDEX_CONTROL_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.CFG_DERATING) //
			.needsHardReset(false) //
			.code(0x060023) //
			.text("Derating: Modulation limit")),
	STATE_DERATING_DC_VOLTAGE_UNBALANCE_CONTROL_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.CFG_DERATING) //
			.needsHardReset(false) //
			.code(0x060024) //
			.text("Derating: DC voltage unbalance")),
	STATE_DERATING_DC_VOLTAGE_MAXIMUM_CONTROL_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.CFG_DERATING) //
			.needsHardReset(false) //
			.code(0x060025) //
			.text("Derating: DC voltage limit")),
	STATE_DERATING_CHOKE_TEMPERATURE_CONTROL_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.CFG_DERATING) //
			.needsHardReset(false) //
			.code(0x060026) //
			.text("Derating: Choke temperature")),
	STATE_DERATING_IGBT_TEMPERATURE_CONTROL_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.CFG_DERATING) //
			.needsHardReset(false) //
			.code(0x060027) //
			.text("Derating: IGBT temperature")),
	STATE_DERATING_MCU_TEMPERATURE_CONTROL_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.CFG_DERATING) //
			.needsHardReset(false) //
			.code(0x060028) //
			.text("Derating: MCU temperature")),
	STATE_DERATING_GRID_VOLTAGE_CONTROL_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.CFG_DERATING) //
			.needsHardReset(false) //
			.code(0x060029) //
			.text("Derating: GridVoltage")),
	STATE_DERATING_NEUTRAL_CURRENT_CONTROL_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.CFG_DERATING) //
			.needsHardReset(false) //
			.code(0x06002A) //
			.text("Derating: RMS current neutral")),
	STATE_STARTSTOP_MODE_StateMachine_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x06002B) //
			.text("Auto start-stop active")),
	STATE_WARMSTART_CCU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.INFO) //
			.needsHardReset(false) //
			.code(0x06002C) //
			.text("CCU warm restart triggered")),
	STATE_SIA_RUNNING_CONTROL_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE).reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x06002D).text("(XT) System Identification Algorithm (SIA) running...")),
	STATE_SIA_WARNING_CONTROL_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x06002E) //
			.text("(XT) SIA Warning")),
	STATE_SUSPECT_HARMONICS_DETECTED_CONTROL_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x06002F) //
			.text("(XT) Suspect Harmonics marked")),
	STATE_CRITICAL_HARMONICS_DETECTED_CONTROL_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE).reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x060030).text("(XT) Compensation of critical harmonics blocked")),
	STATE_VOLTAGE_NOT_VALID_FOR_START_ACDCs_CONTROL_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x060031) //
			.text("(XT) VOLT NOT VALID START ACDC")),
	STATE_FREQUENZ_NOT_VALID_FOR_START_ACDCs_CONTROL_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x060032) //
			.text("(XT) FREQ NOT VALID START ACDC")),
	STATE_PARAM_UNDEFINED_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(true) //
			.code(0x010000) //
			.text("Undefined fault parameter")),
	STATE_PARAM_NULL_POINTER_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(true) //
			.code(0x010001) //
			.text("Null pointer to fault parameter")),
	STATE_INVALID_PARAMETER_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(true) //
			.code(0x010002) //
			.text("Invalid fault parameter")),
	STATE_GROUP_INSPECTOR_NULL_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(true) //
			.code(0x010003) //
			.text("Undefined group inspector")),
	STATE_INVALID_CLIENT_ID_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x010004) //
			.text("Invalid Merkur client ID")),
	STATE_INVALID_HIERARCHY_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(true) //
			.code(0x010005) //
			.text("Invalid fault hierarchy")),
	STATE_MERKUR_INIT_FAIL_DSC_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x010006) //
			.text("Merkur module initialization failed")),
	STATE_INVALID_MIO_ORDER_DSC_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x010007) //
			.text("Invalid MIO ports sequence")),
	STATE_PQM_INVALID_CONFIG_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x010008).text("Invalid measurement configuration")),
	STATE_PQM_CHANNELS_OVERFLOW_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x010009).text("Measurement channels limit reached")),
	STATE_PQM_INVALID_FORMAT_Configuration_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x01000A).text("Invalid current sum configuration format")),
	STATE_PQM_CALCULATIONS_TIMEOUT_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x01000B) //
			.text("Measurements calculation timeout")),
	STATE_PQM_PACKET_MISSING__1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x01000C) //
			.text("Measurement packet missing")),
	STATE_PQM_INVALID_DATA__1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x01000D) //
			.text("Invalid measurement data")),
	STATE_RTDS_SOFTWARE_Software_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.INFO) //
			.needsHardReset(false) //
			.code(0x01000E) //
			.text("Warning: RTDS Version - Do not use on real hardware")),
	STATE_ARRAY_OVERFLOW_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(true) //
			.code(0x01000F) //
			.text("Array overflow")),
	STATE_AUTONOM_TEST_SW_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.INFO) //
			.needsHardReset(false) //
			.code(0x010010) //
			.text("Warning: AUTONOM TEST SW ( Autostart )")),
	STATE_NO_BLACKSTART_OR_SYNC_ENABLED_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x010011) //
			.text("no Blackstart or SyncV enabled")),
	STATE_SYNC_V_AND_BLACKSTART_ENABLED_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x010012) //
			.text("sycnV and Blackstart enabled")),
	STATE_VOLTAGE_NOT_ZERO_FOR_BLACKSTART_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x010013) //
			.text("voltage not zero for Blackstart")),
	STATE_VOLTAGE_SYNC_NOT_SUCCEEDED_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x010014) //
			.text("voltage sync not succeeded")),
	STATE_VOLTAGE_NOT_OK_FOR_SYNC_V_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x010015) //
			.text("voltage not ok for sync V")),
	STATE_GENERATOR_CONNECTED_WRONG_PHASE_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x010016) //
			.text("generator connected wrong phase")),
	STATE_FRT_FAULT_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x010017) //
			.text("FRT Error occurred")),
	STATE_IDC_OVERCURRENT_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x010018) //
			.text("DC-Link Overcurrent")),
	STATE_SUMMANDS_OVERFLOW_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x030000) //
			.text("Summands overflow")),
	STATE_CYCLIC_CONFIG_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x030001).text("Cyclic current sum configuration")),
	STATE_INVALID_PHASE_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x030002).text("Summand set to nonexistent phase")),
	STATE_INVALID_MIO_VOLTAGE_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x030003) //
			.text("Nonexistent MIO voltage channel")),
	STATE_INVALID_MIO_CURRENT_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x030004) //
			.text("Nonexistent MIO current channel")),
	STATE_INVALID_MIO_CURRENT_GROUPS_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x030005).text("Inconsistent MIO current channels grouping")),
	STATE_INVALID_DEVICE_CHANNEL_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x030006).text("Nonexistent device phase current")),
	STATE_CHANNEL_NULL_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x030007).text("Measurement channel not initialized")),
	STATE_PHASE_MISSING_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x030008).text("Device current configuration missing")),
	STATE_DEVICE_DISABLED_SOFTWARE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x030009).text("Initializing disabled device current")),
	STATE_ABCC_ERR_INVALID_PROC_WRITE_DATA_SIZE_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x070000) //
			.text("Invalid write process data size")),
	STATE_ABCC_ERR_INVALID_PROC_READ_DATA_SIZE_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x070001) //
			.text("Invalid read process data size")),
	STATE_ABCC_ERR_MODULE_MISSING_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x070002) //
			.text("Module missing")),
	STATE_ABCC_ERR_NODEID_NOT_SETABLE_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x070003) //
			.text("Cannot set node ID")),
	STATE_ABCC_ERR_ATTR_WRITE_ERROR_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x070004) //
			.text("Cannot set attribute")),
	STATE_ABCC_ERR_INIT_ATTR_READ_ERROR_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x070005).text("Error reading attributes during initialization")),
	STATE_ABCC_ERR_MODULE_WATCHDOG_TIMEOUT_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x070006) //
			.text("Module watchdog timeout")),
	STATE_ABCC_ERR_READING_EXCEPTION_CODE_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x070007) //
			.text("Error reading exception code")),
	STATE_ABCC_ERR_READING_NW_SPEC_EXCEPTION_CODE_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x070008).text("Error reading network specific exception code")),
	STATE_ABCC_ERR_NON_RECOVERABLE_EXCEPTION_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x070009) //
			.text("Non-recoverable exception")),
	STATE_ABCC_ERR_CONNECTION_TERMINATED_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x07000A) //
			.text("Connection terminated")),
	STATE_ABCC_ERR_NETWORK_ERROR_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x07000B) //
			.text("Network error")),
	STATE_ABCC_ERR_DRIVER_STATE_CHANGE_DENIED_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x07000C).text("Driver handler state change denied")),
	STATE_ABCC_ERR_MAX_RESTART_COUNT_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x07000D).text("Module restart count limit reached")),
	STATE_ABCC_ERR_MODULE_RESTART_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x07000E) //
			.text("Module restarted")),
	STATE_ABCC_ERR_DRIVER_TERMINATED_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x07000F) //
			.text("Driver terminated")),
	STATE_ABCC_EXCPT_APP_TO_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x070010) //
			.text("Application timeout")),
	STATE_ABCC_EXCPT_INV_DEV_ADDR_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x070011) //
			.text("Invalid device address")),
	STATE_ABCC_EXCPT_INV_COM_SETTINGS_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x070012) //
			.text("Invalid communication settings")),
	STATE_ABCC_EXCPT_MAJ_UNREC_APP_EVNT_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x070013).text("Major unrecoverable application event")),
	STATE_ABCC_EXCPT_WAIT_APP_RESET_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x070014) //
			.text("Waiting for application reset")),
	STATE_ABCC_EXCPT_INV_PRD_CFG_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x070015).text("Invalid process data configuration")),
	STATE_ABCC_EXCPT_INV_APP_RESPONSE_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x070016) //
			.text("Invalid application response")),
	STATE_ABCC_EXCPT_NVS_CHECKSUM_ERROR_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x070017).text("Non-volatile memory checksum error")),
	STATE_ABCC_EXCPT_NETWORK_SPECIFIC_1_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x070018).text("Network specific exception code: 0x01")),
	STATE_ABCC_EXCPT_NETWORK_SPECIFIC_2_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x070019).text("Network specific exception code: 0x02")),
	STATE_ABCC_EXCPT_NETWORK_SPECIFIC_3_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x07001A).text("Network specific exception code: 0x03")),
	STATE_ABCC_EXCPT_NETWORK_SPECIFIC_4_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x07001B).text("Network specific exception code: 0x04")),
	STATE_ABCC_EXCPT_NETWORK_SPECIFIC_5_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x07001C).text("Network specific exception code: 0x05")),
	STATE_ABCC_EXCPT_NETWORK_SPECIFIC_6_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x07001D).text("Network specific exception code: 0x06")),
	STATE_ABCC_EXCPT_NETWORK_SPECIFIC_7_ANYBUS_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.CFG_ANYBUS) //
			.needsHardReset(false) //
			.code(0x07001E).text("Network specific exception code: 0x07")),
	STATE_COMMUNICATION_FAULT_RACK_GROUP_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x050300) //
			.text("Communication Fault")),
	STATE_IMBALANCE_ALARM_RACK_GROUP_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x05030A) //
			.text("Imbalance alarm")),
	STATE_IMBALANCE_STOP_RACK_GROUP_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x05030B) //
			.text("Imbalance error")),
	STATE_VOLTAGE_DIFF_RACK_GROUP_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x05030C) //
			.text("DC voltage not synchronized")),
	STATE_NO_CONTACTOR_ON_COMMAND_RACK_GROUP_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x05030D) //
			.text("Close contactor command timeout")),
	STATE_CLOSE_BLOCKED_RACK_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x040200) //
			.text("Contactor closing blocked")),
	STATE_OPEN_BLOCKED_RACK_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x040201) //
			.text("Contactor opening blocked")),
	STATE_CELL_OVERTEMPERATURE_RACK_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x040301) //
			.text("Cell Overtemperature Trip")),
	STATE_CELL_UNDERVOLTAGE_RACK_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x040302) //
			.text("Cell Undervoltage Trip")),
	STATE_CELL_OVERVOLTAGE_RACK_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x040303) //
			.text("Cell Overvoltage Trip")),
	STATE_RACK_UNDERVOLTAGE_RACK_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x040307) //
			.text("Rack Undervoltage Trip")),
	STATE_RACK_OVERVOLTAGE_RACK_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x040308) //
			.text("Rack Overvoltage Trip")),
	STATE_RACK_COMM_FAULT_RACK_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x040309) //
			.text("Rack Communication Fault")),
	STATE_TRAY_COMM_FAULT_RACK_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x04030A) //
			.text("Tray Communication Fault")),
	STATE_OVERCURRENT_RACK_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x04030B) //
			.text("Overcurrent Trip")),
	STATE_ADDITIONAL_PROTECTION_RACK_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x04030D) //
			.text("Additional Protection Tray")),
	STATE_DC_CONTACTOR_FAULT_RACK_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x04030E) //
			.text("DC Contactor Fault")),
	STATE_DC_CONTACTOR_SENSOR_FAULT_RACK_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x04030F) //
			.text("DC Contactor Sensor Fault")),
	STATE_CELL_UNDERTEMPERATURE_RACK_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x040310) //
			.text("Warning: Cell Undertemperature")),
	STATE_CELL_OVERTEMPERATURE_RACK_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x040311) //
			.text("Warning: Cell Overtemperature")),
	STATE_CELL_UNDERVOLTAGE_RACK_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x040312) //
			.text("Warning: Cell Undervoltage")),
	STATE_CELL_OVERVOLTAGE_RACK_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x040313) //
			.text("Warning: Cell Overvoltage")),
	STATE_RACK_VOLTAGE_ERROR_RACK_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x040314) //
			.text("Warning: Rack voltage deviates from sum")),
	STATE_CELL_VOLTAGE_IMBALANCE_RACK_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x040315) //
			.text("Warning: Cell Voltage Imbalance")),
	STATE_CELL_TEMPERATURE_IMBALANCE_RACK_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x040316) //
			.text("Warning: Cell Temperature Imbalance")),
	STATE_RACK_UNDERVOLTAGE_RACK_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x040317) //
			.text("Warning: Rack Undervoltage")),
	STATE_RACK_OVERVOLTAGE_RACK_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x040318) //
			.text("Warning: Rack Overvoltage")),
	STATE_RACK_COMM_FAULT_RACK_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x040319) //
			.text("Warning: Rack Communication Fault")),
	STATE_TRAY_COMM_FAULT_RACK_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x04031A) //
			.text("Warning: Tray Communication Fault")),
	STATE_OVERCURRENT_RACK_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x04031B) //
			.text("Warning: Rack Overcurrent")),
	STATE_SOC_FULL_RACK_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.INFO) //
			.needsHardReset(false) //
			.code(0x04031C) //
			.text("Rack Charge Completed")),
	STATE_SENSOR_COMMUNICATION_FAULT_RACK_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x04031D) //
			.text("Warning: Current Sensor Communication Fault")),
	STATE_CHARGE_OVERCURRENT_RACK_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x04031E) //
			.text("Warning: Charge Current Limit Exceeded")),
	STATE_FAN_FAULT_RACK_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x04031F) //
			.text("Warning: Fan Fault")),
	FLOAT_UNDERFLOW_FOR_PROPERTY(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.INFO) //
			.needsHardReset(false) //
			.code(0x08000C) //
			.text("Float underflow for property")),;

	private final Doc doc;

	private ErrorCodeChannelId1(Doc doc) {
		this.doc = doc;
	}

	public Doc doc() {
		return this.doc;
	}

}
