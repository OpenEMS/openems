package io.openems.edge.ess.mr.gridcon.enums;

import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.ess.mr.gridcon.enums.ErrorDoc.Acknowledge;
import io.openems.edge.ess.mr.gridcon.enums.ErrorDoc.ReactionLevel;

/**
 * This enum holds every possible error channel id for a gridcon.
 */
public enum ErrorCodeChannelId1 implements io.openems.edge.common.channel.doc.ChannelId {

	STATE_MERKUR_PORT0_DSC_1(
			new ErrorDoc().acknowledge(Acknowledge.NO_ACKNOWLEDGE).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x060000).text("DSC 10 removed !Restart!  ").level(Level.WARNING)),
	STATE_MERKUR_PORT1_DSC_1(
			new ErrorDoc().acknowledge(Acknowledge.NO_ACKNOWLEDGE).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x060001).text("DSC 9 removed !Restart!  ").level(Level.WARNING)),
	STATE_MERKUR_PORT2_DSC_1(
			new ErrorDoc().acknowledge(Acknowledge.NO_ACKNOWLEDGE).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x060002).text("DSC 8 removed !Restart!  ").level(Level.WARNING)),
	STATE_MERKUR_PORT3_DSC_1(
			new ErrorDoc().acknowledge(Acknowledge.NO_ACKNOWLEDGE).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x060003).text("DSC 7 removed !Restart!  ").level(Level.WARNING)),
	STATE_MERKUR_PORT4_DSC_1(
			new ErrorDoc().acknowledge(Acknowledge.NO_ACKNOWLEDGE).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x060004).text("DSC 6 removed !Restart!  ").level(Level.WARNING)),
	STATE_MERKUR_PORT5_DSC_1(
			new ErrorDoc().acknowledge(Acknowledge.NO_ACKNOWLEDGE).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x060005).text("DSC 5 removed !Restart!  ").level(Level.WARNING)),
	STATE_MERKUR_PORT6_DSC_1(
			new ErrorDoc().acknowledge(Acknowledge.NO_ACKNOWLEDGE).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x060006).text("DSC 4 removed !Restart!  ").level(Level.WARNING)),
	STATE_MERKUR_PORT7_DSC_1(
			new ErrorDoc().acknowledge(Acknowledge.NO_ACKNOWLEDGE).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x060007).text("DSC 3 removed !Restart!  ").level(Level.WARNING)),
	STATE_MERKUR_PORT8_DSC_1(
			new ErrorDoc().acknowledge(Acknowledge.NO_ACKNOWLEDGE).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x060008).text("DSC 2 removed !Restart!  ").level(Level.WARNING)),
	STATE_MERKUR_PORT9_DSC_1(
			new ErrorDoc().acknowledge(Acknowledge.NO_ACKNOWLEDGE).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x060009).text("DSC 1 removed !Restart! ").level(Level.WARNING)),
	STATE_INPUT_SLOT_Blackfin_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
			.needsHardReset(false).code(0x06000A).text("InputSlot Timeout").level(Level.WARNING)),
	STATE_COM_SLOT_Sharc_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
			.needsHardReset(false).code(0x06000B).text("CommunicationSlot Timeout").level(Level.WARNING)),
	STATE_STATUS_SLOT_Sharc_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING)
			.needsHardReset(false).code(0x06000C).text("StatusSlot Timeout").level(Level.WARNING)),
	STATE_WHILE_LOOP_Sharc_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
			.needsHardReset(false).code(0x06000D).text("While Loop Timeout").level(Level.WARNING)),
	STATE_IRQ_TIMEOUT_DSC_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
			.needsHardReset(false).code(0x06000E).text("Interrupt Request Timeout").level(Level.WARNING)),
	STATE_SH_BUFFER_SPORT_1_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
			.needsHardReset(false).code(0x06000F).text("Sharc Buffer Overflow").level(Level.WARNING)),
	STATE_SH_BUFFER_SPORT_1_2(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING)
			.needsHardReset(false).code(0x06000F).text("Sharc Buffer Overflow").level(Level.WARNING)),
	STATE_BF_BUFFER_SPORT_1(
			new ErrorDoc().acknowledge(Acknowledge.AUTO_ACKNOWLEDGE).reactionLevel(ReactionLevel.WARNING)
					.needsHardReset(false).code(0x060010).text("Blackfin Buffer Overflow").level(Level.WARNING)),
	STATE_S_INACTIVE_CCU_Master_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.FORCED)
			.needsHardReset(false).code(0x060011).text("Master Inactive @ Slave").level(Level.WARNING)),
	STATE_S_INVALID_CCU_Master_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.FORCED)
			.needsHardReset(false).code(0x060012).text("Master Packet Invalid @ Slave").level(Level.WARNING)),
	STATE_AUTOSTART_ENABLED_StateMachine_1(
			new ErrorDoc().acknowledge(Acknowledge.AUTO_ACKNOWLEDGE).reactionLevel(ReactionLevel.WARNING)
					.needsHardReset(false).code(0x060013).text("Autostart enabled").level(Level.WARNING)),
	STATE_LCU_PRESSURE_LOSS_Fan_1(new ErrorDoc().acknowledge(Acknowledge.RESTART).reactionLevel(ReactionLevel.SHUTDOWN)
			.needsHardReset(false).code(0x060014).text("LCU pressure loss trip").level(Level.WARNING)),
	STATE_LCU_TEMPERTURE_HIGH_Fan_1(
			new ErrorDoc().acknowledge(Acknowledge.AUTO_ACKNOWLEDGE).reactionLevel(ReactionLevel.WARNING)
					.needsHardReset(false).code(0x060015).text("LCU temperature high").level(Level.WARNING)),
	STATE_DIGITAL_SYSTEMLOCK_MIO_1(
			new ErrorDoc().acknowledge(Acknowledge.AUTO_ACKNOWLEDGE).reactionLevel(ReactionLevel.WARNING)
					.needsHardReset(false).code(0x060016).text("Digital Input : System Locked").level(Level.WARNING)),
	STATE_PROCX_EXTERN_Pro_CX_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING)
			.needsHardReset(false).code(0x060017).text("Pro-CX: External error").level(Level.WARNING)),
	STATE_PROCX_OVERVOLTAGE_Pro_CX_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x060018).text("Pro-CX: Overvoltage").level(Level.WARNING)),
	STATE_PROCX_OVERCOMPENSATED_Pro_CX_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x060019).text("Pro-CX: Overcompensated").level(Level.WARNING)),
	STATE_PROCX_UNDERCOMPENSATED_Pro_CX_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x06001A).text("Pro-CX: Undercompensated").level(Level.WARNING)),
	STATE_PROCX_HARMONICS_Pro_CX_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x06001B).text("Pro-CX: Harmonics").level(Level.WARNING)),
	STATE_PROCX_TEMPERATURE_Pro_CX_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x06001C).text("Pro-CX: Temperature high").level(Level.WARNING)),
	STATE_PROCX_UNDERCURRENT_Pro_CX_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x06001D).text("Pro-CX: Undercurrent").level(Level.WARNING)),
	STATE_PROCX_UNDERVOLTAGE_Pro_CX_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x06001E).text("Pro-CX: Undervoltage").level(Level.WARNING)),
	STATE_PROCX_COMMUNICATION_Pro_CX_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x06001F).text("Pro-CX Communication Error").level(Level.WARNING)),
	STATE_DERATING_RMS_CURRENT_Control_1(
			new ErrorDoc().acknowledge(Acknowledge.AUTO_ACKNOWLEDGE).reactionLevel(ReactionLevel.CFG_DERATING)
					.needsHardReset(false).code(0x060020).text("Derating: RMS current").level(Level.WARNING)),
	STATE_DERATING_PEAK_GRIDCURRENT_Control_1(
			new ErrorDoc().acknowledge(Acknowledge.AUTO_ACKNOWLEDGE).reactionLevel(ReactionLevel.CFG_DERATING)
					.needsHardReset(false).code(0x060021).text("Derating: Output peak current").level(Level.WARNING)),
	STATE_DERATING_PEAK_MODULECURRENT_Control_1(
			new ErrorDoc().acknowledge(Acknowledge.AUTO_ACKNOWLEDGE).reactionLevel(ReactionLevel.CFG_DERATING)
					.needsHardReset(false).code(0x060022).text("Derating: IGBT peak current").level(Level.WARNING)),
	STATE_DERATING_MODULATIONSINDEX_Control_1(
			new ErrorDoc().acknowledge(Acknowledge.AUTO_ACKNOWLEDGE).reactionLevel(ReactionLevel.CFG_DERATING)
					.needsHardReset(false).code(0x060023).text("Derating: Modulation limit").level(Level.WARNING)),
	STATE_DERATING_DC_VOLTAGE_UNBALANCE_Control_1(
			new ErrorDoc().acknowledge(Acknowledge.AUTO_ACKNOWLEDGE).reactionLevel(ReactionLevel.CFG_DERATING)
					.needsHardReset(false).code(0x060024).text("Derating: DC voltage unbalance").level(Level.WARNING)),
	STATE_DERATING_DC_VOLTAGE_MAXIMUM_Control_1(
			new ErrorDoc().acknowledge(Acknowledge.AUTO_ACKNOWLEDGE).reactionLevel(ReactionLevel.CFG_DERATING)
					.needsHardReset(false).code(0x060025).text("Derating: DC voltage limit").level(Level.WARNING)),
	STATE_DERATING_CHOKE_TEMPERATURE_Control_1(
			new ErrorDoc().acknowledge(Acknowledge.AUTO_ACKNOWLEDGE).reactionLevel(ReactionLevel.CFG_DERATING)
					.needsHardReset(false).code(0x060026).text("Derating: Choke temperature").level(Level.WARNING)),
	STATE_DERATING_IGBT_TEMPERATURE_Control_1(
			new ErrorDoc().acknowledge(Acknowledge.AUTO_ACKNOWLEDGE).reactionLevel(ReactionLevel.CFG_DERATING)
					.needsHardReset(false).code(0x060027).text("Derating: IGBT temperature").level(Level.WARNING)),
	STATE_DERATING_MCU_TEMPERATURE_Control_1(
			new ErrorDoc().acknowledge(Acknowledge.AUTO_ACKNOWLEDGE).reactionLevel(ReactionLevel.CFG_DERATING)
					.needsHardReset(false).code(0x060028).text("Derating: MCU temperature").level(Level.WARNING)),
	STATE_DERATING_GRID_VOLTAGE_Control_1(
			new ErrorDoc().acknowledge(Acknowledge.AUTO_ACKNOWLEDGE).reactionLevel(ReactionLevel.CFG_DERATING)
					.needsHardReset(false).code(0x060029).text("Derating: GridVoltage").level(Level.WARNING)),
	STATE_DERATING_NEUTRAL_CURRENT_Control_1(
			new ErrorDoc().acknowledge(Acknowledge.AUTO_ACKNOWLEDGE).reactionLevel(ReactionLevel.CFG_DERATING)
					.needsHardReset(false).code(0x06002A).text("Derating: RMS current neutral").level(Level.WARNING)),
	STATE_STARTSTOP_MODE_StateMachine_1(
			new ErrorDoc().acknowledge(Acknowledge.AUTO_ACKNOWLEDGE).reactionLevel(ReactionLevel.WARNING)
					.needsHardReset(false).code(0x06002B).text("Auto start-stop active").level(Level.WARNING)),
	STATE_WARMSTART_CCU_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.INFO)
			.needsHardReset(false).code(0x06002C).text("CCU warm restart triggered").level(Level.WARNING)),
	STATE_SIA_RUNNING_Control_1(new ErrorDoc().acknowledge(Acknowledge.AUTO_ACKNOWLEDGE)
			.reactionLevel(ReactionLevel.WARNING).needsHardReset(false).code(0x06002D)
			.text("(XT) System Identification Algorithm (SIA) running...").level(Level.WARNING)),
	STATE_SIA_WARNING_Control_1(
			new ErrorDoc().acknowledge(Acknowledge.AUTO_ACKNOWLEDGE).reactionLevel(ReactionLevel.WARNING)
					.needsHardReset(false).code(0x06002E).text("(XT) SIA Warning").level(Level.WARNING)),
	STATE_SUSPECT_HARMONICS_DETECTED_Control_1(
			new ErrorDoc().acknowledge(Acknowledge.AUTO_ACKNOWLEDGE).reactionLevel(ReactionLevel.WARNING)
					.needsHardReset(false).code(0x06002F).text("(XT) Suspect Harmonics marked").level(Level.WARNING)),
	STATE_CRITICAL_HARMONICS_DETECTED_Control_1(new ErrorDoc().acknowledge(Acknowledge.AUTO_ACKNOWLEDGE)
			.reactionLevel(ReactionLevel.WARNING).needsHardReset(false).code(0x060030)
			.text("(XT) Compensation of critical harmonics blocked").level(Level.WARNING)),
	STATE_VOLTAGE_NOT_VALID_FOR_START_ACDCs_Control_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x060031).text("(XT) VOLT NOT VALID START ACDC").level(Level.WARNING)),
	STATE_FREQUENZ_NOT_VALID_FOR_START_ACDCs_Control_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x060032).text("(XT) FREQ NOT VALID START ACDC").level(Level.WARNING)),
	STATE_PARAM_UNDEFINED_Software_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x010000).text("Undefined fault parameter").level(Level.WARNING)),
	STATE_PARAM_NULL_POINTER_Software_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x010001).text("Null pointer to fault parameter").level(Level.WARNING)),
	STATE_INVALID_PARAMETER_Software_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x010002).text("Invalid fault parameter").level(Level.WARNING)),
	STATE_GROUP_INSPECTOR_NULL_Software_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x010003).text("Undefined group inspector").level(Level.WARNING)),
	STATE_INVALID_CLIENT_ID_Software_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x010004).text("Invalid Merkur client ID").level(Level.WARNING)),
	STATE_INVALID_HIERARCHY_Software_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x010005).text("Invalid fault hierarchy").level(Level.WARNING)),
	STATE_MERKUR_INIT_FAIL_DSC_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING)
			.needsHardReset(false).code(0x010006).text("Merkur module initialization failed").level(Level.WARNING)),
	STATE_INVALID_MIO_ORDER_DSC_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x010007).text("Invalid MIO ports sequence").level(Level.WARNING)),
	STATE_PQM_INVALID_CONFIG_Software_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.SHUTDOWN).needsHardReset(false).code(0x010008)
			.text("Invalid measurement configuration").level(Level.WARNING)),
	STATE_PQM_CHANNELS_OVERFLOW_Software_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.SHUTDOWN).needsHardReset(false).code(0x010009)
			.text("Measurement channels limit reached").level(Level.WARNING)),
	STATE_PQM_INVALID_FORMAT_Configuration_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.SHUTDOWN).needsHardReset(false).code(0x01000A)
			.text("Invalid current sum configuration format").level(Level.WARNING)),
	STATE_PQM_CALCULATIONS_TIMEOUT_Software_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x01000B).text("Measurements calculation timeout").level(Level.WARNING)),
	STATE_PQM_PACKET_MISSING__1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING)
			.needsHardReset(false).code(0x01000C).text("Measurement packet missing").level(Level.WARNING)),
	STATE_PQM_INVALID_DATA__1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING)
			.needsHardReset(false).code(0x01000D).text("Invalid measurement data").level(Level.WARNING)),
	STATE_RTDS_SOFTWARE_Software_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.INFO).needsHardReset(false)
					.code(0x01000E).text("Warning: RTDS Version - Do not use on real hardware").level(Level.WARNING)),
	STATE_ARRAY_OVERFLOW_Software_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x01000F).text("Array oveflow").level(Level.WARNING)),
	STATE_AUTONOM_TEST_SW_Software_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.INFO)
			.needsHardReset(false).code(0x010010).text("Warning: AUTONOM TEST SW ( Autostart )").level(Level.WARNING)),
	STATE_NO_BLACKSTART_OR_SYNC_ENABLED_Software_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x010011).text("no Blackstart or SyncV enabled").level(Level.WARNING)),
	STATE_SYNC_V_AND_BLACKSTART_ENABLED_Software_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x010012).text("sycnV and Blackstart enabled").level(Level.WARNING)),
	STATE_VOLTAGE_NOT_ZERO_FOR_BLACKSTART_Software_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x010013).text("voltage not zero for Blackstart").level(Level.WARNING)),
	STATE_VOLTAGE_SYNC_NOT_SUCCEDDED_Software_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x010014).text("voltage sync not succedded").level(Level.WARNING)),
	STATE_VOLTAGE_NOT_OK_FOR_SYNC_V_Software_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x010015).text("voltage not ok for sync V").level(Level.WARNING)),
	STATE_GENERATOR_CONNECTED_WRONG_PHASE_Software_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x010016).text("generator connected wrong phase").level(Level.WARNING)),
	STATE_FRT_FAULT_Software_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
			.needsHardReset(false).code(0x010017).text("FRT Error occured").level(Level.WARNING)),
	STATE_IDC_OVERCURRENT_Software_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x010018).text("DC-Link Overcurrent").level(Level.WARNING)),
	STATE_SUMMANDS_OVERFLOW_Software_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x030000).text("Summands overflow").level(Level.WARNING)),
	STATE_CYCLIC_CONFIG_Software_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.SHUTDOWN).needsHardReset(false).code(0x030001)
			.text("Cyclic current sum configuration").level(Level.WARNING)),
	STATE_INVALID_PHASE_Software_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.SHUTDOWN).needsHardReset(false).code(0x030002)
			.text("Summand set to nonexistent phase").level(Level.WARNING)),
	STATE_INVALID_MIO_VOLTAGE_Software_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x030003).text("Nonexistent MIO voltage channel").level(Level.WARNING)),
	STATE_INVALID_MIO_CURRENT_Software_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x030004).text("Nonexistent MIO current channel").level(Level.WARNING)),
	STATE_INVALID_MIO_CURRENT_GROUPS_Software_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.SHUTDOWN).needsHardReset(false).code(0x030005)
			.text("Inconsistent MIO current channels grouping").level(Level.WARNING)),
	STATE_INVALID_DEVICE_CHANNEL_Software_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.SHUTDOWN).needsHardReset(false).code(0x030006)
			.text("Nonexistent device phase current").level(Level.WARNING)),
	STATE_CHANNEL_NULL_Software_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.SHUTDOWN).needsHardReset(false).code(0x030007)
			.text("Measurement channel not initialized").level(Level.WARNING)),
	STATE_PHASE_MISSING_Software_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.SHUTDOWN).needsHardReset(false).code(0x030008)
			.text("Device current configuration missing").level(Level.WARNING)),
	STATE_DEVICE_DISABLED_Software_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.SHUTDOWN).needsHardReset(false).code(0x030009)
			.text("Initializing disabled device current").level(Level.WARNING)),
	STATE_ABCC_ERR_INVALID_PROC_WRITE_DATA_SIZE_Anybus_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS)
					.needsHardReset(false).code(0x070000).text("Invalid write process data size").level(Level.WARNING)),
	STATE_ABCC_ERR_INVALID_PROC_READ_DATA_SIZE_Anybus_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS)
					.needsHardReset(false).code(0x070001).text("Invalid read process data size").level(Level.WARNING)),
	STATE_ABCC_ERR_MODULE_MISSING_Anybus_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS)
					.needsHardReset(false).code(0x070002).text("Module missing").level(Level.WARNING)),
	STATE_ABCC_ERR_NODEID_NOT_SETABLE_Anybus_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS)
					.needsHardReset(false).code(0x070003).text("Cannot set node ID").level(Level.WARNING)),
	STATE_ABCC_ERR_ATTR_WRITE_ERROR_Anybus_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS)
					.needsHardReset(false).code(0x070004).text("Cannot set attribute").level(Level.WARNING)),
	STATE_ABCC_ERR_INIT_ATTR_READ_ERROR_Anybus_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.CFG_ANYBUS).needsHardReset(false).code(0x070005)
			.text("Error reading attributes during initialization").level(Level.WARNING)),
	STATE_ABCC_ERR_MODULE_WATCHDOG_TIMEOUT_Anybus_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS)
					.needsHardReset(false).code(0x070006).text("Module watchdog timeout").level(Level.WARNING)),
	STATE_ABCC_ERR_READING_EXCEPTION_CODE_Anybus_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS)
					.needsHardReset(false).code(0x070007).text("Error reading exception code").level(Level.WARNING)),
	STATE_ABCC_ERR_READING_NW_SPEC_EXCEPTION_CODE_Anybus_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.CFG_ANYBUS).needsHardReset(false).code(0x070008)
			.text("Error reading network specific exception code").level(Level.WARNING)),
	STATE_ABCC_ERR_NON_RECOVERABLE_EXCEPTION_Anybus_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS)
					.needsHardReset(false).code(0x070009).text("Non-recoverable exception").level(Level.WARNING)),
	STATE_ABCC_ERR_CONNECTION_TERMINATED_Anybus_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS)
					.needsHardReset(false).code(0x07000A).text("Connection terminated").level(Level.WARNING)),
	STATE_ABCC_ERR_NETWORK_ERROR_Anybus_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS)
					.needsHardReset(false).code(0x07000B).text("Network error").level(Level.WARNING)),
	STATE_ABCC_ERR_DRIVER_STATE_CHANGE_DENIED_Anybus_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.CFG_ANYBUS).needsHardReset(false).code(0x07000C)
			.text("Driver handler state change denied").level(Level.WARNING)),
	STATE_ABCC_ERR_MAX_RESTART_COUNT_Anybus_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.CFG_ANYBUS).needsHardReset(false).code(0x07000D)
			.text("Module restart count limit reached").level(Level.WARNING)),
	STATE_ABCC_ERR_MODULE_RESTART_Anybus_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x07000E).text("Module restarted").level(Level.WARNING)),
	STATE_ABCC_ERR_DRIVER_TERMINATED_Anybus_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS)
					.needsHardReset(false).code(0x07000F).text("Driver terminated").level(Level.WARNING)),
	STATE_ABCC_EXCPT_APP_TO_Anybus_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS)
					.needsHardReset(false).code(0x070010).text("Application timeout").level(Level.WARNING)),
	STATE_ABCC_EXCPT_INV_DEV_ADDR_Anybus_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS)
					.needsHardReset(false).code(0x070011).text("Invalid device address").level(Level.WARNING)),
	STATE_ABCC_EXCPT_INV_COM_SETTINGS_Anybus_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS)
					.needsHardReset(false).code(0x070012).text("Invalid communication settings").level(Level.WARNING)),
	STATE_ABCC_EXCPT_MAJ_UNREC_APP_EVNT_Anybus_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.CFG_ANYBUS).needsHardReset(false).code(0x070013)
			.text("Major unrecoverable application event").level(Level.WARNING)),
	STATE_ABCC_EXCPT_WAIT_APP_RESET_Anybus_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS)
					.needsHardReset(false).code(0x070014).text("Waiting for application reset").level(Level.WARNING)),
	STATE_ABCC_EXCPT_INV_PRD_CFG_Anybus_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.CFG_ANYBUS).needsHardReset(false).code(0x070015)
			.text("Invalid process data configuration").level(Level.WARNING)),
	STATE_ABCC_EXCPT_INV_APP_RESPONSE_Anybus_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.CFG_ANYBUS)
					.needsHardReset(false).code(0x070016).text("Invalid application response").level(Level.WARNING)),
	STATE_ABCC_EXCPT_NVS_CHECKSUM_ERROR_Anybus_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.CFG_ANYBUS).needsHardReset(false).code(0x070017)
			.text("Non-volatile memory checksum error").level(Level.WARNING)),
	STATE_ABCC_EXCPT_NETWORK_SPECIFIC_1_Anybus_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.CFG_ANYBUS).needsHardReset(false).code(0x070018)
			.text("Network specific exception code: 0x01").level(Level.WARNING)),
	STATE_ABCC_EXCPT_NETWORK_SPECIFIC_2_Anybus_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.CFG_ANYBUS).needsHardReset(false).code(0x070019)
			.text("Network specific exception code: 0x02").level(Level.WARNING)),
	STATE_ABCC_EXCPT_NETWORK_SPECIFIC_3_Anybus_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.CFG_ANYBUS).needsHardReset(false).code(0x07001A)
			.text("Network specific exception code: 0x03").level(Level.WARNING)),
	STATE_ABCC_EXCPT_NETWORK_SPECIFIC_4_Anybus_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.CFG_ANYBUS).needsHardReset(false).code(0x07001B)
			.text("Network specific exception code: 0x04").level(Level.WARNING)),
	STATE_ABCC_EXCPT_NETWORK_SPECIFIC_5_Anybus_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.CFG_ANYBUS).needsHardReset(false).code(0x07001C)
			.text("Network specific exception code: 0x05").level(Level.WARNING)),
	STATE_ABCC_EXCPT_NETWORK_SPECIFIC_6_Anybus_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.CFG_ANYBUS).needsHardReset(false).code(0x07001D)
			.text("Network specific exception code: 0x06").level(Level.WARNING)),
	STATE_ABCC_EXCPT_NETWORK_SPECIFIC_7_Anybus_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED)
			.reactionLevel(ReactionLevel.CFG_ANYBUS).needsHardReset(false).code(0x07001E)
			.text("Network specific exception code: 0x07").level(Level.WARNING)),
	STATE_COMMUNICATION_FAULT_Rack_Group_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x050300).text("Communication Fault").level(Level.WARNING)),
	STATE_IMBALANCE_ALARM_Rack_Group_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x05030A).text("Imbalance alarm").level(Level.WARNING)),
	STATE_IMBALANCE_STOP_Rack_Group_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x05030B).text("Imbalance error").level(Level.WARNING)),
	STATE_VOLTAGE_DIFF_Rack_Group_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x05030C).text("DC voltage not synchronized").level(Level.WARNING)),
	STATE_NO_CONTACTOR_ON_COMMAND_Rack_Group_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x05030D).text("Close contactor command timeout").level(Level.WARNING)),
	STATE_CLOSE_BLOCKED_Rack_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING)
			.needsHardReset(false).code(0x040200).text("Contactor closing blocked").level(Level.WARNING)),
	STATE_OPEN_BLOCKED_Rack_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING)
			.needsHardReset(false).code(0x040201).text("Contactor opening blocked").level(Level.WARNING)),
	STATE_CELL_OVERTEMPERATURE_Rack_1_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x040301).text("Cell Overtemperature Trip").level(Level.WARNING)),
	STATE_CELL_UNDERVOLTAGE_Rack_1_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x040302).text("Cell Undervoltage Trip").level(Level.WARNING)),
	STATE_CELL_OVERVOLTAGE_Rack_1_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x040303).text("Cell Overvoltage Trip").level(Level.WARNING)),
	STATE_RACK_UNDERVOLTAGE_Rack_1_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x040307).text("Rack Undervoltage Trip").level(Level.WARNING)),
	STATE_RACK_OVERVOLTAGE_Rack_1_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x040308).text("Rack Overvoltage Trip").level(Level.WARNING)),
	STATE_RACK_COMM_FAULT_Rack_1_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x040309).text("Rack Communication Fault").level(Level.WARNING)),
	STATE_TRAY_COMM_FAULT_Rack_1_2(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x04030A).text("Tray Communication Fault").level(Level.WARNING)),
	STATE_OVERCURRENT_Rack_1_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
			.needsHardReset(false).code(0x04030B).text("Overcurrent Trip").level(Level.WARNING)),
	STATE_ADDITIONAL_PROTECTION_Rack_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x04030D).text("Additional Protection Tray").level(Level.WARNING)),
	STATE_DC_CONTACTOR_FAULT_Rack_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x04030E).text("DC Contactor Fault").level(Level.WARNING)),
	STATE_DC_CONTACTOR_SENSOR_FAULT_Rack_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.SHUTDOWN)
					.needsHardReset(false).code(0x04030F).text("DC Contactor Sensor Fault").level(Level.WARNING)),
	STATE_CELL_UNDERTEMPERATURE_Rack_1_2(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x040310).text("Warning: Cell Undertemperature").level(Level.WARNING)),
	STATE_CELL_OVERTEMPERATURE_Rack_1_2(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x040311).text("Warning: Cell Overtemperature").level(Level.WARNING)),
	STATE_CELL_UNDERVOLTAGE_Rack_1_2(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x040312).text("Warning: Cell Undervoltage").level(Level.WARNING)),
	STATE_CELL_OVERVOLTAGE_Rack_1_2(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x040313).text("Warning: Cell Overvoltage").level(Level.WARNING)),
	STATE_RACK_VOLTAGE_ERROR_Rack_1_2(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x040314).text("Warning: Rack voltage deviates from sum").level(Level.WARNING)),
	STATE_CELL_VOLTAGE_IMBALANCE_Rack_1_2(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x040315).text("Warning: Cell Voltage Imbalance").level(Level.WARNING)),
	STATE_CELL_TEMPERATURE_IMBALANCE_Rack_1_2(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x040316).text("Warning: Cell Temperature Imbalance").level(Level.WARNING)),
	STATE_RACK_UNDERVOLTAGE_Rack_1_2(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x040317).text("Warning: Rack Undervoltage").level(Level.WARNING)),
	STATE_RACK_OVERVOLTAGE_Rack_1_2(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x040318).text("Warning: Rack Overvoltage").level(Level.WARNING)),
	STATE_RACK_COMM_FAULT_Rack_1_2(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x040319).text("Warning: Rack Communication Fault").level(Level.WARNING)),
	STATE_TRAY_COMM_FAULT_Rack_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING)
			.needsHardReset(false).code(0x04031A).text("Warning: Tray Communication Fault").level(Level.WARNING)),
	STATE_OVERCURRENT_Rack_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING)
			.needsHardReset(false).code(0x04031B).text("Warning: Rack Overcurrent").level(Level.WARNING)),
	STATE_SOC_FULL_Rack_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.INFO)
			.needsHardReset(false).code(0x04031C).text("Rack Charge Completed").level(Level.WARNING)),
	STATE_SENSOR_COMMUNICATION_FAULT_Rack_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x04031D).text("Warning: Current Sensor Communication Fault").level(Level.WARNING)),
	STATE_CHARGE_OVERCURRENT_Rack_1(
			new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING).needsHardReset(false)
					.code(0x04031E).text("Warning: Charge Current Limit Exceeded").level(Level.WARNING)),
	STATE_FAN_FAULT_Rack_1(new ErrorDoc().acknowledge(Acknowledge.UNDEFINED).reactionLevel(ReactionLevel.WARNING)
			.needsHardReset(false).code(0x04031F).text("Warning: Fan Fault").level(Level.WARNING)),
	;

	private final Doc doc;

	private ErrorCodeChannelId1(Doc doc) {
		this.doc = doc;
	}

	public Doc doc() {
		return this.doc;
	}

}
