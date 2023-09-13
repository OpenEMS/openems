package io.openems.edge.energy.task;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.energy.api.schedulable.Schedule.Preset;

public class ManualTaskTest {

	@Test
	public void testParseSchedules() throws OpenemsNamedException {
		var json = """
				{
				    "ctrlFixActivePower0": {
				        "presets": [
				            "OFF",
				            "OFF",
				            "FORCE_CHARGE_5000_W",
				            "FORCE_CHARGE_5000_W",
				            "FORCE_CHARGE_5000_W",
				            "OFF",
				            "OFF",
				            "OFF",
				            "FORCE_ZERO",
				            "FORCE_ZERO",
				            "OFF",
				            "OFF",
				            "OFF",
				            "OFF",
				            "OFF",
				            "OFF",
				            "OFF",
				            "OFF",
				            "OFF",
				            "OFF",
				            "OFF",
				            "OFF",
				            "OFF",
				            "FORCE_CHARGE_5000_W"
				        ]
				    },
				    "ctrlEvcs0": {
				        "presets": [
				            "OFF",
				            "OFF",
				            "FORCE_FAST_CHARGE",
				            "FORCE_FAST_CHARGE",
				            "FORCE_FAST_CHARGE",
				            "EXCESS_POWER",
				            "EXCESS_POWER",
				            "EXCESS_POWER",
				            "EXCESS_POWER",
				            "EXCESS_POWER",
				            "EXCESS_POWER",
				            "EXCESS_POWER",
				            "EXCESS_POWER",
				            "EXCESS_POWER",
				            "EXCESS_POWER",
				            "EXCESS_POWER",
				            "EXCESS_POWER",
				            "EXCESS_POWER",
				            "EXCESS_POWER",
				            "EXCESS_POWER",
				            "OFF",
				            "OFF",
				            "OFF",
				            "FORCE_FAST_CHARGE"
				        ]
				    }
				}""";

		var schedules = ManualTask.parseSchedules(json);
		var schedule0 = schedules.get("ctrlFixActivePower0");
		assertEquals(24, schedule0.length);
		assertEquals("OFF", schedule0[0]);
		assertEquals("FORCE_CHARGE_5000_W", schedule0[23]);

		var schedule1 = schedules.get("ctrlEvcs0");
		assertEquals(24, schedule1.length);
		assertEquals("OFF", schedule1[0]);
		assertEquals("FORCE_FAST_CHARGE", schedule1[23]);
	}

	private static enum HourlyPresets implements Preset {
		PRESET_0, PRESET_1, PRESET_2, PRESET_3, PRESET_4, PRESET_5, PRESET_6, PRESET_7, PRESET_8, PRESET_9, PRESET_10,
		PRESET_11, PRESET_12, PRESET_13, PRESET_14, PRESET_15, PRESET_16, PRESET_17, PRESET_18, PRESET_19, PRESET_20,
		PRESET_21, PRESET_22, PRESET_23
	}

	@Test
	public void testToScheduleHourly() throws OpenemsNamedException {
		var now = ZonedDateTime.of(2022, 06, 23, 10, 1, 2, 3, ZoneId.of("UTC"));
		var stringPresets = new String[] { "PRESET_0", "PRESET_1", "PRESET_2", "PRESET_3", "PRESET_4", "PRESET_5",
				"PRESET_6", "PRESET_7", "PRESET_8", "PRESET_9", "PRESET_10", "PRESET_11", "PRESET_12", "PRESET_13",
				"PRESET_14", "PRESET_15", "PRESET_16", "PRESET_17", "PRESET_18", "PRESET_19", "PRESET_20", "PRESET_21",
				"PRESET_22", "PRESET_23" };

		var s = ManualTask.toSchedule(now, stringPresets, HourlyPresets.values());
		assertTrue(s.toString().startsWith("2022-06-23T10:00Z[UTC]: PRESET_10"));
	}

