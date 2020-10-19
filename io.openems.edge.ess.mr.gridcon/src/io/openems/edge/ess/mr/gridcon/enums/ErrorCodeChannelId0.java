package io.openems.edge.ess.mr.gridcon.enums;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.ess.mr.gridcon.enums.ErrorDoc.Acknowledge;
import io.openems.edge.ess.mr.gridcon.enums.ErrorDoc.ReactionLevel;

/**
 * This enum holds every possible error channel id for a gridcon.
 */
public enum ErrorCodeChannelId0 implements ChannelId {

	STATE_TEMP_TRIP_IGBT_3_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201200) //
			.text("Temp Trip IGBT 3")),
	STATE_TEMP_TRIP_IGBT_3_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202200) //
			.text("Temp Trip IGBT 3")),
	STATE_TEMP_TRIP_IGBT_3_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203200) //
			.text("Temp Trip IGBT 3")),
	STATE_TEMP_TRIP_IGBT_3_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204200) //
			.text("Temp Trip IGBT 3")),
	STATE_TEMP_TRIP_IGBT_2_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201201) //
			.text("Temp Trip IGBT 2")),
	STATE_TEMP_TRIP_IGBT_2_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202201) //
			.text("Temp Trip IGBT 2")),
	STATE_TEMP_TRIP_IGBT_2_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203201) //
			.text("Temp Trip IGBT 2")),
	STATE_TEMP_TRIP_IGBT_2_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204201) //
			.text("Temp Trip IGBT 2")),
	STATE_TEMP_TRIP_IGBT_1_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201202) //
			.text("Temp Trip IGBT 1")),
	STATE_TEMP_TRIP_IGBT_1_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202202) //
			.text("Temp Trip IGBT 1")),
	STATE_TEMP_TRIP_IGBT_1_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203202) //
			.text("Temp Trip IGBT 1")),
	STATE_TEMP_TRIP_IGBT_1_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204202) //
			.text("Temp Trip IGBT 1")),
	STATE_TEMP_TRIP_4_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201203) //
			.text("Temp Trip Board")),
	STATE_TEMP_TRIP_4_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202203) //
			.text("Temp Trip Board")),
	STATE_TEMP_TRIP_4_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203203) //
			.text("Temp Trip Board")),
	STATE_TEMP_TRIP_4_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204203) //
			.text("Temp Trip Board")),
	STATE_TEMP_TRIP_3_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201204) //
			.text("Temp Trip Grid Choke")),
	STATE_TEMP_TRIP_3_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202204) //
			.text("Temp Trip Grid Choke")),
	STATE_TEMP_TRIP_3_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203204) //
			.text("Temp Trip Grid Choke")),
	STATE_TEMP_TRIP_3_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204204) //
			.text("Temp Trip Grid Choke")),
	STATE_TEMP_TRIP_2_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201205) //
			.text("Temp Trip Heatsink Sensor")),
	STATE_TEMP_TRIP_2_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202205) //
			.text("Temp Trip Heatsink Sensor")),
	STATE_TEMP_TRIP_2_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203205) //
			.text("Temp Trip Heatsink Sensor")),
	STATE_TEMP_TRIP_2_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204205) //
			.text("Temp Trip Heatsink Sensor")),
	STATE_TEMP_TRIP_1_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201206) //
			.text("Temp Trip Module Choke")),
	STATE_TEMP_TRIP_1_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202206) //
			.text("Temp Trip Module Choke")),
	STATE_TEMP_TRIP_1_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203206) //
			.text("Temp Trip Module Choke")),
	STATE_TEMP_TRIP_1_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204206) //
			.text("Temp Trip Module Choke")),
	STATE_DESAT_TRIP_1_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201207) //
			.text("IGBT 1 Saturation")),
	STATE_DESAT_TRIP_1_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202207) //
			.text("IGBT 1 Saturation")),
	STATE_DESAT_TRIP_1_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203207) //
			.text("IGBT 1 Saturation")),
	STATE_DESAT_TRIP_1_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204207) //
			.text("IGBT 1 Saturation")),
	STATE_DESAT_TRIP_2_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201208) //
			.text("IGBT 2 Saturation")),
	STATE_DESAT_TRIP_2_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202208) //
			.text("IGBT 2 Saturation")),
	STATE_DESAT_TRIP_2_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203208) //
			.text("IGBT 2 Saturation")),
	STATE_DESAT_TRIP_2_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204208) //
			.text("IGBT 2 Saturation")),
	STATE_DESAT_TRIP_3_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201209) //
			.text("IGBT 3 Saturation")),
	STATE_DESAT_TRIP_3_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202209) //
			.text("IGBT 3 Saturation")),
	STATE_DESAT_TRIP_3_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203209) //
			.text("IGBT 3 Saturation")),
	STATE_DESAT_TRIP_3_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204209) //
			.text("IGBT 3 Saturation")),
	STATE_INTERN_TRIP_1_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20120A) //
			.text("Invalid Dutycycles Phase 1")),
	STATE_INTERN_TRIP_1_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20220A) //
			.text("Invalid Dutycycles Phase 1")),
	STATE_INTERN_TRIP_1_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20320A) //
			.text("Invalid Dutycycles Phase 1")),
	STATE_INTERN_TRIP_1_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20420A) //
			.text("Invalid Dutycycles Phase 1")),
	STATE_INTERN_TRIP_2_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20120B) //
			.text("Invalid Dutycycles Phase 2")),
	STATE_INTERN_TRIP_2_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20220B) //
			.text("Invalid Dutycycles Phase 2")),
	STATE_INTERN_TRIP_2_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20320B) //
			.text("Invalid Dutycycles Phase 2")),
	STATE_INTERN_TRIP_2_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20420B) //
			.text("Invalid Dutycycles Phase 2")),
	STATE_INTERN_TRIP_3_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20120C) //
			.text("Invalid Dutycycles Phase 3")),
	STATE_INTERN_TRIP_3_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20220C) //
			.text("Invalid Dutycycles Phase 3")),
	STATE_INTERN_TRIP_3_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20320C) //
			.text("Invalid Dutycycles Phase 3")),
	STATE_INTERN_TRIP_3_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20420C) //
			.text("Invalid Dutycycles Phase 3")),
	STATE_POWER_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20120D) //
			.text("Power Trip")),
	STATE_POWER_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20220D) //
			.text("Power Trip")),
	STATE_POWER_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20320D) //
			.text("Power Trip")),
	STATE_POWER_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20420D) //
			.text("Power Trip")),
	STATE_OC_TRIP_1_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20120E) //
			.text("Overcurrent Phase 1")),
	STATE_OC_TRIP_1_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20220E) //
			.text("Overcurrent Phase 1")),
	STATE_OC_TRIP_1_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20320E) //
			.text("Overcurrent Phase 1")),
	STATE_OC_TRIP_1_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20420E) //
			.text("Overcurrent Phase 1")),
	STATE_OC_TRIP_2_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20120F) //
			.text("Overcurrent Phase 2")),
	STATE_OC_TRIP_2_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20220F) //
			.text("Overcurrent Phase 2")),
	STATE_OC_TRIP_2_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20320F) //
			.text("Overcurrent Phase 2")),
	STATE_OC_TRIP_2_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20420F) //
			.text("Overcurrent Phase 2")),
	STATE_OC_TRIP_3_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201210) //
			.text("Overcurrent Phase 3")),
	STATE_OC_TRIP_3_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202210) //
			.text("Overcurrent Phase 3")),
	STATE_OC_TRIP_3_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203210) //
			.text("Overcurrent Phase 3")),
	STATE_OC_TRIP_3_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204210) //
			.text("Overcurrent Phase 3")),
	STATE_SWFREQ_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201211) //
			.text("Switching Frequency Error")),
	STATE_SWFREQ_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202211) //
			.text("Switching Frequency Error")),
	STATE_SWFREQ_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203211) //
			.text("Switching Frequency Error")),
	STATE_SWFREQ_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204211) //
			.text("Switching Frequency Error")),
	STATE_RMS_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201212) //
			.text("RMS Current Error")),
	STATE_RMS_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202212) //
			.text("RMS Current Error")),
	STATE_RMS_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203212) //
			.text("RMS Current Error")),
	STATE_RMS_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204212) //
			.text("RMS Current Error")),
	STATE_SOFTWARE_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201213) //
			.text("Software Error")),
	STATE_SOFTWARE_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202213) //
			.text("Software Error")),
	STATE_SOFTWARE_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203213) //
			.text("Software Error")),
	STATE_SOFTWARE_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204213) //
			.text("Software Error")),
	STATE_V1_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201214) //
			.text("Grid Overcurrent Phase 3")),
	STATE_V1_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202214) //
			.text("Grid Overcurrent Phase 3")),
	STATE_V1_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203214) //
			.text("Grid Overcurrent Phase 3")),
	STATE_V1_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204214) //
			.text("Grid Overcurrent Phase 3")),
	STATE_V2_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201215) //
			.text("Grid Overcurrent Phase 2")),
	STATE_V2_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202215) //
			.text("Grid Overcurrent Phase 2")),
	STATE_V2_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203215) //
			.text("Grid Overcurrent Phase 2")),
	STATE_V2_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204215) //
			.text("Grid Overcurrent Phase 2")),
	STATE_V3_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201216) //
			.text("Grid Overcurrent Phase 1")),
	STATE_V3_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202216) //
			.text("Grid Overcurrent Phase 1")),
	STATE_V3_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203216) //
			.text("Grid Overcurrent Phase 1")),
	STATE_V3_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204216) //
			.text("Grid Overcurrent Phase 1")),
	STATE_V4_TRIP_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201217) //
			.text("24 V Fault")),
	STATE_V4_TRIP_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202217) //
			.text("24 V Fault")),
	STATE_V4_TRIP_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203217) //
			.text("24 V Fault")),
	STATE_V4_TRIP_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204217) //
			.text("24 V Fault")),
	STATE_V5_TRIP_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201218) //
			.text("Module Overcurrent Phase A")),
	STATE_V5_TRIP_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202218) //
			.text("Module Overcurrent Phase A")),
	STATE_V5_TRIP_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203218) //
			.text("Module Overcurrent Phase A")),
	STATE_V5_TRIP_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204218) //
			.text("Module Overcurrent Phase A")),
	STATE_V6_TRIP_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201219) //
			.text("Module Overcurrent Phase B")),
	STATE_V6_TRIP_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202219) //
			.text("Module Overcurrent Phase B")),
	STATE_V6_TRIP_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203219) //
			.text("Module Overcurrent Phase B")),
	STATE_V6_TRIP_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204219) //
			.text("Module Overcurrent Phase B")),
	STATE_V7_TRIP_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20121A) //
			.text("Module Overcurrent Phase C")),
	STATE_V7_TRIP_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20221A) //
			.text("Module Overcurrent Phase C")),
	STATE_V7_TRIP_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20321A) //
			.text("Module Overcurrent Phase C")),
	STATE_V7_TRIP_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20421A) //
			.text("Module Overcurrent Phase C")),
	STATE_UDC_P_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20121B) //
			.text("DC-Link Positive Voltage Fault")),
	STATE_UDC_P_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20221B) //
			.text("DC-Link Positive Voltage Fault")),
	STATE_UDC_P_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20321B) //
			.text("DC-Link Positive Voltage Fault")),
	STATE_UDC_P_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20421B) //
			.text("DC-Link Positive Voltage Fault")),
	STATE_UDC_N_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20121C) //
			.text("DC-Link Negative Voltage Fault")),
	STATE_UDC_N_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20221C) //
			.text("DC-Link Negative Voltage Fault")),
	STATE_UDC_N_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20321C) //
			.text("DC-Link Negative Voltage Fault")),
	STATE_UDC_N_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20421C) //
			.text("DC-Link Negative Voltage Fault")),
	STATE_UDC_DIFF_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20121D) //
			.text("DC-Link Voltage Imbalance")),
	STATE_UDC_DIFF_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20221D) //
			.text("DC-Link Voltage Imbalance")),
	STATE_UDC_DIFF_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20321D) //
			.text("DC-Link Voltage Imbalance")),
	STATE_UDC_DIFF_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20421D) //
			.text("DC-Link Voltage Imbalance")),
	STATE_TACHO_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20121E) //
			.text("Fan Error")),
	STATE_TACHO_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20221E) //
			.text("Fan Error")),
	STATE_TACHO_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20321E) //
			.text("Fan Error")),
	STATE_TACHO_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20421E) //
			.text("Fan Error")),
	STATE_CURRENT_LOOP_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x201100) //
			.text("CurrentLoop Open")),
	STATE_CURRENT_LOOP_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x202100) //
			.text("CurrentLoop Open")),
	STATE_CURRENT_LOOP_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x203100) //
			.text("CurrentLoop Open")),
	STATE_CURRENT_LOOP_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x204100) //
			.text("CurrentLoop Open")),
	STATE_WATCHDOG_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201101) //
			.text("Watchdog Timeout")),
	STATE_WATCHDOG_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202101) //
			.text("Watchdog Timeout")),
	STATE_WATCHDOG_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203101) //
			.text("Watchdog Timeout")),
	STATE_WATCHDOG_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204101) //
			.text("Watchdog Timeout")),
	STATE_SLAVE_UNREACHABLE_CCU_SLAVE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x201102) //
			.text("Slave unreachable")),
	STATE_SLAVE_UNREACHABLE_CCU_SLAVE_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x202102) //
			.text("Slave unreachable")),
	STATE_SLAVE_UNREACHABLE_CCU_SLAVE_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x203102) //
			.text("Slave unreachable")),
	STATE_SLAVE_UNREACHABLE_CCU_SLAVE_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x204102) //
			.text("Slave unreachable")),
	STATE_SLAVE_ERROR_CCU_SLAVE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201103) //
			.text("Slave error")),
	STATE_SLAVE_ERROR_CCU_SLAVE_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202103) //
			.text("Slave error")),
	STATE_SLAVE_ERROR_CCU_SLAVE_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203103) //
			.text("Slave error")),
	STATE_SLAVE_ERROR_CCU_SLAVE_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204103) //
			.text("Slave error")),
	STATE_TEMP_TRIP_IGBT_3_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401200) //
			.text("Bit 0")),
	STATE_TEMP_TRIP_IGBT_3_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402200) //
			.text("Bit 0")),
	STATE_TEMP_TRIP_IGBT_3_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403200) //
			.text("Bit 0")),
	STATE_TEMP_TRIP_IGBT_3_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404200) //
			.text("Bit 0")),
	STATE_TEMP_TRIP_IGBT_2_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401201) //
			.text("Bit 1")),
	STATE_TEMP_TRIP_IGBT_2_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402201) //
			.text("Bit 1")),
	STATE_TEMP_TRIP_IGBT_2_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403201) //
			.text("Bit 1")),
	STATE_TEMP_TRIP_IGBT_2_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404201) //
			.text("Bit 1")),
	STATE_TEMP_TRIP_IGBT_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401202) //
			.text("Temp Trip IGBT")),
	STATE_TEMP_TRIP_IGBT_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402202) //
			.text("Temp Trip IGBT")),
	STATE_TEMP_TRIP_IGBT_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403202) //
			.text("Temp Trip IGBT")),
	STATE_TEMP_TRIP_IGBT_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404202) //
			.text("Temp Trip IGBT")),
	STATE_TEMP_TRIP_MCU_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401203) //
			.text("Temp Trip Board")),
	STATE_TEMP_TRIP_MCU_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402203) //
			.text("Temp Trip Board")),
	STATE_TEMP_TRIP_MCU_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403203) //
			.text("Temp Trip Board")),
	STATE_TEMP_TRIP_MCU_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404203) //
			.text("Temp Trip Board")),
	STATE_TEMP_TRIP_C1_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401204) //
			.text("Temp Trip Choke 1")),
	STATE_TEMP_TRIP_C1_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402204) //
			.text("Temp Trip Choke 1")),
	STATE_TEMP_TRIP_C1_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403204) //
			.text("Temp Trip Choke 1")),
	STATE_TEMP_TRIP_C1_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404204) //
			.text("Temp Trip Choke 1")),
	STATE_TEMP_TRIP_C2_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401205) //
			.text("Temp Trip Choke 2")),
	STATE_TEMP_TRIP_C2_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402205) //
			.text("Temp Trip Choke 2")),
	STATE_TEMP_TRIP_C2_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403205) //
			.text("Temp Trip Choke 2")),
	STATE_TEMP_TRIP_C2_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404205) //
			.text("Temp Trip Choke 2")),
	STATE_TEMP_TRIP_C3_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401206) //
			.text("Temp Trip Choke 3")),
	STATE_TEMP_TRIP_C3_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402206) //
			.text("Temp Trip Choke 3")),
	STATE_TEMP_TRIP_C3_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403206) //
			.text("Temp Trip Choke 3")),
	STATE_TEMP_TRIP_C3_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404206) //
			.text("Temp Trip Choke 3")),
	STATE_DESAT_TRIP_1_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401207) //
			.text("IGBT A Error")),
	STATE_DESAT_TRIP_1_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402207) //
			.text("IGBT A Error")),
	STATE_DESAT_TRIP_1_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403207) //
			.text("IGBT A Error")),
	STATE_DESAT_TRIP_1_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404207) //
			.text("IGBT A Error")),
	STATE_DESAT_TRIP_2_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401208) //
			.text("IGBT B Error")),
	STATE_DESAT_TRIP_2_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402208) //
			.text("IGBT B Error")),
	STATE_DESAT_TRIP_2_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403208) //
			.text("IGBT B Error")),
	STATE_DESAT_TRIP_2_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404208) //
			.text("IGBT B Error")),
	STATE_DESAT_TRIP_3_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401209) //
			.text("IGBT C Error")),
	STATE_DESAT_TRIP_3_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402209) //
			.text("IGBT C Error")),
	STATE_DESAT_TRIP_3_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403209) //
			.text("IGBT C Error")),
	STATE_DESAT_TRIP_3_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404209) //
			.text("IGBT C Error")),
	STATE_INTERN_TRIP_1_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40120A) //
			.text("Modulation Error P1")),
	STATE_INTERN_TRIP_1_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40220A) //
			.text("Modulation Error P1")),
	STATE_INTERN_TRIP_1_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40320A) //
			.text("Modulation Error P1")),
	STATE_INTERN_TRIP_1_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40420A) //
			.text("Modulation Error P1")),
	STATE_INTERN_TRIP_2_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40120B) //
			.text("Modulation Error P2")),
	STATE_INTERN_TRIP_2_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40220B) //
			.text("Modulation Error P2")),
	STATE_INTERN_TRIP_2_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40320B) //
			.text("Modulation Error P2")),
	STATE_INTERN_TRIP_2_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40420B) //
			.text("Modulation Error P2")),
	STATE_INTERN_TRIP_3_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40120C) //
			.text("Modulation Error P3")),
	STATE_INTERN_TRIP_3_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40220C) //
			.text("Modulation Error P3")),
	STATE_INTERN_TRIP_3_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40320C) //
			.text("Modulation Error P3")),
	STATE_INTERN_TRIP_3_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40420C) //
			.text("Modulation Error P3")),
	STATE_POWER_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40120D) //
			.text("Power Trip")),
	STATE_POWER_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40220D) //
			.text("Power Trip")),
	STATE_POWER_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40320D) //
			.text("Power Trip")),
	STATE_POWER_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40420D) //
			.text("Power Trip")),
	STATE_OC_TRIP_1_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40120E) //
			.text("Overcurrent Protection Phase A")),
	STATE_OC_TRIP_1_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40220E) //
			.text("Overcurrent Protection Phase A")),
	STATE_OC_TRIP_1_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40320E) //
			.text("Overcurrent Protection Phase A")),
	STATE_OC_TRIP_1_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40420E) //
			.text("Overcurrent Protection Phase A")),
	STATE_OC_TRIP_2_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40120F) //
			.text("Overcurrent Protection Phase B")),
	STATE_OC_TRIP_2_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40220F) //
			.text("Overcurrent Protection Phase B")),
	STATE_OC_TRIP_2_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40320F) //
			.text("Overcurrent Protection Phase B")),
	STATE_OC_TRIP_2_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40420F) //
			.text("Overcurrent Protection Phase B")),
	STATE_OC_TRIP_3_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401210) //
			.text("Overcurrent Protection Phase C")),
	STATE_OC_TRIP_3_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402210) //
			.text("Overcurrent Protection Phase C")),
	STATE_OC_TRIP_3_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403210) //
			.text("Overcurrent Protection Phase C")),
	STATE_OC_TRIP_3_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404210) //
			.text("Overcurrent Protection Phase C")),
	STATE_SWFREQ_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401211) //
			.text("Switching Frequency Error")),
	STATE_SWFREQ_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402211) //
			.text("Switching Frequency Error")),
	STATE_SWFREQ_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403211) //
			.text("Switching Frequency Error")),
	STATE_SWFREQ_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404211) //
			.text("Switching Frequency Error")),
	STATE_RMS_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401212) //
			.text("RMS Current Error")),
	STATE_RMS_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402212) //
			.text("RMS Current Error")),
	STATE_RMS_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403212) //
			.text("RMS Current Error")),
	STATE_RMS_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404212) //
			.text("RMS Current Error")),
	STATE_SOFTWARE_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401213) //
			.text("Software Error")),
	STATE_SOFTWARE_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402213) //
			.text("Software Error")),
	STATE_SOFTWARE_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403213) //
			.text("Software Error")),
	STATE_SOFTWARE_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404213) //
			.text("Software Error")),
	STATE_V1_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401214) //
			.text("Bit 20")),
	STATE_V1_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402214) //
			.text("Bit 20")),
	STATE_V1_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403214) //
			.text("Bit 20")),
	STATE_V1_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404214) //
			.text("Bit 20")),
	STATE_V2_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401215) //
			.text("Bit 21")),
	STATE_V2_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402215) //
			.text("Bit 21")),
	STATE_V2_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403215) //
			.text("Bit 21")),
	STATE_V2_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404215) //
			.text("Bit 21")),
	STATE_V3_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401216) //
			.text("Bit 22")),
	STATE_V3_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402216) //
			.text("Bit 22")),
	STATE_V3_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403216) //
			.text("Bit 22")),
	STATE_V3_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404216) //
			.text("Bit 22")),
	STATE_V24_TRIP_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401217) //
			.text("24 V Fault")),
	STATE_V24_TRIP_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402217) //
			.text("24 V Fault")),
	STATE_V24_TRIP_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403217) //
			.text("24 V Fault")),
	STATE_V24_TRIP_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404217) //
			.text("24 V Fault")),
	STATE_OCA_TRIP_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401218) //
			.text("Overcurrent Phase A")),
	STATE_OCA_TRIP_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402218) //
			.text("Overcurrent Phase A")),
	STATE_OCA_TRIP_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403218) //
			.text("Overcurrent Phase A")),
	STATE_OCA_TRIP_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404218) //
			.text("Overcurrent Phase A")),
	STATE_OCB_TRIP_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401219) //
			.text("Overcurrent Phase B")),
	STATE_OCB_TRIP_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402219) //
			.text("Overcurrent Phase B")),
	STATE_OCB_TRIP_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403219) //
			.text("Overcurrent Phase B")),
	STATE_OCB_TRIP_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404219) //
			.text("Overcurrent Phase B")),
	STATE_OCC_TRIP_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40121A) //
			.text("Overcurrent Phase C")),
	STATE_OCC_TRIP_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40221A) //
			.text("Overcurrent Phase C")),
	STATE_OCC_TRIP_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40321A) //
			.text("Overcurrent Phase C")),
	STATE_OCC_TRIP_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40421A) //
			.text("Overcurrent Phase C")),
	STATE_UB1_TRIP_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40121B) //
			.text("DC String 1 Overvoltage")),
	STATE_UB1_TRIP_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40221B) //
			.text("DC String 1 Overvoltage")),
	STATE_UB1_TRIP_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40321B) //
			.text("DC String 1 Overvoltage")),
	STATE_UB1_TRIP_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40421B) //
			.text("DC String 1 Overvoltage")),
	STATE_UB2_TRIP_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40121C) //
			.text("DC String 2 Overvoltage")),
	STATE_UB2_TRIP_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40221C) //
			.text("DC String 2 Overvoltage")),
	STATE_UB2_TRIP_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40321C) //
			.text("DC String 2 Overvoltage")),
	STATE_UB2_TRIP_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40421C) //
			.text("DC String 2 Overvoltage")),
	STATE_UB3_TRIP_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40121D) //
			.text("DC String 3 Overvoltage")),
	STATE_UB3_TRIP_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40221D) //
			.text("DC String 3 Overvoltage")),
	STATE_UB3_TRIP_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40321D) //
			.text("DC String 3 Overvoltage")),
	STATE_UB3_TRIP_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40421D) //
			.text("DC String 3 Overvoltage")),
	STATE_TACHO_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40121E) //
			.text("Fan Error")),
	STATE_TACHO_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40221E) //
			.text("Fan Error")),
	STATE_TACHO_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40321E) //
			.text("Fan Error")),
	STATE_TACHO_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40421E) //
			.text("Fan Error")),
	STATE_UZK_TRIP_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40121F) //
			.text("DC-Link Overvoltage Fault")),
	STATE_UZK_TRIP_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40221F) //
			.text("DC-Link Overvoltage Fault")),
	STATE_UZK_TRIP_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40321F) //
			.text("DC-Link Overvoltage Fault")),
	STATE_UZK_TRIP_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x40421F) //
			.text("DC-Link Overvoltage Fault")),
	STATE_CURRENT_LOOP_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x401100) //
			.text("CurrentLoop Open")),
	STATE_CURRENT_LOOP_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x402100) //
			.text("CurrentLoop Open")),
	STATE_CURRENT_LOOP_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x403100) //
			.text("CurrentLoop Open")),
	STATE_CURRENT_LOOP_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x404100) //
			.text("CurrentLoop Open")),
	STATE_WATCHDOG_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401101) //
			.text("Watchdog Timeout")),
	STATE_WATCHDOG_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402101) //
			.text("Watchdog Timeout")),
	STATE_WATCHDOG_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403101) //
			.text("Watchdog Timeout")),
	STATE_WATCHDOG_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404101) //
			.text("Watchdog Timeout")),
	STATE_TEMP_TRIP_IGBT_1_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201400) //
			.text("(XT) Temp Trip IGBT 1")),
	STATE_TEMP_TRIP_IGBT_1_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202400) //
			.text("(XT) Temp Trip IGBT 1")),
	STATE_TEMP_TRIP_IGBT_1_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203400) //
			.text("(XT) Temp Trip IGBT 1")),
	STATE_TEMP_TRIP_IGBT_1_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204400) //
			.text("(XT) Temp Trip IGBT 1")),
	STATE_TEMP_TRIP_IGBT_2_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201401) //
			.text("(XT) Temp Trip IGBT 2")),
	STATE_TEMP_TRIP_IGBT_2_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202401) //
			.text("(XT) Temp Trip IGBT 2")),
	STATE_TEMP_TRIP_IGBT_2_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203401) //
			.text("(XT) Temp Trip IGBT 2")),
	STATE_TEMP_TRIP_IGBT_2_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204401) //
			.text("(XT) Temp Trip IGBT 2")),
	STATE_TEMP_TRIP_IGBT_3_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201402) //
			.text("(XT) Temp Trip IGBT 3")),
	STATE_TEMP_TRIP_IGBT_3_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202402) //
			.text("(XT) Temp Trip IGBT 3")),
	STATE_TEMP_TRIP_IGBT_3_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203402) //
			.text("(XT) Temp Trip IGBT 3")),
	STATE_TEMP_TRIP_IGBT_3_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204402) //
			.text("(XT) Temp Trip IGBT 3")),
	STATE_TEMP_TRIP_FILTERCHOKE_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x201403) //
			.text("(XT) Temp Trip Filter Choke")),
	STATE_TEMP_TRIP_FILTERCHOKE_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x202403) //
			.text("(XT) Temp Trip Filter Choke")),
	STATE_TEMP_TRIP_FILTERCHOKE_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x203403) //
			.text("(XT) Temp Trip Filter Choke")),
	STATE_TEMP_TRIP_FILTERCHOKE_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x204403) //
			.text("(XT) Temp Trip Filter Choke")),
	STATE_TEMP_TRIP_GRIDCHOKE_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x201404) //
			.text("(XT) Temp Trip Grid Choke")),
	STATE_TEMP_TRIP_GRIDCHOKE_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x202404) //
			.text("(XT) Temp Trip Grid Choke")),
	STATE_TEMP_TRIP_GRIDCHOKE_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x203404) //
			.text("(XT) Temp Trip Grid Choke")),
	STATE_TEMP_TRIP_GRIDCHOKE_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x204404) //
			.text("(XT) Temp Trip Grid Choke")),
	STATE_OUTPUT_PEAK_TRIP_1_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201405) //
			.text("(XT) Output Current Peak Trip Phase 1")),
	STATE_OUTPUT_PEAK_TRIP_1_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202405) //
			.text("(XT) Output Current Peak Trip Phase 1")),
	STATE_OUTPUT_PEAK_TRIP_1_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203405) //
			.text("(XT) Output Current Peak Trip Phase 1")),
	STATE_OUTPUT_PEAK_TRIP_1_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204405) //
			.text("(XT) Output Current Peak Trip Phase 1")),
	STATE_OUTPUT_PEAK_TRIP_2_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201406) //
			.text("(XT) Output Current Peak Trip Phase 2")),
	STATE_OUTPUT_PEAK_TRIP_2_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202406) //
			.text("(XT) Output Current Peak Trip Phase 2")),
	STATE_OUTPUT_PEAK_TRIP_2_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203406) //
			.text("(XT) Output Current Peak Trip Phase 2")),
	STATE_OUTPUT_PEAK_TRIP_2_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204406) //
			.text("(XT) Output Current Peak Trip Phase 2")),
	STATE_OUTPUT_PEAK_TRIP_3_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201407) //
			.text("(XT) Output Current Peak Trip Phase 3")),
	STATE_OUTPUT_PEAK_TRIP_3_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202407) //
			.text("(XT) Output Current Peak Trip Phase 3")),
	STATE_OUTPUT_PEAK_TRIP_3_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203407) //
			.text("(XT) Output Current Peak Trip Phase 3")),
	STATE_OUTPUT_PEAK_TRIP_3_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204407) //
			.text("(XT) Output Current Peak Trip Phase 3")),
	STATE_MODULE_PEAK_TRIP_1_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201408) //
			.text("(XT) Module Current Peak Trip Phase 1")),
	STATE_MODULE_PEAK_TRIP_1_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202408) //
			.text("(XT) Module Current Peak Trip Phase 1")),
	STATE_MODULE_PEAK_TRIP_1_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203408) //
			.text("(XT) Module Current Peak Trip Phase 1")),
	STATE_MODULE_PEAK_TRIP_1_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204408) //
			.text("(XT) Module Current Peak Trip Phase 1")),
	STATE_MODULE_PEAK_TRIP_2_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201409) //
			.text("(XT) Module Current Peak Trip Phase 2")),
	STATE_MODULE_PEAK_TRIP_2_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202409) //
			.text("(XT) Module Current Peak Trip Phase 2")),
	STATE_MODULE_PEAK_TRIP_2_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203409) //
			.text("(XT) Module Current Peak Trip Phase 2")),
	STATE_MODULE_PEAK_TRIP_2_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204409) //
			.text("(XT) Module Current Peak Trip Phase 2")),
	STATE_MODULE_PEAK_TRIP_3_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x20140A) //
			.text("(XT) Module Current Peak Trip Phase 3")),
	STATE_MODULE_PEAK_TRIP_3_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x20240A) //
			.text("(XT) Module Current Peak Trip Phase 3")),
	STATE_MODULE_PEAK_TRIP_3_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x20340A) //
			.text("(XT) Module Current Peak Trip Phase 3")),
	STATE_MODULE_PEAK_TRIP_3_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x20440A) //
			.text("(XT) Module Current Peak Trip Phase 3")),
	STATE_FILTER_RMS_TRIP_1_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20140B) //
			.text("(XT) Filter Current RMS Trip Phase 1")),
	STATE_FILTER_RMS_TRIP_1_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20240B) //
			.text("(XT) Filter Current RMS Trip Phase 1")),
	STATE_FILTER_RMS_TRIP_1_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20340B) //
			.text("(XT) Filter Current RMS Trip Phase 1")),
	STATE_FILTER_RMS_TRIP_1_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20440B) //
			.text("(XT) Filter Current RMS Trip Phase 1")),
	STATE_FILTER_RMS_TRIP_2_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20140C) //
			.text("(XT) Filter Current RMS Trip Phase 2")),
	STATE_FILTER_RMS_TRIP_2_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20240C) //
			.text("(XT) Filter Current RMS Trip Phase 2")),
	STATE_FILTER_RMS_TRIP_2_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20340C) //
			.text("(XT) Filter Current RMS Trip Phase 2")),
	STATE_FILTER_RMS_TRIP_2_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20440C) //
			.text("(XT) Filter Current RMS Trip Phase 2")),
	STATE_FILTER_RMS_TRIP_3_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20140D) //
			.text("(XT) Filter Current RMS Trip Phase 3")),
	STATE_FILTER_RMS_TRIP_3_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20240D) //
			.text("(XT) Filter Current RMS Trip Phase 3")),
	STATE_FILTER_RMS_TRIP_3_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20340D) //
			.text("(XT) Filter Current RMS Trip Phase 3")),
	STATE_FILTER_RMS_TRIP_3_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20440D) //
			.text("(XT) Filter Current RMS Trip Phase 3")),
	STATE_FILTER_SLOWRMS_TRIP_1_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20140E) //
			.text("(XT) Filter Current Slow RMS Trip Phase 1")),
	STATE_FILTER_SLOWRMS_TRIP_1_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20240E) //
			.text("(XT) Filter Current Slow RMS Trip Phase 1")),
	STATE_FILTER_SLOWRMS_TRIP_1_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20340E) //
			.text("(XT) Filter Current Slow RMS Trip Phase 1")),
	STATE_FILTER_SLOWRMS_TRIP_1_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20440E) //
			.text("(XT) Filter Current Slow RMS Trip Phase 1")),
	STATE_FILTER_SLOWRMS_TRIP_2_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20140F) //
			.text("(XT) Filter Current Slow RMS Trip Phase 2")),
	STATE_FILTER_SLOWRMS_TRIP_2_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20240F) //
			.text("(XT) Filter Current Slow RMS Trip Phase 2")),
	STATE_FILTER_SLOWRMS_TRIP_2_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20340F) //
			.text("(XT) Filter Current Slow RMS Trip Phase 2")),
	STATE_FILTER_SLOWRMS_TRIP_2_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20440F) //
			.text("(XT) Filter Current Slow RMS Trip Phase 2")),
	STATE_FILTER_SLOWRMS_TRIP_3_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x201410) //
			.text("(XT) Filter Current Slow RMS Trip Phase 3")),
	STATE_FILTER_SLOWRMS_TRIP_3_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x202410) //
			.text("(XT) Filter Current Slow RMS Trip Phase 3")),
	STATE_FILTER_SLOWRMS_TRIP_3_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x203410) //
			.text("(XT) Filter Current Slow RMS Trip Phase 3")),
	STATE_FILTER_SLOWRMS_TRIP_3_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x204410) //
			.text("(XT) Filter Current Slow RMS Trip Phase 3")),
	STATE_OUTPUT_RMS_TRIP_1_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x201411) //
			.text("(XT) Output Current RMS Trip Phase 1")),
	STATE_OUTPUT_RMS_TRIP_1_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x202411) //
			.text("(XT) Output Current RMS Trip Phase 1")),
	STATE_OUTPUT_RMS_TRIP_1_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x203411) //
			.text("(XT) Output Current RMS Trip Phase 1")),
	STATE_OUTPUT_RMS_TRIP_1_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x204411) //
			.text("(XT) Output Current RMS Trip Phase 1")),
	STATE_OUTPUT_RMS_TRIP_2_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x201412) //
			.text("(XT) Output Current RMS Trip Phase 2")),
	STATE_OUTPUT_RMS_TRIP_2_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x202412) //
			.text("(XT) Output Current RMS Trip Phase 2")),
	STATE_OUTPUT_RMS_TRIP_2_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x203412) //
			.text("(XT) Output Current RMS Trip Phase 2")),
	STATE_OUTPUT_RMS_TRIP_2_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x204412) //
			.text("(XT) Output Current RMS Trip Phase 2")),
	STATE_OUTPUT_RMS_TRIP_3_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x201413) //
			.text("(XT) Output Current RMS Trip Phase 3")),
	STATE_OUTPUT_RMS_TRIP_3_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x202413) //
			.text("(XT) Output Current RMS Trip Phase 3")),
	STATE_OUTPUT_RMS_TRIP_3_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x203413) //
			.text("(XT) Output Current RMS Trip Phase 3")),
	STATE_OUTPUT_RMS_TRIP_3_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x204413) //
			.text("(XT) Output Current RMS Trip Phase 3")),
	STATE_MODULE_FRD_FILTER_C_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x201414).text("(XT) Fast Resonance Detection (FRD-F)")),
	STATE_MODULE_FRD_FILTER_C_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x202414).text("(XT) Fast Resonance Detection (FRD-F)")),
	STATE_MODULE_FRD_FILTER_C_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x203414).text("(XT) Fast Resonance Detection (FRD-F)")),
	STATE_MODULE_FRD_FILTER_C_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x204414).text("(XT) Fast Resonance Detection (FRD-F)")),
	STATE_MODULE_FRD_OUTPUTPEAK_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x201415).text("(XT) Fast Resonance Detection (FRD-P)")),
	STATE_MODULE_FRD_OUTPUTPEAK_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x202415).text("(XT) Fast Resonance Detection (FRD-P)")),
	STATE_MODULE_FRD_OUTPUTPEAK_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x203415).text("(XT) Fast Resonance Detection (FRD-P)")),
	STATE_MODULE_FRD_OUTPUTPEAK_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x204415).text("(XT) Fast Resonance Detection (FRD-P)")),
	STATE_SIA_RUNNING_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x201416) //
			.text("(XT) System Identification Algorithm (SIA) running...")),
	STATE_SIA_RUNNING_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x202416) //
			.text("(XT) System Identification Algorithm (SIA) running...")),
	STATE_SIA_RUNNING_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x203416) //
			.text("(XT) System Identification Algorithm (SIA) running...")),
	STATE_SIA_RUNNING_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x204416) //
			.text("(XT) System Identification Algorithm (SIA) running...")),
	STATE_SIA_WARNING_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x201417) //
			.text("(XT) SIA Warning")),
	STATE_SIA_WARNING_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x202417) //
			.text("(XT) SIA Warning")),
	STATE_SIA_WARNING_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x203417) //
			.text("(XT) SIA Warning")),
	STATE_SIA_WARNING_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x204417) //
			.text("(XT) SIA Warning")),
	STATE_NEUTRAL_RMS_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201418) //
			.text("(XT) Neutral Current RMS Trip")),
	STATE_NEUTRAL_RMS_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202418) //
			.text("(XT) Neutral Current RMS Trip")),
	STATE_NEUTRAL_RMS_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203418) //
			.text("(XT) Neutral Current RMS Trip")),
	STATE_NEUTRAL_RMS_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204418) //
			.text("(XT) Neutral Current RMS Trip")),
	STATE_SUSPECT_HARMONICS_DETECTED_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x201419) //
			.text("(XT) Suspect Harmonics marked")),
	STATE_SUSPECT_HARMONICS_DETECTED_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x202419) //
			.text("(XT) Suspect Harmonics marked")),
	STATE_SUSPECT_HARMONICS_DETECTED_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x203419) //
			.text("(XT) Suspect Harmonics marked")),
	STATE_SUSPECT_HARMONICS_DETECTED_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x204419) //
			.text("(XT) Suspect Harmonics marked")),
	STATE_CRITICAL_HARMONICS_DETECTED_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x20141A).text("(XT) Compensation of critical harmonics blocked")),
	STATE_CRITICAL_HARMONICS_DETECTED_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x20241A).text("(XT) Compensation of critical harmonics blocked")),
	STATE_CRITICAL_HARMONICS_DETECTED_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x20341A).text("(XT) Compensation of critical harmonics blocked")),
	STATE_CRITICAL_HARMONICS_DETECTED_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x20441A).text("(XT) Compensation of critical harmonics blocked")),
	STATE_UDC_P_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201800) //
			.text("(XT) DC-Link Positive Voltage Fault")),
	STATE_UDC_P_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202800) //
			.text("(XT) DC-Link Positive Voltage Fault")),
	STATE_UDC_P_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203800) //
			.text("(XT) DC-Link Positive Voltage Fault")),
	STATE_UDC_P_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204800) //
			.text("(XT) DC-Link Positive Voltage Fault")),
	STATE_UDC_N_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201801) //
			.text("(XT) DC-Link Negative Voltage Fault")),
	STATE_UDC_N_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202801) //
			.text("(XT) DC-Link Negative Voltage Fault")),
	STATE_UDC_N_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203801) //
			.text("(XT) DC-Link Negative Voltage Fault")),
	STATE_UDC_N_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204801) //
			.text("(XT) DC-Link Negative Voltage Fault")),
	STATE_UDC_DIFF_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201802) //
			.text("(XT) DC-Link Voltage Imbalance")),
	STATE_UDC_DIFF_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202802) //
			.text("(XT) DC-Link Voltage Imbalance")),
	STATE_UDC_DIFF_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203802) //
			.text("(XT) DC-Link Voltage Imbalance")),
	STATE_UDC_DIFF_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204802) //
			.text("(XT) DC-Link Voltage Imbalance")),
	STATE_UDC_P_PRECHG_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x201803).text("(XT) DC-Link Precharge Positive Voltage Trip")),
	STATE_UDC_P_PRECHG_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x202803).text("(XT) DC-Link Precharge Positive Voltage Trip")),
	STATE_UDC_P_PRECHG_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x203803).text("(XT) DC-Link Precharge Positive Voltage Trip")),
	STATE_UDC_P_PRECHG_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x204803).text("(XT) DC-Link Precharge Positive Voltage Trip")),
	STATE_UDC_N_PRECHG_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x201804).text("(XT) DC-Link Precharge Negative Voltage Trip")),
	STATE_UDC_N_PRECHG_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x202804).text("(XT) DC-Link Precharge Negative Voltage Trip")),
	STATE_UDC_N_PRECHG_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x203804).text("(XT) DC-Link Precharge Negative Voltage Trip")),
	STATE_UDC_N_PRECHG_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x204804).text("(XT) DC-Link Precharge Negative Voltage Trip")),
	STATE_UDC_P_PRECHG_5_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x201805).text("(XT) DC-Link Precharge Positive Undervoltage Trip")),
	STATE_UDC_P_PRECHG_5_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x202805).text("(XT) DC-Link Precharge Positive Undervoltage Trip")),
	STATE_UDC_P_PRECHG_5_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x203805).text("(XT) DC-Link Precharge Positive Undervoltage Trip")),
	STATE_UDC_P_PRECHG_5_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x204805).text("(XT) DC-Link Precharge Positive Undervoltage Trip")),
	STATE_UDC_N_PRECHG_5_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x201806).text("(XT) DC-Link Precharge Negative Undervoltage Trip")),
	STATE_UDC_N_PRECHG_5_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x202806).text("(XT) DC-Link Precharge Negative Undervoltage Trip")),
	STATE_UDC_N_PRECHG_5_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x203806).text("(XT) DC-Link Precharge Negative Undervoltage Trip")),
	STATE_UDC_N_PRECHG_5_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x204806).text("(XT) DC-Link Precharge Negative Undervoltage Trip")),
	STATE_UDC_P_PRECHG_MAX_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x201807).text("(XT) DC-Link Precharge Positive Overvoltage Trip")),
	STATE_UDC_P_PRECHG_MAX_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x202807).text("(XT) DC-Link Precharge Positive Overvoltage Trip")),
	STATE_UDC_P_PRECHG_MAX_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x203807).text("(XT) DC-Link Precharge Positive Overvoltage Trip")),
	STATE_UDC_P_PRECHG_MAX_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x204807).text("(XT) DC-Link Precharge Positive Overvoltage Trip")),
	STATE_UDC_N_PRECHG_MAX_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x201808).text("(XT) DC-Link Precharge Negative Overvoltage Trip")),
	STATE_UDC_N_PRECHG_MAX_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x202808).text("(XT) DC-Link Precharge Negative Overvoltage Trip")),
	STATE_UDC_N_PRECHG_MAX_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x203808).text("(XT) DC-Link Precharge Negative Overvoltage Trip")),
	STATE_UDC_N_PRECHG_MAX_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x204808).text("(XT) DC-Link Precharge Negative Overvoltage Trip")),
	STATE_TEMP_TRIP_IGBT_1_IPU_1_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201400) //
			.text("(XT) Temp Trip IGBT 1")),
	STATE_TEMP_TRIP_IGBT_1_IPU_2_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202400) //
			.text("(XT) Temp Trip IGBT 1")),
	STATE_TEMP_TRIP_IGBT_1_IPU_3_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203400) //
			.text("(XT) Temp Trip IGBT 1")),
	STATE_TEMP_TRIP_IGBT_1_IPU_4_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204400) //
			.text("(XT) Temp Trip IGBT 1")),
	STATE_TEMP_TRIP_IGBT_2_IPU_1_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201401) //
			.text("(XT) Temp Trip IGBT 2")),
	STATE_TEMP_TRIP_IGBT_2_IPU_2_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202401) //
			.text("(XT) Temp Trip IGBT 2")),
	STATE_TEMP_TRIP_IGBT_2_IPU_3_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203401) //
			.text("(XT) Temp Trip IGBT 2")),
	STATE_TEMP_TRIP_IGBT_2_IPU_4_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204401) //
			.text("(XT) Temp Trip IGBT 2")),
	STATE_TEMP_TRIP_IGBT_3_IPU_1_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201402) //
			.text("(XT) Temp Trip IGBT 3")),
	STATE_TEMP_TRIP_IGBT_3_IPU_2_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202402) //
			.text("(XT) Temp Trip IGBT 3")),
	STATE_TEMP_TRIP_IGBT_3_IPU_3_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203402) //
			.text("(XT) Temp Trip IGBT 3")),
	STATE_TEMP_TRIP_IGBT_3_IPU_4_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204402) //
			.text("(XT) Temp Trip IGBT 3")),
	STATE_TEMP_TRIP_FILTERCHOKE_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x201403) //
			.text("(XT) Temp Trip Filter Choke")),
	STATE_TEMP_TRIP_FILTERCHOKE_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x202403) //
			.text("(XT) Temp Trip Filter Choke")),
	STATE_TEMP_TRIP_FILTERCHOKE_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x203403) //
			.text("(XT) Temp Trip Filter Choke")),
	STATE_TEMP_TRIP_FILTERCHOKE_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x204403) //
			.text("(XT) Temp Trip Filter Choke")),
	STATE_TEMP_TRIP_GRIDCHOKE_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x201404) //
			.text("(XT) Temp Trip Grid Choke")),
	STATE_TEMP_TRIP_GRIDCHOKE_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x202404) //
			.text("(XT) Temp Trip Grid Choke")),
	STATE_TEMP_TRIP_GRIDCHOKE_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x203404) //
			.text("(XT) Temp Trip Grid Choke")),
	STATE_TEMP_TRIP_GRIDCHOKE_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x204404) //
			.text("(XT) Temp Trip Grid Choke")),
	STATE_OUTPUT_PEAK_TRIP_1_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201405) //
			.text("(XT) Output Current Peak Trip Phase 1")),
	STATE_OUTPUT_PEAK_TRIP_1_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202405) //
			.text("(XT) Output Current Peak Trip Phase 1")),
	STATE_OUTPUT_PEAK_TRIP_1_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203405) //
			.text("(XT) Output Current Peak Trip Phase 1")),
	STATE_OUTPUT_PEAK_TRIP_1_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204405) //
			.text("(XT) Output Current Peak Trip Phase 1")),
	STATE_OUTPUT_PEAK_TRIP_2_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201406) //
			.text("(XT) Output Current Peak Trip Phase 2")),
	STATE_OUTPUT_PEAK_TRIP_2_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202406) //
			.text("(XT) Output Current Peak Trip Phase 2")),
	STATE_OUTPUT_PEAK_TRIP_2_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203406) //
			.text("(XT) Output Current Peak Trip Phase 2")),
	STATE_OUTPUT_PEAK_TRIP_2_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204406) //
			.text("(XT) Output Current Peak Trip Phase 2")),
	STATE_OUTPUT_PEAK_TRIP_3_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201407) //
			.text("(XT) Output Current Peak Trip Phase 3")),
	STATE_OUTPUT_PEAK_TRIP_3_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202407) //
			.text("(XT) Output Current Peak Trip Phase 3")),
	STATE_OUTPUT_PEAK_TRIP_3_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203407) //
			.text("(XT) Output Current Peak Trip Phase 3")),
	STATE_OUTPUT_PEAK_TRIP_3_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204407) //
			.text("(XT) Output Current Peak Trip Phase 3")),
	STATE_MODULE_PEAK_TRIP_1_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201408) //
			.text("(XT) Module Current Peak Trip Phase 1")),
	STATE_MODULE_PEAK_TRIP_1_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202408) //
			.text("(XT) Module Current Peak Trip Phase 1")),
	STATE_MODULE_PEAK_TRIP_1_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203408) //
			.text("(XT) Module Current Peak Trip Phase 1")),
	STATE_MODULE_PEAK_TRIP_1_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204408) //
			.text("(XT) Module Current Peak Trip Phase 1")),
	STATE_MODULE_PEAK_TRIP_2_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201409) //
			.text("(XT) Module Current Peak Trip Phase 2")),
	STATE_MODULE_PEAK_TRIP_2_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202409) //
			.text("(XT) Module Current Peak Trip Phase 2")),
	STATE_MODULE_PEAK_TRIP_2_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203409) //
			.text("(XT) Module Current Peak Trip Phase 2")),
	STATE_MODULE_PEAK_TRIP_2_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204409) //
			.text("(XT) Module Current Peak Trip Phase 2")),
	STATE_MODULE_PEAK_TRIP_3_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x20140A) //
			.text("(XT) Module Current Peak Trip Phase 3")),
	STATE_MODULE_PEAK_TRIP_3_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x20240A) //
			.text("(XT) Module Current Peak Trip Phase 3")),
	STATE_MODULE_PEAK_TRIP_3_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x20340A) //
			.text("(XT) Module Current Peak Trip Phase 3")),
	STATE_MODULE_PEAK_TRIP_3_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x20440A) //
			.text("(XT) Module Current Peak Trip Phase 3")),
	STATE_FILTER_RMS_TRIP_1_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20140B) //
			.text("(XT) Filter Current RMS Trip Phase 1")),
	STATE_FILTER_RMS_TRIP_1_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20240B) //
			.text("(XT) Filter Current RMS Trip Phase 1")),
	STATE_FILTER_RMS_TRIP_1_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20340B) //
			.text("(XT) Filter Current RMS Trip Phase 1")),
	STATE_FILTER_RMS_TRIP_1_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20440B) //
			.text("(XT) Filter Current RMS Trip Phase 1")),
	STATE_FILTER_RMS_TRIP_2_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20140C) //
			.text("(XT) Filter Current RMS Trip Phase 2")),
	STATE_FILTER_RMS_TRIP_2_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20240C) //
			.text("(XT) Filter Current RMS Trip Phase 2")),
	STATE_FILTER_RMS_TRIP_2_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20340C) //
			.text("(XT) Filter Current RMS Trip Phase 2")),
	STATE_FILTER_RMS_TRIP_2_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20440C) //
			.text("(XT) Filter Current RMS Trip Phase 2")),
	STATE_FILTER_RMS_TRIP_3_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20140D) //
			.text("(XT) Filter Current RMS Trip Phase 3")),
	STATE_FILTER_RMS_TRIP_3_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20240D) //
			.text("(XT) Filter Current RMS Trip Phase 3")),
	STATE_FILTER_RMS_TRIP_3_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20340D) //
			.text("(XT) Filter Current RMS Trip Phase 3")),
	STATE_FILTER_RMS_TRIP_3_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20440D) //
			.text("(XT) Filter Current RMS Trip Phase 3")),
	STATE_FILTER_SLOWRMS_TRIP_1_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20140E) //
			.text("(XT) Filter Current Slow RMS Trip Phase 1")),
	STATE_FILTER_SLOWRMS_TRIP_1_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20240E) //
			.text("(XT) Filter Current Slow RMS Trip Phase 1")),
	STATE_FILTER_SLOWRMS_TRIP_1_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20340E) //
			.text("(XT) Filter Current Slow RMS Trip Phase 1")),
	STATE_FILTER_SLOWRMS_TRIP_1_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20440E) //
			.text("(XT) Filter Current Slow RMS Trip Phase 1")),
	STATE_FILTER_SLOWRMS_TRIP_2_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20140F) //
			.text("(XT) Filter Current Slow RMS Trip Phase 2")),
	STATE_FILTER_SLOWRMS_TRIP_2_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20240F) //
			.text("(XT) Filter Current Slow RMS Trip Phase 2")),
	STATE_FILTER_SLOWRMS_TRIP_2_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20340F) //
			.text("(XT) Filter Current Slow RMS Trip Phase 2")),
	STATE_FILTER_SLOWRMS_TRIP_2_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20440F) //
			.text("(XT) Filter Current Slow RMS Trip Phase 2")),
	STATE_FILTER_SLOWRMS_TRIP_3_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x201410) //
			.text("(XT) Filter Current Slow RMS Trip Phase 3")),
	STATE_FILTER_SLOWRMS_TRIP_3_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x202410) //
			.text("(XT) Filter Current Slow RMS Trip Phase 3")),
	STATE_FILTER_SLOWRMS_TRIP_3_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x203410) //
			.text("(XT) Filter Current Slow RMS Trip Phase 3")),
	STATE_FILTER_SLOWRMS_TRIP_3_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x204410) //
			.text("(XT) Filter Current Slow RMS Trip Phase 3")),
	STATE_OUTPUT_RMS_TRIP_1_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x201411) //
			.text("(XT) Output Current RMS Trip Phase 1")),
	STATE_OUTPUT_RMS_TRIP_1_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x202411) //
			.text("(XT) Output Current RMS Trip Phase 1")),
	STATE_OUTPUT_RMS_TRIP_1_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x203411) //
			.text("(XT) Output Current RMS Trip Phase 1")),
	STATE_OUTPUT_RMS_TRIP_1_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x204411) //
			.text("(XT) Output Current RMS Trip Phase 1")),
	STATE_OUTPUT_RMS_TRIP_2_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x201412) //
			.text("(XT) Output Current RMS Trip Phase 2")),
	STATE_OUTPUT_RMS_TRIP_2_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x202412) //
			.text("(XT) Output Current RMS Trip Phase 2")),
	STATE_OUTPUT_RMS_TRIP_2_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x203412) //
			.text("(XT) Output Current RMS Trip Phase 2")),
	STATE_OUTPUT_RMS_TRIP_2_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x204412) //
			.text("(XT) Output Current RMS Trip Phase 2")),
	STATE_OUTPUT_RMS_TRIP_3_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x201413) //
			.text("(XT) Output Current RMS Trip Phase 3")),
	STATE_OUTPUT_RMS_TRIP_3_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x202413) //
			.text("(XT) Output Current RMS Trip Phase 3")),
	STATE_OUTPUT_RMS_TRIP_3_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x203413) //
			.text("(XT) Output Current RMS Trip Phase 3")),
	STATE_OUTPUT_RMS_TRIP_3_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x204413) //
			.text("(XT) Output Current RMS Trip Phase 3")),
	STATE_MODULE_FRD_FILTER_C_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x201414).text("(XT) Fast Resonance Detection (FRD-F)")),
	STATE_MODULE_FRD_FILTER_C_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x202414).text("(XT) Fast Resonance Detection (FRD-F)")),
	STATE_MODULE_FRD_FILTER_C_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x203414).text("(XT) Fast Resonance Detection (FRD-F)")),
	STATE_MODULE_FRD_FILTER_C_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x204414).text("(XT) Fast Resonance Detection (FRD-F)")),
	STATE_MODULE_FRD_OUTPUTPEAK_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x201415).text("(XT) Fast Resonance Detection (FRD-P)")),
	STATE_MODULE_FRD_OUTPUTPEAK_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x202415).text("(XT) Fast Resonance Detection (FRD-P)")),
	STATE_MODULE_FRD_OUTPUTPEAK_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x203415).text("(XT) Fast Resonance Detection (FRD-P)")),
	STATE_MODULE_FRD_OUTPUTPEAK_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x204415).text("(XT) Fast Resonance Detection (FRD-P)")),
	STATE_SIA_RUNNING_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x201416) //
			.text("(XT) System Identification Algorithm (SIA) running...")),
	STATE_SIA_RUNNING_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x202416) //
			.text("(XT) System Identification Algorithm (SIA) running...")),
	STATE_SIA_RUNNING_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x203416) //
			.text("(XT) System Identification Algorithm (SIA) running...")),
	STATE_SIA_RUNNING_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x204416) //
			.text("(XT) System Identification Algorithm (SIA) running...")),
	STATE_SIA_WARNING_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x201417) //
			.text("(XT) SIA Warning")),
	STATE_SIA_WARNING_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x202417) //
			.text("(XT) SIA Warning")),
	STATE_SIA_WARNING_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x203417) //
			.text("(XT) SIA Warning")),
	STATE_SIA_WARNING_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x204417) //
			.text("(XT) SIA Warning")),
	STATE_NEUTRAL_RMS_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201418) //
			.text("(XT) Neutral Current RMS Trip")),
	STATE_NEUTRAL_RMS_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202418) //
			.text("(XT) Neutral Current RMS Trip")),
	STATE_NEUTRAL_RMS_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203418) //
			.text("(XT) Neutral Current RMS Trip")),
	STATE_NEUTRAL_RMS_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204418) //
			.text("(XT) Neutral Current RMS Trip")),
	STATE_SUSPECT_HARMONICS_DETECTED_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x201419) //
			.text("(XT) Suspect Harmonics marked")),
	STATE_SUSPECT_HARMONICS_DETECTED_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x202419) //
			.text("(XT) Suspect Harmonics marked")),
	STATE_SUSPECT_HARMONICS_DETECTED_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x203419) //
			.text("(XT) Suspect Harmonics marked")),
	STATE_SUSPECT_HARMONICS_DETECTED_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x204419) //
			.text("(XT) Suspect Harmonics marked")),
	STATE_CRITICAL_HARMONICS_DETECTED_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x20141A).text("(XT) Compensation of critical harmonics blocked")),
	STATE_CRITICAL_HARMONICS_DETECTED_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x20241A).text("(XT) Compensation of critical harmonics blocked")),
	STATE_CRITICAL_HARMONICS_DETECTED_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x20341A).text("(XT) Compensation of critical harmonics blocked")),
	STATE_CRITICAL_HARMONICS_DETECTED_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x20441A).text("(XT) Compensation of critical harmonics blocked")),
	STATE_UDC_P_TRIP_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201800) //
			.text("(XT) DC-Link Positive Voltage Fault")),
	STATE_UDC_P_TRIP_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202800) //
			.text("(XT) DC-Link Positive Voltage Fault")),
	STATE_UDC_P_TRIP_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203800) //
			.text("(XT) DC-Link Positive Voltage Fault")),
	STATE_UDC_P_TRIP_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204800) //
			.text("(XT) DC-Link Positive Voltage Fault")),
	STATE_UDC_N_TRIP_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201801) //
			.text("(XT) DC-Link Negative Voltage Fault")),
	STATE_UDC_N_TRIP_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202801) //
			.text("(XT) DC-Link Negative Voltage Fault")),
	STATE_UDC_N_TRIP_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203801) //
			.text("(XT) DC-Link Negative Voltage Fault")),
	STATE_UDC_N_TRIP_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204801) //
			.text("(XT) DC-Link Negative Voltage Fault")),
	STATE_UDC_DIFF_TRIP_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201802) //
			.text("(XT) DC-Link Voltage Imbalance")),
	STATE_UDC_DIFF_TRIP_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202802) //
			.text("(XT) DC-Link Voltage Imbalance")),
	STATE_UDC_DIFF_TRIP_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203802) //
			.text("(XT) DC-Link Voltage Imbalance")),
	STATE_UDC_DIFF_TRIP_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204802) //
			.text("(XT) DC-Link Voltage Imbalance")),
	STATE_UDC_P_PRECHG_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x201803).text("(XT) DC-Link Precharge Positive Voltage Trip")),
	STATE_UDC_P_PRECHG_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x202803).text("(XT) DC-Link Precharge Positive Voltage Trip")),
	STATE_UDC_P_PRECHG_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x203803).text("(XT) DC-Link Precharge Positive Voltage Trip")),
	STATE_UDC_P_PRECHG_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x204803).text("(XT) DC-Link Precharge Positive Voltage Trip")),
	STATE_UDC_N_PRECHG_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x201804).text("(XT) DC-Link Precharge Negative Voltage Trip")),
	STATE_UDC_N_PRECHG_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x202804).text("(XT) DC-Link Precharge Negative Voltage Trip")),
	STATE_UDC_N_PRECHG_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x203804).text("(XT) DC-Link Precharge Negative Voltage Trip")),
	STATE_UDC_N_PRECHG_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x204804).text("(XT) DC-Link Precharge Negative Voltage Trip")),
	STATE_UDC_P_PRECHG_5_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x201805).text("(XT) DC-Link Precharge Positive Undervoltage Trip")),
	STATE_UDC_P_PRECHG_5_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x202805).text("(XT) DC-Link Precharge Positive Undervoltage Trip")),
	STATE_UDC_P_PRECHG_5_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x203805).text("(XT) DC-Link Precharge Positive Undervoltage Trip")),
	STATE_UDC_P_PRECHG_5_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x204805).text("(XT) DC-Link Precharge Positive Undervoltage Trip")),
	STATE_UDC_N_PRECHG_5_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x201806).text("(XT) DC-Link Precharge Negative Undervoltage Trip")),
	STATE_UDC_N_PRECHG_5_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x202806).text("(XT) DC-Link Precharge Negative Undervoltage Trip")),
	STATE_UDC_N_PRECHG_5_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x203806).text("(XT) DC-Link Precharge Negative Undervoltage Trip")),
	STATE_UDC_N_PRECHG_5_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x204806).text("(XT) DC-Link Precharge Negative Undervoltage Trip")),
	STATE_UDC_P_PRECHG_MAX_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x201807).text("(XT) DC-Link Precharge Positive Overvoltage Trip")),
	STATE_UDC_P_PRECHG_MAX_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x202807).text("(XT) DC-Link Precharge Positive Overvoltage Trip")),
	STATE_UDC_P_PRECHG_MAX_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x203807).text("(XT) DC-Link Precharge Positive Overvoltage Trip")),
	STATE_UDC_P_PRECHG_MAX_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x204807).text("(XT) DC-Link Precharge Positive Overvoltage Trip")),
	STATE_UDC_N_PRECHG_MAX_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x201808).text("(XT) DC-Link Precharge Negative Overvoltage Trip")),
	STATE_UDC_N_PRECHG_MAX_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x202808).text("(XT) DC-Link Precharge Negative Overvoltage Trip")),
	STATE_UDC_N_PRECHG_MAX_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x203808).text("(XT) DC-Link Precharge Negative Overvoltage Trip")),
	STATE_UDC_N_PRECHG_MAX_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x204808).text("(XT) DC-Link Precharge Negative Overvoltage Trip")),
	STATE_TEMP_TRIP_IGBT_1_IPU_1_5(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201400) //
			.text("(XT) Temp Trip IGBT 1")),
	STATE_TEMP_TRIP_IGBT_1_IPU_2_5(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202400) //
			.text("(XT) Temp Trip IGBT 1")),
	STATE_TEMP_TRIP_IGBT_1_IPU_3_5(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203400) //
			.text("(XT) Temp Trip IGBT 1")),
	STATE_TEMP_TRIP_IGBT_1_IPU_4_5(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204400) //
			.text("(XT) Temp Trip IGBT 1")),
	STATE_TEMP_TRIP_IGBT_2_IPU_1_5(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201401) //
			.text("(XT) Temp Trip IGBT 2")),
	STATE_TEMP_TRIP_IGBT_2_IPU_2_5(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202401) //
			.text("(XT) Temp Trip IGBT 2")),
	STATE_TEMP_TRIP_IGBT_2_IPU_3_5(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203401) //
			.text("(XT) Temp Trip IGBT 2")),
	STATE_TEMP_TRIP_IGBT_2_IPU_4_5(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204401) //
			.text("(XT) Temp Trip IGBT 2")),
	STATE_TEMP_TRIP_IGBT_3_IPU_1_5(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201402) //
			.text("(XT) Temp Trip IGBT 3")),
	STATE_TEMP_TRIP_IGBT_3_IPU_2_5(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202402) //
			.text("(XT) Temp Trip IGBT 3")),
	STATE_TEMP_TRIP_IGBT_3_IPU_3_5(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203402) //
			.text("(XT) Temp Trip IGBT 3")),
	STATE_TEMP_TRIP_IGBT_3_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204402) //
			.text("(XT) Temp Trip IGBT 3")),
	STATE_TEMP_TRIP_FILTERCHOKE_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x201403) //
			.text("(XT) Temp Trip Filter Choke")),
	STATE_TEMP_TRIP_FILTERCHOKE_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x202403) //
			.text("(XT) Temp Trip Filter Choke")),
	STATE_TEMP_TRIP_FILTERCHOKE_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x203403) //
			.text("(XT) Temp Trip Filter Choke")),
	STATE_TEMP_TRIP_FILTERCHOKE_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x204403) //
			.text("(XT) Temp Trip Filter Choke")),
	STATE_TEMP_TRIP_GRIDCHOKE_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x201404) //
			.text("(XT) Temp Trip Grid Choke")),
	STATE_TEMP_TRIP_GRIDCHOKE_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x202404) //
			.text("(XT) Temp Trip Grid Choke")),
	STATE_TEMP_TRIP_GRIDCHOKE_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x203404) //
			.text("(XT) Temp Trip Grid Choke")),
	STATE_TEMP_TRIP_GRIDCHOKE_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x204404) //
			.text("(XT) Temp Trip Grid Choke")),
	STATE_OUTPUT_PEAK_TRIP_1_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201405) //
			.text("(XT) Output Current Peak Trip Phase 1")),
	STATE_OUTPUT_PEAK_TRIP_1_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202405) //
			.text("(XT) Output Current Peak Trip Phase 1")),
	STATE_OUTPUT_PEAK_TRIP_1_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203405) //
			.text("(XT) Output Current Peak Trip Phase 1")),
	STATE_OUTPUT_PEAK_TRIP_1_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204405) //
			.text("(XT) Output Current Peak Trip Phase 1")),
	STATE_OUTPUT_PEAK_TRIP_2_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201406) //
			.text("(XT) Output Current Peak Trip Phase 2")),
	STATE_OUTPUT_PEAK_TRIP_2_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202406) //
			.text("(XT) Output Current Peak Trip Phase 2")),
	STATE_OUTPUT_PEAK_TRIP_2_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203406) //
			.text("(XT) Output Current Peak Trip Phase 2")),
	STATE_OUTPUT_PEAK_TRIP_2_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204406) //
			.text("(XT) Output Current Peak Trip Phase 2")),
	STATE_OUTPUT_PEAK_TRIP_3_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201407) //
			.text("(XT) Output Current Peak Trip Phase 3")),
	STATE_OUTPUT_PEAK_TRIP_3_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202407) //
			.text("(XT) Output Current Peak Trip Phase 3")),
	STATE_OUTPUT_PEAK_TRIP_3_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203407) //
			.text("(XT) Output Current Peak Trip Phase 3")),
	STATE_OUTPUT_PEAK_TRIP_3_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204407) //
			.text("(XT) Output Current Peak Trip Phase 3")),
	STATE_MODULE_PEAK_TRIP_1_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201408) //
			.text("(XT) Module Current Peak Trip Phase 1")),
	STATE_MODULE_PEAK_TRIP_1_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202408) //
			.text("(XT) Module Current Peak Trip Phase 1")),
	STATE_MODULE_PEAK_TRIP_1_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203408) //
			.text("(XT) Module Current Peak Trip Phase 1")),
	STATE_MODULE_PEAK_TRIP_1_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204408) //
			.text("(XT) Module Current Peak Trip Phase 1")),
	STATE_MODULE_PEAK_TRIP_2_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201409) //
			.text("(XT) Module Current Peak Trip Phase 2")),
	STATE_MODULE_PEAK_TRIP_2_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202409) //
			.text("(XT) Module Current Peak Trip Phase 2")),
	STATE_MODULE_PEAK_TRIP_2_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203409) //
			.text("(XT) Module Current Peak Trip Phase 2")),
	STATE_MODULE_PEAK_TRIP_2_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204409) //
			.text("(XT) Module Current Peak Trip Phase 2")),
	STATE_MODULE_PEAK_TRIP_3_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x20140A) //
			.text("(XT) Module Current Peak Trip Phase 3")),
	STATE_MODULE_PEAK_TRIP_3_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x20240A) //
			.text("(XT) Module Current Peak Trip Phase 3")),
	STATE_MODULE_PEAK_TRIP_3_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x20340A) //
			.text("(XT) Module Current Peak Trip Phase 3")),
	STATE_MODULE_PEAK_TRIP_3_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x20440A) //
			.text("(XT) Module Current Peak Trip Phase 3")),
	STATE_FILTER_RMS_TRIP_1_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20140B) //
			.text("(XT) Filter Current RMS Trip Phase 1")),
	STATE_FILTER_RMS_TRIP_1_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20240B) //
			.text("(XT) Filter Current RMS Trip Phase 1")),
	STATE_FILTER_RMS_TRIP_1_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20340B) //
			.text("(XT) Filter Current RMS Trip Phase 1")),
	STATE_FILTER_RMS_TRIP_1_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20440B) //
			.text("(XT) Filter Current RMS Trip Phase 1")),
	STATE_FILTER_RMS_TRIP_2_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20140C) //
			.text("(XT) Filter Current RMS Trip Phase 2")),
	STATE_FILTER_RMS_TRIP_2_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20240C) //
			.text("(XT) Filter Current RMS Trip Phase 2")),
	STATE_FILTER_RMS_TRIP_2_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20340C) //
			.text("(XT) Filter Current RMS Trip Phase 2")),
	STATE_FILTER_RMS_TRIP_2_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20440C) //
			.text("(XT) Filter Current RMS Trip Phase 2")),
	STATE_FILTER_RMS_TRIP_3_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20140D) //
			.text("(XT) Filter Current RMS Trip Phase 3")),
	STATE_FILTER_RMS_TRIP_3_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20240D) //
			.text("(XT) Filter Current RMS Trip Phase 3")),
	STATE_FILTER_RMS_TRIP_3_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20340D) //
			.text("(XT) Filter Current RMS Trip Phase 3")),
	STATE_FILTER_RMS_TRIP_3_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20440D) //
			.text("(XT) Filter Current RMS Trip Phase 3")),
	STATE_FILTER_SLOWRMS_TRIP_1_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20140E) //
			.text("(XT) Filter Current Slow RMS Trip Phase 1")),
	STATE_FILTER_SLOWRMS_TRIP_1_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20240E) //
			.text("(XT) Filter Current Slow RMS Trip Phase 1")),
	STATE_FILTER_SLOWRMS_TRIP_1_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20340E) //
			.text("(XT) Filter Current Slow RMS Trip Phase 1")),
	STATE_FILTER_SLOWRMS_TRIP_1_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20440E) //
			.text("(XT) Filter Current Slow RMS Trip Phase 1")),
	STATE_FILTER_SLOWRMS_TRIP_2_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20140F) //
			.text("(XT) Filter Current Slow RMS Trip Phase 2")),
	STATE_FILTER_SLOWRMS_TRIP_2_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20240F) //
			.text("(XT) Filter Current Slow RMS Trip Phase 2")),
	STATE_FILTER_SLOWRMS_TRIP_2_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20340F) //
			.text("(XT) Filter Current Slow RMS Trip Phase 2")),
	STATE_FILTER_SLOWRMS_TRIP_2_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x20440F) //
			.text("(XT) Filter Current Slow RMS Trip Phase 2")),
	STATE_FILTER_SLOWRMS_TRIP_3_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x201410) //
			.text("(XT) Filter Current Slow RMS Trip Phase 3")),
	STATE_FILTER_SLOWRMS_TRIP_3_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x202410) //
			.text("(XT) Filter Current Slow RMS Trip Phase 3")),
	STATE_FILTER_SLOWRMS_TRIP_3_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x203410) //
			.text("(XT) Filter Current Slow RMS Trip Phase 3")),
	STATE_FILTER_SLOWRMS_TRIP_3_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x204410) //
			.text("(XT) Filter Current Slow RMS Trip Phase 3")),
	STATE_OUTPUT_RMS_TRIP_1_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x201411) //
			.text("(XT) Output Current RMS Trip Phase 1")),
	STATE_OUTPUT_RMS_TRIP_1_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x202411) //
			.text("(XT) Output Current RMS Trip Phase 1")),
	STATE_OUTPUT_RMS_TRIP_1_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x203411) //
			.text("(XT) Output Current RMS Trip Phase 1")),
	STATE_OUTPUT_RMS_TRIP_1_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x204411) //
			.text("(XT) Output Current RMS Trip Phase 1")),
	STATE_OUTPUT_RMS_TRIP_2_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x201412) //
			.text("(XT) Output Current RMS Trip Phase 2")),
	STATE_OUTPUT_RMS_TRIP_2_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x202412) //
			.text("(XT) Output Current RMS Trip Phase 2")),
	STATE_OUTPUT_RMS_TRIP_2_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x203412) //
			.text("(XT) Output Current RMS Trip Phase 2")),
	STATE_OUTPUT_RMS_TRIP_2_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x204412) //
			.text("(XT) Output Current RMS Trip Phase 2")),
	STATE_OUTPUT_RMS_TRIP_3_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x201413) //
			.text("(XT) Output Current RMS Trip Phase 3")),
	STATE_OUTPUT_RMS_TRIP_3_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x202413) //
			.text("(XT) Output Current RMS Trip Phase 3")),
	STATE_OUTPUT_RMS_TRIP_3_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x203413) //
			.text("(XT) Output Current RMS Trip Phase 3")),
	STATE_OUTPUT_RMS_TRIP_3_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x204413) //
			.text("(XT) Output Current RMS Trip Phase 3")),
	STATE_MODULE_FRD_FILTER_C_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x201414).text("(XT) Fast Resonance Detection (FRD-F)")),
	STATE_MODULE_FRD_FILTER_C_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x202414).text("(XT) Fast Resonance Detection (FRD-F)")),
	STATE_MODULE_FRD_FILTER_C_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x203414).text("(XT) Fast Resonance Detection (FRD-F)")),
	STATE_MODULE_FRD_FILTER_C_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x204414).text("(XT) Fast Resonance Detection (FRD-F)")),
	STATE_MODULE_FRD_OUTPUTPEAK_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x201415).text("(XT) Fast Resonance Detection (FRD-P)")),
	STATE_MODULE_FRD_OUTPUTPEAK_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x202415).text("(XT) Fast Resonance Detection (FRD-P)")),
	STATE_MODULE_FRD_OUTPUTPEAK_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x203415).text("(XT) Fast Resonance Detection (FRD-P)")),
	STATE_MODULE_FRD_OUTPUTPEAK_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x204415).text("(XT) Fast Resonance Detection (FRD-P)")),
	STATE_SIA_RUNNING_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x201416) //
			.text("(XT) System Identification Algorithm (SIA) running...")),
	STATE_SIA_RUNNING_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x202416) //
			.text("(XT) System Identification Algorithm (SIA) running...")),
	STATE_SIA_RUNNING_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x203416) //
			.text("(XT) System Identification Algorithm (SIA) running...")),
	STATE_SIA_RUNNING_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x204416) //
			.text("(XT) System Identification Algorithm (SIA) running...")),
	STATE_SIA_WARNING_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x201417) //
			.text("(XT) SIA Warning")),
	STATE_SIA_WARNING_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x202417) //
			.text("(XT) SIA Warning")),
	STATE_SIA_WARNING_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x203417) //
			.text("(XT) SIA Warning")),
	STATE_SIA_WARNING_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x204417) //
			.text("(XT) SIA Warning")),
	STATE_NEUTRAL_RMS_TRIP_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201418) //
			.text("(XT) Neutral Current RMS Trip")),
	STATE_NEUTRAL_RMS_TRIP_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202418) //
			.text("(XT) Neutral Current RMS Trip")),
	STATE_NEUTRAL_RMS_TRIP_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203418) //
			.text("(XT) Neutral Current RMS Trip")),
	STATE_NEUTRAL_RMS_TRIP_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204418) //
			.text("(XT) Neutral Current RMS Trip")),
	STATE_SUSPECT_HARMONICS_DETECTED_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x201419) //
			.text("(XT) Suspect Harmonics marked")),
	STATE_SUSPECT_HARMONICS_DETECTED_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x202419) //
			.text("(XT) Suspect Harmonics marked")),
	STATE_SUSPECT_HARMONICS_DETECTED_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x203419) //
			.text("(XT) Suspect Harmonics marked")),
	STATE_SUSPECT_HARMONICS_DETECTED_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x204419) //
			.text("(XT) Suspect Harmonics marked")),
	STATE_CRITICAL_HARMONICS_DETECTED_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x20141A).text("(XT) Compensation of critical harmonics blocked")),
	STATE_CRITICAL_HARMONICS_DETECTED_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x20241A).text("(XT) Compensation of critical harmonics blocked")),
	STATE_CRITICAL_HARMONICS_DETECTED_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x20341A).text("(XT) Compensation of critical harmonics blocked")),
	STATE_CRITICAL_HARMONICS_DETECTED_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x20441A).text("(XT) Compensation of critical harmonics blocked")),
	STATE_UDC_P_TRIP_IPU_1_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201800) //
			.text("(XT) DC-Link Positive Voltage Fault")),
	STATE_UDC_P_TRIP_IPU_2_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202800) //
			.text("(XT) DC-Link Positive Voltage Fault")),
	STATE_UDC_P_TRIP_IPU_3_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203800) //
			.text("(XT) DC-Link Positive Voltage Fault")),
	STATE_UDC_P_TRIP_IPU_4_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204800) //
			.text("(XT) DC-Link Positive Voltage Fault")),
	STATE_UDC_N_TRIP_IPU_1_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201801) //
			.text("(XT) DC-Link Negative Voltage Fault")),
	STATE_UDC_N_TRIP_IPU_2_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202801) //
			.text("(XT) DC-Link Negative Voltage Fault")),
	STATE_UDC_N_TRIP_IPU_3_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203801) //
			.text("(XT) DC-Link Negative Voltage Fault")),
	STATE_UDC_N_TRIP_IPU_4_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204801) //
			.text("(XT) DC-Link Negative Voltage Fault")),
	STATE_UDC_DIFF_TRIP_IPU_1_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x201802) //
			.text("(XT) DC-Link Voltage Imbalance")),
	STATE_UDC_DIFF_TRIP_IPU_2_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x202802) //
			.text("(XT) DC-Link Voltage Imbalance")),
	STATE_UDC_DIFF_TRIP_IPU_3_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x203802) //
			.text("(XT) DC-Link Voltage Imbalance")),
	STATE_UDC_DIFF_TRIP_IPU_4_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x204802) //
			.text("(XT) DC-Link Voltage Imbalance")),
	STATE_UDC_P_PRECHG_TRIP_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x201803).text("(XT) DC-Link Precharge Positive Voltage Trip")),
	STATE_UDC_P_PRECHG_TRIP_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x202803).text("(XT) DC-Link Precharge Positive Voltage Trip")),
	STATE_UDC_P_PRECHG_TRIP_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x203803).text("(XT) DC-Link Precharge Positive Voltage Trip")),
	STATE_UDC_P_PRECHG_TRIP_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x204803).text("(XT) DC-Link Precharge Positive Voltage Trip")),
	STATE_UDC_N_PRECHG_TRIP_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x201804).text("(XT) DC-Link Precharge Negative Voltage Trip")),
	STATE_UDC_N_PRECHG_TRIP_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x202804).text("(XT) DC-Link Precharge Negative Voltage Trip")),
	STATE_UDC_N_PRECHG_TRIP_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x203804).text("(XT) DC-Link Precharge Negative Voltage Trip")),
	STATE_UDC_N_PRECHG_TRIP_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x204804).text("(XT) DC-Link Precharge Negative Voltage Trip")),
	STATE_UDC_P_PRECHG_5_TRIP_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x201805).text("(XT) DC-Link Precharge Positive Undervoltage Trip")),
	STATE_UDC_P_PRECHG_5_TRIP_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x202805).text("(XT) DC-Link Precharge Positive Undervoltage Trip")),
	STATE_UDC_P_PRECHG_5_TRIP_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x203805).text("(XT) DC-Link Precharge Positive Undervoltage Trip")),
	STATE_UDC_P_PRECHG_5_TRIP_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x204805).text("(XT) DC-Link Precharge Positive Undervoltage Trip")),
	STATE_UDC_N_PRECHG_5_TRIP_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x201806).text("(XT) DC-Link Precharge Negative Undervoltage Trip")),
	STATE_UDC_N_PRECHG_5_TRIP_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x202806).text("(XT) DC-Link Precharge Negative Undervoltage Trip")),
	STATE_UDC_N_PRECHG_5_TRIP_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x203806).text("(XT) DC-Link Precharge Negative Undervoltage Trip")),
	STATE_UDC_N_PRECHG_5_TRIP_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x204806).text("(XT) DC-Link Precharge Negative Undervoltage Trip")),
	STATE_UDC_P_PRECHG_MAX_TRIP_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x201807).text("(XT) DC-Link Precharge Positive Overvoltage Trip")),
	STATE_UDC_P_PRECHG_MAX_TRIP_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x202807).text("(XT) DC-Link Precharge Positive Overvoltage Trip")),
	STATE_UDC_P_PRECHG_MAX_TRIP_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x203807).text("(XT) DC-Link Precharge Positive Overvoltage Trip")),
	STATE_UDC_P_PRECHG_MAX_TRIP_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x204807).text("(XT) DC-Link Precharge Positive Overvoltage Trip")),
	STATE_UDC_N_PRECHG_MAX_TRIP_IPU_1_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x201808).text("(XT) DC-Link Precharge Negative Overvoltage Trip")),
	STATE_UDC_N_PRECHG_MAX_TRIP_IPU_2_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x202808).text("(XT) DC-Link Precharge Negative Overvoltage Trip")),
	STATE_UDC_N_PRECHG_MAX_TRIP_IPU_3_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x203808).text("(XT) DC-Link Precharge Negative Overvoltage Trip")),
	STATE_UDC_N_PRECHG_MAX_TRIP_IPU_4_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x204808).text("(XT) DC-Link Precharge Negative Overvoltage Trip")),
	STATE_HARDWARE_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(true) //
			.code(0x201809) //
			.text("(XT) Hardware Trip")),
	STATE_HARDWARE_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(true) //
			.code(0x202809) //
			.text("(XT) Hardware Trip")),
	STATE_HARDWARE_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(true) //
			.code(0x203809) //
			.text("(XT) Hardware Trip")),
	STATE_HARDWARE_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(true) //
			.code(0x204809) //
			.text("(XT) Hardware Trip")),
	STATE_PRECHARGE_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20180A) //
			.text("(XT) Precharge Trip")),
	STATE_PRECHARGE_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20280A) //
			.text("(XT) Precharge Trip")),
	STATE_PRECHARGE_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20380A) //
			.text("(XT) Precharge Trip")),
	STATE_PRECHARGE_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20480A) //
			.text("(XT) Precharge Trip")),
	STATE_STATE_TRIP_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20180B) //
			.text("(XT) StateObject Trip")),
	STATE_STATE_TRIP_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20280B) //
			.text("(XT) StateObject Trip")),
	STATE_STATE_TRIP_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20380B) //
			.text("(XT) StateObject Trip")),
	STATE_STATE_TRIP_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20480B) //
			.text("(XT) StateObject Trip")),
	STATE_TRH_ERROR_Control_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20180C) //
			.text("(XT) Transient Handling Error")),
	STATE_TRH_ERROR_Control_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20280C) //
			.text("(XT) Transient Handling Error")),
	STATE_TRH_ERROR_Control_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20380C) //
			.text("(XT) Transient Handling Error")),
	STATE_TRH_ERROR_Control_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x20480C) //
			.text("(XT) Transient Handling Error")),
	STATE_M_TIMEOUT_CCU_MASTER_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x20180D) //
			.text("(XT) Communication Timeout @ Master")),
	STATE_M_TIMEOUT_CCU_MASTER_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x20280D) //
			.text("(XT) Communication Timeout @ Master")),
	STATE_M_TIMEOUT_CCU_MASTER_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x20380D) //
			.text("(XT) Communication Timeout @ Master")),
	STATE_M_TIMEOUT_CCU_MASTER_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x20480D) //
			.text("(XT) Communication Timeout @ Master")),
	STATE_S_COMMUNICATION_CCU_SLAVE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x20180E).text("(XT) Communication Error @ Slave")),
	STATE_S_COMMUNICATION_CCU_SLAVE_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x20280E).text("(XT) Communication Error @ Slave")),
	STATE_S_COMMUNICATION_CCU_SLAVE_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x20380E).text("(XT) Communication Error @ Slave")),
	STATE_S_COMMUNICATION_CCU_SLAVE_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x20480E).text("(XT) Communication Error @ Slave")),
	STATE_S_SOFTWARE_CCU_SLAVE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x20180F) //
			.text("(XT) SW Error @ Slave")),
	STATE_S_SOFTWARE_CCU_SLAVE_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x20280F) //
			.text("(XT) SW Error @ Slave")),
	STATE_S_SOFTWARE_CCU_SLAVE_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x20380F) //
			.text("(XT) SW Error @ Slave")),
	STATE_S_SOFTWARE_CCU_SLAVE_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.DISABLED) //
			.needsHardReset(false) //
			.code(0x20480F) //
			.text("(XT) SW Error @ Slave")),
	STATE_INDIVIDUAL_SM_TIMEOUT_STATE_MACHINE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201810) //
			.text("(XT) IPU Statemachine timeout")),
	STATE_INDIVIDUAL_SM_TIMEOUT_STATE_MACHINE_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202810) //
			.text("(XT) IPU Statemachine timeout")),
	STATE_INDIVIDUAL_SM_TIMEOUT_STATE_MACHINE_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203810) //
			.text("(XT) IPU Statemachine timeout")),
	STATE_INDIVIDUAL_SM_TIMEOUT_STATE_MACHINE_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204810) //
			.text("(XT) IPU Statemachine timeout")),
	STATE_WRONGIPUTYPE_IPU_1_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201811) //
			.text("(XT) Incorrect IPU type detected")),
	STATE_WRONGIPUTYPE_IPU_2_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202811) //
			.text("(XT) Incorrect IPU type detected")),
	STATE_WRONGIPUTYPE_IPU_3_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203811) //
			.text("(XT) Incorrect IPU type detected")),
	STATE_WRONGIPUTYPE_IPU_4_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204811) //
			.text("(XT) Incorrect IPU type detected")),
	STATE_WRONGFWVERSION_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201812) //
			.text("(XT) IPU Firmware incompatible")),
	STATE_WRONGFWVERSION_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202812) //
			.text("(XT) IPU Firmware incompatible")),
	STATE_WRONGFWVERSION_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203812) //
			.text("(XT) IPU Firmware incompatible")),
	STATE_WRONGFWVERSION_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204812) //
			.text("(XT) IPU Firmware incompatible")),
	STATE_INDIV_SM_IGNORED_SYSLOCK_STATE_MACHINE_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201813).text("(XT) individual SM ignored Syslock")),
	STATE_INDIV_SM_IGNORED_SYSLOCK_STATE_MACHINE_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202813).text("(XT) individual SM ignored Syslock")),
	STATE_INDIV_SM_IGNORED_SYSLOCK_STATE_MACHINE_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203813).text("(XT) individual SM ignored Syslock")),
	STATE_INDIV_SM_IGNORED_SYSLOCK_STATE_MACHINE_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204813).text("(XT) individual SM ignored Syslock")),
	STATE_ENABLE24_OFF__1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x201814) //
			.text("(XT) ENABLE 24V OFF")),
	STATE_ENABLE24_OFF__2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x202814) //
			.text("(XT) ENABLE 24V OFF")),
	STATE_ENABLE24_OFF__3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x203814) //
			.text("(XT) ENABLE 24V OFF")),
	STATE_ENABLE24_OFF__4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x204814) //
			.text("(XT) ENABLE 24V OFF")),
	STATE_TEMP_TRIP_IGBT_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401400) //
			.text("(XT) Temp Trip IGBT")),
	STATE_TEMP_TRIP_IGBT_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402400) //
			.text("(XT) Temp Trip IGBT")),
	STATE_TEMP_TRIP_IGBT_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403400) //
			.text("(XT) Temp Trip IGBT")),
	STATE_TEMP_TRIP_IGBT_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404400) //
			.text("(XT) Temp Trip IGBT")),
	STATE_TEMP_CHOKE_TRIP_C_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401401) //
			.text("(XT) Temp Trip Choke C")),
	STATE_TEMP_CHOKE_TRIP_C_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402401) //
			.text("(XT) Temp Trip Choke C")),
	STATE_TEMP_CHOKE_TRIP_C_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403401) //
			.text("(XT) Temp Trip Choke C")),
	STATE_TEMP_CHOKE_TRIP_C_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404401) //
			.text("(XT) Temp Trip Choke C")),
	STATE_TEMP_CHOKE_TRIP_B_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401402) //
			.text("(XT) Temp Trip Choke B")),
	STATE_TEMP_CHOKE_TRIP_B_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402402) //
			.text("(XT) Temp Trip Choke B")),
	STATE_TEMP_CHOKE_TRIP_B_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403402) //
			.text("(XT) Temp Trip Choke B")),
	STATE_TEMP_CHOKE_TRIP_B_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404402) //
			.text("(XT) Temp Trip Choke B")),
	STATE_TEMP_CHOKE_TRIP_A_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401403) //
			.text("(XT) Temp Trip Choke A")),
	STATE_TEMP_CHOKE_TRIP_A_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402403) //
			.text("(XT) Temp Trip Choke A")),
	STATE_TEMP_CHOKE_TRIP_A_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403403) //
			.text("(XT) Temp Trip Choke A")),
	STATE_TEMP_CHOKE_TRIP_A_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404403) //
			.text("(XT) Temp Trip Choke A")),
	STATE_PEAK_CURRENT_TRIP_A_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x401404) //
			.text("(XT) String Current Peak Trip Phase A")),
	STATE_PEAK_CURRENT_TRIP_A_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x402404) //
			.text("(XT) String Current Peak Trip Phase A")),
	STATE_PEAK_CURRENT_TRIP_A_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x403404) //
			.text("(XT) String Current Peak Trip Phase A")),
	STATE_PEAK_CURRENT_TRIP_A_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x404404) //
			.text("(XT) String Current Peak Trip Phase A")),
	STATE_PEAK_CURRENT_TRIP_B_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x401405) //
			.text("(XT) String Current Peak Trip Phase B")),
	STATE_PEAK_CURRENT_TRIP_B_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x402405) //
			.text("(XT) String Current Peak Trip Phase B")),
	STATE_PEAK_CURRENT_TRIP_B_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x403405) //
			.text("(XT) String Current Peak Trip Phase B")),
	STATE_PEAK_CURRENT_TRIP_B_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x404405) //
			.text("(XT) String Current Peak Trip Phase B")),
	STATE_PEAK_CURRENT_TRIP_C_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x401406) //
			.text("(XT) String Current Peak Trip Phase C")),
	STATE_PEAK_CURRENT_TRIP_C_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x402406) //
			.text("(XT) String Current Peak Trip Phase C")),
	STATE_PEAK_CURRENT_TRIP_C_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x403406) //
			.text("(XT) String Current Peak Trip Phase C")),
	STATE_PEAK_CURRENT_TRIP_C_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x404406) //
			.text("(XT) String Current Peak Trip Phase C")),
	STATE_STRING_HIGH_VOLTAGE_TRIP_A_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x401407) //
			.text("(XT) String High Voltage Trip Phase A")),
	STATE_STRING_HIGH_VOLTAGE_TRIP_A_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x402407) //
			.text("(XT) String High Voltage Trip Phase A")),
	STATE_STRING_HIGH_VOLTAGE_TRIP_A_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x403407) //
			.text("(XT) String High Voltage Trip Phase A")),
	STATE_STRING_HIGH_VOLTAGE_TRIP_A_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x404407) //
			.text("(XT) String High Voltage Trip Phase A")),
	STATE_STRING_HIGH_VOLTAGE_TRIP_B_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x401408) //
			.text("(XT) String High Voltage Trip Phase B")),
	STATE_STRING_HIGH_VOLTAGE_TRIP_B_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x402408) //
			.text("(XT) String High Voltage Trip Phase B")),
	STATE_STRING_HIGH_VOLTAGE_TRIP_B_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x403408) //
			.text("(XT) String High Voltage Trip Phase B")),
	STATE_STRING_HIGH_VOLTAGE_TRIP_B_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x404408) //
			.text("(XT) String High Voltage Trip Phase B")),
	STATE_STRING_HIGH_VOLTAGE_TRIP_C_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x401409) //
			.text("(XT) String High Voltage Trip Phase C")),
	STATE_STRING_HIGH_VOLTAGE_TRIP_C_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x402409) //
			.text("(XT) String High Voltage Trip Phase C")),
	STATE_STRING_HIGH_VOLTAGE_TRIP_C_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x403409) //
			.text("(XT) String High Voltage Trip Phase C")),
	STATE_STRING_HIGH_VOLTAGE_TRIP_C_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x404409) //
			.text("(XT) String High Voltage Trip Phase C")),
	STATE_STRING_LOW_VOLTAGE_TRIP_A_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x40140A) //
			.text("(XT) String Low Voltage Trip Phase A")),
	STATE_STRING_LOW_VOLTAGE_TRIP_A_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x40240A) //
			.text("(XT) String Low Voltage Trip Phase A")),
	STATE_STRING_LOW_VOLTAGE_TRIP_A_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x40340A) //
			.text("(XT) String Low Voltage Trip Phase A")),
	STATE_STRING_LOW_VOLTAGE_TRIP_A_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x40440A) //
			.text("(XT) String Low Voltage Trip Phase A")),
	STATE_STRING_LOW_VOLTAGE_TRIP_B_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x40140B) //
			.text("(XT) String Low Voltage Trip Phase B")),
	STATE_STRING_LOW_VOLTAGE_TRIP_B_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x40240B) //
			.text("(XT) String Low Voltage Trip Phase B")),
	STATE_STRING_LOW_VOLTAGE_TRIP_B_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x40340B) //
			.text("(XT) String Low Voltage Trip Phase B")),
	STATE_STRING_LOW_VOLTAGE_TRIP_B_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x40440B) //
			.text("(XT) String Low Voltage Trip Phase B")),
	STATE_STRING_LOW_VOLTAGE_TRIP_C_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x40140C) //
			.text("(XT) String Low Voltage Trip Phase C")),
	STATE_STRING_LOW_VOLTAGE_TRIP_C_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x40240C) //
			.text("(XT) String Low Voltage Trip Phase C")),
	STATE_STRING_LOW_VOLTAGE_TRIP_C_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x40340C) //
			.text("(XT) String Low Voltage Trip Phase C")),
	STATE_STRING_LOW_VOLTAGE_TRIP_C_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x40440C) //
			.text("(XT) String Low Voltage Trip Phase C")),
	STATE_STRING_CURRENT_TRIP_1_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x40140D) //
			.text("(XT) String Current RMS Trip Phase 1")),
	STATE_STRING_CURRENT_TRIP_1_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x40240D) //
			.text("(XT) String Current RMS Trip Phase 1")),
	STATE_STRING_CURRENT_TRIP_1_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x40340D) //
			.text("(XT) String Current RMS Trip Phase 1")),
	STATE_STRING_CURRENT_TRIP_1_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x40440D) //
			.text("(XT) String Current RMS Trip Phase 1")),
	STATE_STRING_CURRENT_TRIP_2_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x40140E) //
			.text("(XT) String Current RMS Trip Phase 2")),
	STATE_STRING_CURRENT_TRIP_2_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x40240E) //
			.text("(XT) String Current RMS Trip Phase 2")),
	STATE_STRING_CURRENT_TRIP_2_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x40340E) //
			.text("(XT) String Current RMS Trip Phase 2")),
	STATE_STRING_CURRENT_TRIP_2_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x40440E) //
			.text("(XT) String Current RMS Trip Phase 2")),
	STATE_STRING_CURRENT_TRIP_3_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x40140F) //
			.text("(XT) String Current RMS Trip Phase 3")),
	STATE_STRING_CURRENT_TRIP_3_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x40240F) //
			.text("(XT) String Current RMS Trip Phase 3")),
	STATE_STRING_CURRENT_TRIP_3_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x40340F) //
			.text("(XT) String Current RMS Trip Phase 3")),
	STATE_STRING_CURRENT_TRIP_3_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false).code(0x40440F) //
			.text("(XT) String Current RMS Trip Phase 3")),
	STATE_UDC_P_TRIP_IPU_1_5(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x401800) //
			.text("(XT) DC-Link Positive Voltage Fault")),
	STATE_UDC_P_TRIP_IPU_2_5(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x402800) //
			.text("(XT) DC-Link Positive Voltage Fault")),
	STATE_UDC_P_TRIP_IPU_3_5(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x403800) //
			.text("(XT) DC-Link Positive Voltage Fault")),
	STATE_UDC_P_TRIP_IPU_4_5(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x404800) //
			.text("(XT) DC-Link Positive Voltage Fault")),
	STATE_HARDWARE_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(true) //
			.code(0x401801) //
			.text("(XT) Hardware Trip")),
	STATE_HARDWARE_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(true) //
			.code(0x402801) //
			.text("(XT) Hardware Trip")),
	STATE_HARDWARE_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(true) //
			.code(0x403801) //
			.text("(XT) Hardware Trip")),
	STATE_HARDWARE_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(true) //
			.code(0x404801) //
			.text("(XT) Hardware Trip")),
	STATE_PRECHARGE_TRIP_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401802) //
			.text("(XT) Precharge Trip")),
	STATE_PRECHARGE_TRIP_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402802) //
			.text("(XT) Precharge Trip")),
	STATE_PRECHARGE_TRIP_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403802) //
			.text("(XT) Precharge Trip")),
	STATE_PRECHARGE_TRIP_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404802) //
			.text("(XT) Precharge Trip")),
	STATE_STATE_TRIP_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401803) //
			.text("(XT) StateObject Trip")),
	STATE_STATE_TRIP_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402803) //
			.text("(XT) StateObject Trip")),
	STATE_STATE_TRIP_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403803) //
			.text("(XT) StateObject Trip")),
	STATE_STATE_TRIP_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(true) // not according to list, hard reset is necessary
			.code(0x404803) //
			.text("(XT) StateObject Trip")),
	STATE_ENABLE24_OFF_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401804) //
			.text("(XT) ENABLE 24V OFF")),
	STATE_ENABLE24_OFF_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402804) //
			.text("(XT) ENABLE 24V OFF")),
	STATE_ENABLE24_OFF_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403804) //
			.text("(XT) ENABLE 24V OFF")),
	STATE_ENABLE24_OFF_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404804) //
			.text("(XT) ENABLE 24V OFF")),
	STATE_LINKVOLT_NOT_VALID_FOR_CONNECTING_DCDC_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401805).text("(XT) Ulink is not valid for connecting DCDC")),
	STATE_LINKVOLT_NOT_VALID_FOR_CONNECTING_DCDC_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402805).text("(XT) Ulink is not valid for connecting DCDC")),
	STATE_LINKVOLT_NOT_VALID_FOR_CONNECTING_DCDC_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403805).text("(XT) Ulink is not valid for connecting DCDC")),
	STATE_LINKVOLT_NOT_VALID_FOR_CONNECTING_DCDC_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404805).text("(XT) Ulink is not valid for connecting DCDC")),
	STATE_INDIVIDUAL_SM_TIMEOUT_IPU_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401806) //
			.text("(XT) IPU Statemachine timeout")),
	STATE_INDIVIDUAL_SM_TIMEOUT_IPU_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402806) //
			.text("(XT) IPU Statemachine timeout")),
	STATE_INDIVIDUAL_SM_TIMEOUT_IPU_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403806) //
			.text("(XT) IPU Statemachine timeout")),
	STATE_INDIVIDUAL_SM_TIMEOUT_IPU_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404806) //
			.text("(XT) IPU Statemachine timeout")),
	STATE_WRONGIPUTYPE_IPU_1_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x401807) //
			.text("(XT) Incorrect IPU type detected")),
	STATE_WRONGIPUTYPE_IPU_2_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x402807) //
			.text("(XT) Incorrect IPU type detected")),
	STATE_WRONGIPUTYPE_IPU_3_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x403807) //
			.text("(XT) Incorrect IPU type detected")),
	STATE_WRONGIPUTYPE_IPU_4_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x404807) //
			.text("(XT) Incorrect IPU type detected")),
	STATE_VOLTAGE_TRIP_MIO30D_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x301100) //
			.text("AC Supply Trip")),
	STATE_VOLTAGE_TRIP_MIO30D_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x302100) //
			.text("AC Supply Trip")),
	STATE_VOLTAGE_TRIP_MIO30D_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x303100) //
			.text("AC Supply Trip")),
	STATE_VOLTAGE_TRIP_MIO30D_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x304100) //
			.text("AC Supply Trip")),
	STATE_WATCHDOG_TRIP_MIO30D_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x301109) //
			.text("Watchdog Timeout")),
	STATE_WATCHDOG_TRIP_MIO30D_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x302109) //
			.text("Watchdog Timeout")),
	STATE_WATCHDOG_TRIP_MIO30D_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x303109) //
			.text("Watchdog Timeout")),
	STATE_WATCHDOG_TRIP_MIO30D_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x304109) //
			.text("Watchdog Timeout")),
	STATE_CURRENT_LOOP_TRIP_MIO30D_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x30110A) //
			.text("CurrentLoop Open")),
	STATE_CURRENT_LOOP_TRIP_MIO30D_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x30210A) //
			.text("CurrentLoop Open")),
	STATE_CURRENT_LOOP_TRIP_MIO30D_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x30310A) //
			.text("CurrentLoop Open")),
	STATE_CURRENT_LOOP_TRIP_MIO30D_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false).code(0x30410A) //
			.text("CurrentLoop Open")),
	STATE_PLC_TRIP_FAST_MIO30D_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x301400) //
			.text("(XT) PLC Trip Fast")),
	STATE_PLC_TRIP_FAST_MIO30D_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x302400) //
			.text("(XT) PLC Trip Fast")),
	STATE_PLC_TRIP_FAST_MIO30D_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x303400) //
			.text("(XT) PLC Trip Fast")),
	STATE_PLC_TRIP_FAST_MIO30D_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x304400) //
			.text("(XT) PLC Trip Fast")),
	STATE_PLC_TRIP_SEQUENCED_MIO30D_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false).code(0x301401) //
			.text("(XT) PLC Trip Sequenced")),
	STATE_PLC_TRIP_SEQUENCED_MIO30D_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false).code(0x302401) //
			.text("(XT) PLC Trip Sequenced")),
	STATE_PLC_TRIP_SEQUENCED_MIO30D_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false).code(0x303401) //
			.text("(XT) PLC Trip Sequenced")),
	STATE_PLC_TRIP_SEQUENCED_MIO30D_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false).code(0x304401) //
			.text("(XT) PLC Trip Sequenced")),
	STATE_VBE_TRIP_FAST_MIO30D_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x301402) //
			.text("(XT) VBE Trip Fast")),
	STATE_VBE_TRIP_FAST_MIO30D_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x302402) //
			.text("(XT) VBE Trip Fast")),
	STATE_VBE_TRIP_FAST_MIO30D_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x303402) //
			.text("(XT) VBE Trip Fast")),
	STATE_VBE_TRIP_FAST_MIO30D_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x304402) //
			.text("(XT) VBE Trip Fast")),
	STATE_VBE_TRIP_SEQUENCED_MIO30D_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false).code(0x301403) //
			.text("(XT) VBE Trip Sequenced")),
	STATE_VBE_TRIP_SEQUENCED_MIO30D_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false).code(0x302403) //
			.text("(XT) VBE Trip Sequenced")),
	STATE_VBE_TRIP_SEQUENCED_MIO30D_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false).code(0x303403) //
			.text("(XT) VBE Trip Sequenced")),
	STATE_VBE_TRIP_SEQUENCED_MIO30D_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false).code(0x304403) //
			.text("(XT) VBE Trip Sequenced")),
	STATE_KZ04_TRIP_FAST_MIO30D_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x301404) //
			.text("(XT) KZ04 Trip Fast ")),
	STATE_KZ04_TRIP_FAST_MIO30D_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x302404) //
			.text("(XT) KZ04 Trip Fast ")),
	STATE_KZ04_TRIP_FAST_MIO30D_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x303404) //
			.text("(XT) KZ04 Trip Fast ")),
	STATE_KZ04_TRIP_FAST_MIO30D_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x304404) //
			.text("(XT) KZ04 Trip Fast ")),
	STATE_VOLTAGE_TRIP_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x101100) //
			.text("AC Supply Trip")),
	STATE_VOLTAGE_TRIP_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x102100) //
			.text("AC Supply Trip")),
	STATE_VOLTAGE_TRIP_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x103100) //
			.text("AC Supply Trip")),
	STATE_VOLTAGE_TRIP_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x104100) //
			.text("AC Supply Trip")),
	STATE_WATCHDOG_TRIP_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x101109) //
			.text("Watchdog Timeout")),
	STATE_WATCHDOG_TRIP_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x102109) //
			.text("Watchdog Timeout")),
	STATE_WATCHDOG_TRIP_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x103109) //
			.text("Watchdog Timeout")),
	STATE_WATCHDOG_TRIP_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x104109) //
			.text("Watchdog Timeout")),
	STATE_CURRENT_LOOP_TRIP_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10110A) //
			.text("CurrentLoop Open")),
	STATE_CURRENT_LOOP_TRIP_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10210A) //
			.text("CurrentLoop Open")),
	STATE_CURRENT_LOOP_TRIP_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10310A) //
			.text("CurrentLoop Open")),
	STATE_CURRENT_LOOP_TRIP_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10410A) //
			.text("CurrentLoop Open")),
	STATE_INTERN_TEMP_LOW_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x101400).text("(XT) Warning: Internal Temperature Low")),
	STATE_INTERN_TEMP_LOW_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x102400).text("(XT) Warning: Internal Temperature Low")),
	STATE_INTERN_TEMP_LOW_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x103400).text("(XT) Warning: Internal Temperature Low")),
	STATE_INTERN_TEMP_LOW_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x104400).text("(XT) Warning: Internal Temperature Low")),
	STATE_INTERN_TEMP_HIGH_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x101401).text("(XT) Warning: Internal Temperature High")),
	STATE_INTERN_TEMP_HIGH_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x102401).text("(XT) Warning: Internal Temperature High")),
	STATE_INTERN_TEMP_HIGH_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x103401).text("(XT) Warning: Internal Temperature High")),
	STATE_INTERN_TEMP_HIGH_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x104401).text("(XT) Warning: Internal Temperature High")),
	STATE_VOLT_1_LOW_WARNING_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x101402).text("(XT) Warning: Voltage Low Phase 1")),
	STATE_VOLT_1_LOW_WARNING_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x102402).text("(XT) Warning: Voltage Low Phase 1")),
	STATE_VOLT_1_LOW_WARNING_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x103402).text("(XT) Warning: Voltage Low Phase 1")),
	STATE_VOLT_1_LOW_WARNING_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x104402).text("(XT) Warning: Voltage Low Phase 1")),
	STATE_VOLT_2_LOW_WARNING_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x101403).text("(XT) Warning: Voltage Low Phase 2")),
	STATE_VOLT_2_LOW_WARNING_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x102403).text("(XT) Warning: Voltage Low Phase 2")),
	STATE_VOLT_2_LOW_WARNING_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x103403).text("(XT) Warning: Voltage Low Phase 2")),
	STATE_VOLT_2_LOW_WARNING_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x104403).text("(XT) Warning: Voltage Low Phase 2")),
	STATE_VOLT_3_LOW_WARNING_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x101404).text("(XT) Warning: Voltage Low Phase 3")),
	STATE_VOLT_3_LOW_WARNING_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x102404).text("(XT) Warning: Voltage Low Phase 3")),
	STATE_VOLT_3_LOW_WARNING_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x103404).text("(XT) Warning: Voltage Low Phase 3")),
	STATE_VOLT_3_LOW_WARNING_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x104404).text("(XT) Warning: Voltage Low Phase 3")),
	STATE_VOLT_1_HIGH_WARNING_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x101405).text("(XT) Warning: Voltage High Phase 1")),
	STATE_VOLT_1_HIGH_WARNING_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x102405).text("(XT) Warning: Voltage High Phase 1")),
	STATE_VOLT_1_HIGH_WARNING_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x103405).text("(XT) Warning: Voltage High Phase 1")),
	STATE_VOLT_1_HIGH_WARNING_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x104405).text("(XT) Warning: Voltage High Phase 1")),
	STATE_VOLT_2_HIGH_WARNING_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x101406).text("(XT) Warning: Voltage High Phase 2")),
	STATE_VOLT_2_HIGH_WARNING_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x102406).text("(XT) Warning: Voltage High Phase 2")),
	STATE_VOLT_2_HIGH_WARNING_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x103406).text("(XT) Warning: Voltage High Phase 2")),
	STATE_VOLT_2_HIGH_WARNING_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x104406).text("(XT) Warning: Voltage High Phase 2")),
	STATE_VOLT_3_HIGH_WARNING_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x101407).text("(XT) Warning: Voltage High Phase 3")),
	STATE_VOLT_3_HIGH_WARNING_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x102407).text("(XT) Warning: Voltage High Phase 3")),
	STATE_VOLT_3_HIGH_WARNING_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x103407).text("(XT) Warning: Voltage High Phase 3")),
	STATE_VOLT_3_HIGH_WARNING_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x104407).text("(XT) Warning: Voltage High Phase 3")),
	STATE_VOLT_1_THD_WARNING_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x101408).text("(XT) Warning: Voltage THD High Phase 1")),
	STATE_VOLT_1_THD_WARNING_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x102408).text("(XT) Warning: Voltage THD High Phase 1")),
	STATE_VOLT_1_THD_WARNING_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x103408).text("(XT) Warning: Voltage THD High Phase 1")),
	STATE_VOLT_1_THD_WARNING_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x104408).text("(XT) Warning: Voltage THD High Phase 1")),
	STATE_VOLT_2_THD_WARNING_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x101409).text("(XT) Warning: Voltage THD High Phase 2")),
	STATE_VOLT_2_THD_WARNING_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x102409).text("(XT) Warning: Voltage THD High Phase 2")),
	STATE_VOLT_2_THD_WARNING_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x103409).text("(XT) Warning: Voltage THD High Phase 2")),
	STATE_VOLT_2_THD_WARNING_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x104409).text("(XT) Warning: Voltage THD High Phase 2")),
	STATE_VOLT_3_THD_WARNING_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x10140A).text("(XT) Warning: Voltage THD High Phase 3")),
	STATE_VOLT_3_THD_WARNING_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x10240A).text("(XT) Warning: Voltage THD High Phase 3")),
	STATE_VOLT_3_THD_WARNING_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x10340A).text("(XT) Warning: Voltage THD High Phase 3")),
	STATE_VOLT_3_THD_WARNING_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x10440A).text("(XT) Warning: Voltage THD High Phase 3")),
	STATE_VOLT_1_LOW_TRIP_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10140B) //
			.text("(XT) Voltage Low Trip Phase 1")),
	STATE_VOLT_1_LOW_TRIP_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10240B) //
			.text("(XT) Voltage Low Trip Phase 1")),
	STATE_VOLT_1_LOW_TRIP_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10340B) //
			.text("(XT) Voltage Low Trip Phase 1")),
	STATE_VOLT_1_LOW_TRIP_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10440B) //
			.text("(XT) Voltage Low Trip Phase 1")),
	STATE_VOLT_2_LOW_TRIP_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10140C) //
			.text("(XT) Voltage Low Trip Phase 2")),
	STATE_VOLT_2_LOW_TRIP_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10240C) //
			.text("(XT) Voltage Low Trip Phase 2")),
	STATE_VOLT_2_LOW_TRIP_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10340C) //
			.text("(XT) Voltage Low Trip Phase 2")),
	STATE_VOLT_2_LOW_TRIP_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10440C) //
			.text("(XT) Voltage Low Trip Phase 2")),
	STATE_VOLT_3_LOW_TRIP_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10140D) //
			.text("(XT) Voltage Low Trip Phase 3")),
	STATE_VOLT_3_LOW_TRIP_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10240D) //
			.text("(XT) Voltage Low Trip Phase 3")),
	STATE_VOLT_3_LOW_TRIP_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10340D) //
			.text("(XT) Voltage Low Trip Phase 3")),
	STATE_VOLT_3_LOW_TRIP_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10440D) //
			.text("(XT) Voltage Low Trip Phase 3")),
	STATE_VOLT_1_HIGH_TRIP_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10140E) //
			.text("(XT) Voltage High Trip Phase 1")),
	STATE_VOLT_1_HIGH_TRIP_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10240E) //
			.text("(XT) Voltage High Trip Phase 1")),
	STATE_VOLT_1_HIGH_TRIP_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10340E) //
			.text("(XT) Voltage High Trip Phase 1")),
	STATE_VOLT_1_HIGH_TRIP_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10440E) //
			.text("(XT) Voltage High Trip Phase 1")),
	STATE_VOLT_2_HIGH_TRIP_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10140F) //
			.text("(XT) Voltage High Trip Phase 2")),
	STATE_VOLT_2_HIGH_TRIP_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10240F) //
			.text("(XT) Voltage High Trip Phase 2")),
	STATE_VOLT_2_HIGH_TRIP_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10340F) //
			.text("(XT) Voltage High Trip Phase 2")),
	STATE_VOLT_2_HIGH_TRIP_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10440F) //
			.text("(XT) Voltage High Trip Phase 2")),
	STATE_VOLT_3_HIGH_TRIP_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x101410) //
			.text("(XT) Voltage High Trip Phase 3")),
	STATE_VOLT_3_HIGH_TRIP_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x102410) //
			.text("(XT) Voltage High Trip Phase 3")),
	STATE_VOLT_3_HIGH_TRIP_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x103410) //
			.text("(XT) Voltage High Trip Phase 3")),
	STATE_VOLT_3_HIGH_TRIP_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x104410) //
			.text("(XT) Voltage High Trip Phase 3")),
	STATE_VOLT_1_THD_TRIP_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x101411) //
			.text("(XT) Voltage THD Trip Phase 1")),
	STATE_VOLT_1_THD_TRIP_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x102411) //
			.text("(XT) Voltage THD Trip Phase 1")),
	STATE_VOLT_1_THD_TRIP_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x103411) //
			.text("(XT) Voltage THD Trip Phase 1")),
	STATE_VOLT_1_THD_TRIP_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x104411) //
			.text("(XT) Voltage THD Trip Phase 1")),
	STATE_VOLT_2_THD_TRIP_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x101412) //
			.text("(XT) Voltage THD Trip Phase 2")),
	STATE_VOLT_2_THD_TRIP_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x102412) //
			.text("(XT) Voltage THD Trip Phase 2")),
	STATE_VOLT_2_THD_TRIP_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x103412) //
			.text("(XT) Voltage THD Trip Phase 2")),
	STATE_VOLT_2_THD_TRIP_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x104412) //
			.text("(XT) Voltage THD Trip Phase 2")),
	STATE_VOLT_3_THD_TRIP_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x101413) //
			.text("(XT) Voltage THD Trip Phase 3")),
	STATE_VOLT_3_THD_TRIP_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x102413) //
			.text("(XT) Voltage THD Trip Phase 3")),
	STATE_VOLT_3_THD_TRIP_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x103413) //
			.text("(XT) Voltage THD Trip Phase 3")),
	STATE_VOLT_3_THD_TRIP_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.FORCED) //
			.needsHardReset(false) //
			.code(0x104413) //
			.text("(XT) Voltage THD Trip Phase 3")),
	STATE_FREQUENCY_LOW_TRIP_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x101414) //
			.text("(XT) Frequency Low Trip")),
	STATE_FREQUENCY_LOW_TRIP_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x102414) //
			.text("(XT) Frequency Low Trip")),
	STATE_FREQUENCY_LOW_TRIP_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x103414) //
			.text("(XT) Frequency Low Trip")),
	STATE_FREQUENCY_LOW_TRIP_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x104414) //
			.text("(XT) Frequency Low Trip")),
	STATE_FREQUENCY_HIGH_TRIP_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x101415) //
			.text("(XT) Frequency High Trip")),
	STATE_FREQUENCY_HIGH_TRIP_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x102415) //
			.text("(XT) Frequency High Trip")),
	STATE_FREQUENCY_HIGH_TRIP_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x103415) //
			.text("(XT) Frequency High Trip")),
	STATE_FREQUENCY_HIGH_TRIP_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x104415) //
			.text("(XT) Frequency High Trip")),
	STATE_PHASE_ORDER_TRIP_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x101416) //
			.text("(XT) Invalid Phase Order Trip")),
	STATE_PHASE_ORDER_TRIP_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x102416) //
			.text("(XT) Invalid Phase Order Trip")),
	STATE_PHASE_ORDER_TRIP_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x103416) //
			.text("(XT) Invalid Phase Order Trip")),
	STATE_PHASE_ORDER_TRIP_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.RESTART) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x104416) //
			.text("(XT) Invalid Phase Order Trip")),
	STATE_VOLT_1_SRD_WARNING_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x101417).text("(XT) Selective Resonance Detection (SRD) Phase 1")),
	STATE_VOLT_1_SRD_WARNING_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x102417).text("(XT) Selective Resonance Detection (SRD) Phase 1")),
	STATE_VOLT_1_SRD_WARNING_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x103417).text("(XT) Selective Resonance Detection (SRD) Phase 1")),
	STATE_VOLT_1_SRD_WARNING_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x104417).text("(XT) Selective Resonance Detection (SRD) Phase 1")),
	STATE_VOLT_2_SRD_WARNING_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x101418).text("(XT) Selective Resonance Detection (SRD) Phase 2")),
	STATE_VOLT_2_SRD_WARNING_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x102418).text("(XT) Selective Resonance Detection (SRD) Phase 2")),
	STATE_VOLT_2_SRD_WARNING_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x103418).text("(XT) Selective Resonance Detection (SRD) Phase 2")),
	STATE_VOLT_2_SRD_WARNING_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x104418).text("(XT) Selective Resonance Detection (SRD) Phase 2")),
	STATE_VOLT_3_SRD_WARNING_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x101419).text("(XT) Selective Resonance Detection (SRD) Phase 3")),
	STATE_VOLT_3_SRD_WARNING_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x102419).text("(XT) Selective Resonance Detection (SRD) Phase 3")),
	STATE_VOLT_3_SRD_WARNING_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x103419).text("(XT) Selective Resonance Detection (SRD) Phase 3")),
	STATE_VOLT_3_SRD_WARNING_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.AUTO_ACKNOWLEDGE) //
			.reactionLevel(ReactionLevel.WARNING) //
			.needsHardReset(false) //
			.code(0x104419).text("(XT) Selective Resonance Detection (SRD) Phase 3")),
	STATE_FREQUENCY_DFDT_TRIP_MIO_1(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10141A) //
			.text("(XT) Frequency Change Rate Trip")),
	STATE_FREQUENCY_DFDT_TRIP_MIO_2(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10241A) //
			.text("(XT) Frequency Change Rate Trip")),
	STATE_FREQUENCY_DFDT_TRIP_MIO_3(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10341A) //
			.text("(XT) Frequency Change Rate Trip")),
	STATE_FREQUENCY_DFDT_TRIP_MIO_4(new ErrorDoc(Level.WARNING) //
			.acknowledge(Acknowledge.UNDEFINED) //
			.reactionLevel(ReactionLevel.SHUTDOWN) //
			.needsHardReset(false) //
			.code(0x10441A).text("(XT) Frequency Change Rate Trip"));

	private final Doc doc;

	private ErrorCodeChannelId0(Doc doc) {
		this.doc = doc;
	}

	public Doc doc() {
		return this.doc;
	}

}
