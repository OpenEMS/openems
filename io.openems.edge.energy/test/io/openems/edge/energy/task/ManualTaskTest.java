package io.openems.edge.energy.task;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

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

}