	private static enum QuarterlyPresets implements Preset {
		PRESET_0_00, PRESET_0_15, PRESET_0_30, PRESET_0_45, //
		PRESET_1_00, PRESET_1_15, PRESET_1_30, PRESET_1_45, //
		PRESET_2_00, PRESET_2_15, PRESET_2_30, PRESET_2_45, //
		PRESET_3_00, PRESET_3_15, PRESET_3_30, PRESET_3_45, //
		PRESET_4_00, PRESET_4_15, PRESET_4_30, PRESET_4_45, //
		PRESET_5_00, PRESET_5_15, PRESET_5_30, PRESET_5_45, //
		PRESET_6_00, PRESET_6_15, PRESET_6_30, PRESET_6_45, //
		PRESET_7_00, PRESET_7_15, PRESET_7_30, PRESET_7_45, //
		PRESET_8_00, PRESET_8_15, PRESET_8_30, PRESET_8_45, //
		PRESET_9_00, PRESET_9_15, PRESET_9_30, PRESET_9_45, //
		PRESET_10_00, PRESET_10_15, PRESET_10_30, PRESET_10_45, //
		PRESET_11_00, PRESET_11_15, PRESET_11_30, PRESET_11_45, //
		PRESET_12_00, PRESET_12_15, PRESET_12_30, PRESET_12_45, //
		PRESET_13_00, PRESET_13_15, PRESET_13_30, PRESET_13_45, //
		PRESET_14_00, PRESET_14_15, PRESET_14_30, PRESET_14_45, //
		PRESET_15_00, PRESET_15_15, PRESET_15_30, PRESET_15_45, //
		PRESET_16_00, PRESET_16_15, PRESET_16_30, PRESET_16_45, //
		PRESET_17_00, PRESET_17_15, PRESET_17_30, PRESET_17_45, //
		PRESET_18_00, PRESET_18_15, PRESET_18_30, PRESET_18_45, //
		PRESET_19_00, PRESET_19_15, PRESET_19_30, PRESET_19_45, //
		PRESET_20_00, PRESET_20_15, PRESET_20_30, PRESET_20_45, //
		PRESET_21_00, PRESET_21_15, PRESET_21_30, PRESET_21_45, //
		PRESET_22_00, PRESET_22_15, PRESET_22_30, PRESET_22_45, //
		PRESET_23_00, PRESET_23_15, PRESET_23_30, PRESET_23_45
	}

	@Test
	public void testToScheduleQuarterly() throws OpenemsNamedException {
		var now = ZonedDateTime.of(2022, 06, 23, 15, 30, 1, 2, ZoneId.of("UTC"));
		var stringPresets = new String[] { //
				"PRESET_0_00", "PRESET_0_15", "PRESET_0_30", "PRESET_0_45", //
				"PRESET_1_00", "PRESET_1_15", "PRESET_1_30", "PRESET_1_45", //
				"PRESET_2_00", "PRESET_2_15", "PRESET_2_30", "PRESET_2_45", //
				"PRESET_3_00", "PRESET_3_15", "PRESET_3_30", "PRESET_3_45", //
				"PRESET_4_00", "PRESET_4_15", "PRESET_4_30", "PRESET_4_45", //
				"PRESET_5_00", "PRESET_5_15", "PRESET_5_30", "PRESET_5_45", //
				"PRESET_6_00", "PRESET_6_15", "PRESET_6_30", "PRESET_6_45", //
				"PRESET_7_00", "PRESET_7_15", "PRESET_7_30", "PRESET_7_45", //
				"PRESET_8_00", "PRESET_8_15", "PRESET_8_30", "PRESET_8_45", //
				"PRESET_9_00", "PRESET_9_15", "PRESET_9_30", "PRESET_9_45", //
				"PRESET_10_00", "PRESET_10_15", "PRESET_10_30", "PRESET_10_45", //
				"PRESET_11_00", "PRESET_11_15", "PRESET_11_30", "PRESET_11_45", //
				"PRESET_12_00", "PRESET_12_15", "PRESET_12_30", "PRESET_12_45", //
				"PRESET_13_00", "PRESET_13_15", "PRESET_13_30", "PRESET_13_45", //
				"PRESET_14_00", "PRESET_14_15", "PRESET_14_30", "PRESET_14_45", //
				"PRESET_15_00", "PRESET_15_15", "PRESET_15_30", "PRESET_15_45", //
				"PRESET_16_00", "PRESET_16_15", "PRESET_16_30", "PRESET_16_45", //
				"PRESET_17_00", "PRESET_17_15", "PRESET_17_30", "PRESET_17_45", //
				"PRESET_18_00", "PRESET_18_15", "PRESET_18_30", "PRESET_18_45", //
				"PRESET_19_00", "PRESET_19_15", "PRESET_19_30", "PRESET_19_45", //
				"PRESET_20_00", "PRESET_20_15", "PRESET_20_30", "PRESET_20_45", //
				"PRESET_21_00", "PRESET_21_15", "PRESET_21_30", "PRESET_21_45", //
				"PRESET_22_00", "PRESET_22_15", "PRESET_22_30", "PRESET_22_45", //
				"PRESET_23_00", "PRESET_23_15", "PRESET_23_30", "PRESET_23_45" };

		var s = ManualTask.toSchedule(now, stringPresets, QuarterlyPresets.values());
		assertTrue(s.toString().startsWith("2022-06-23T15:30Z[UTC]: PRESET_15_30"));
	}
}
