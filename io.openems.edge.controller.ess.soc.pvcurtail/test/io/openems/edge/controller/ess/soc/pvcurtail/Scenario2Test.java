package io.openems.edge.controller.ess.soc.pvcurtail;

import static io.openems.edge.controller.ess.soc.pvcurtail.Config.ChargePriority.BATTERY_FIRST;
import static io.openems.edge.controller.ess.soc.pvcurtail.Config.ChargePriority.EXPORT_FIRST;
import static io.openems.edge.controller.ess.soc.pvcurtail.Config.ChargeSource.GRID_ONLY;
import static io.openems.edge.controller.ess.soc.pvcurtail.Config.ChargeSource.PV_AND_GRID;
import static io.openems.edge.controller.ess.soc.pvcurtail.Config.ChargeSource.PV_ONLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

public class Scenario2Test {

	private static final int MW = 1_000_000;
	private static final int MAX_CHARGE = 1 * MW; // battery may take 1.0 MW
	private static final int GRID_SETTING = MW / 2; // 0.5 MW

	/** Steady state: the BESS already charges what it was told. */
	private static Scenario2.Inputs steady(int pvW, int bessW) {
		return new Scenario2.Inputs(pvW, bessW, 0, MAX_CHARGE, 4250000);
	}

	@Test
	public void testWindow() {
		var from = LocalTime.of(5, 30);
		var to = LocalTime.of(11, 35);
		assertTrue(Scenario2.inWindow(from, to, LocalTime.of(5, 30)));
		assertTrue(Scenario2.inWindow(from, to, LocalTime.of(11, 34)));
		assertFalse(Scenario2.inWindow(from, to, LocalTime.of(11, 35)));
		assertFalse(Scenario2.inWindow(from, from, LocalTime.of(6, 0)));
		// crosses midnight
		assertTrue(Scenario2.inWindow(LocalTime.of(22, 0), LocalTime.of(2, 0), LocalTime.of(1, 0)));
		assertFalse(Scenario2.inWindow(LocalTime.of(22, 0), LocalTime.of(2, 0), LocalTime.of(12, 0)));
		assertEquals(LocalTime.of(5, 30), Scenario2.parseTime("05:30"));
		assertEquals(LocalTime.of(5, 30), Scenario2.parseTime("5:30"));
		assertNull(Scenario2.parseTime("25:00"));
		assertNull(Scenario2.parseTime(""));
	}

	@Test
	public void testBaseExport() {
		assertEquals(GRID_SETTING, Scenario2.baseExportW(GRID_SETTING, PV_ONLY, GRID_SETTING));
		assertEquals(GRID_SETTING, Scenario2.baseExportW(GRID_SETTING, PV_ONLY, 2 * MW));
		assertEquals(GRID_SETTING, Scenario2.baseExportW(2 * MW, PV_ONLY, GRID_SETTING));
		assertEquals(0, Scenario2.baseExportW(800000, GRID_ONLY, 2 * MW));
	}

	@Test
	public void testChargeBatteryFirst() {
		// PV 1.8 MW available: limit = 0.5 export + 1.0 battery, the rest is curtailed
		var r = Scenario2.charge(steady(1500000, -MAX_CHARGE), PV_ONLY, BATTERY_FIRST, GRID_SETTING, 0, false, 20000);
		assertEquals(-MAX_CHARGE, r.bessW());
		assertEquals(1500000, r.pvLimitW());
		// PV 1.2 MW: battery 1.0, export 0.2
		r = Scenario2.charge(steady(1200000, -MAX_CHARGE), PV_ONLY, BATTERY_FIRST, GRID_SETTING, 0, false, 20000);
		assertEquals(-MAX_CHARGE, r.bessW());
		// PV 0.4 MW: battery takes all of it
		r = Scenario2.charge(steady(400000, -400000), PV_ONLY, BATTERY_FIRST, GRID_SETTING, 0, false, 20000);
		assertEquals(-400000, r.bessW());
	}

	@Test
	public void testChargeExportFirst() {
		// PV 1.2 MW: export 0.5, battery 0.7
		var r = Scenario2.charge(steady(1200000, -700000), PV_ONLY, EXPORT_FIRST, GRID_SETTING, 0, false, 20000);
		assertEquals(-700000, r.bessW());
		// PV 0.4 MW: everything exported, battery 0
		r = Scenario2.charge(steady(400000, 0), PV_ONLY, EXPORT_FIRST, GRID_SETTING, 0, false, 20000);
		assertEquals(0, r.bessW());
		// battery not charging yet: PV may only run ahead by the headroom
		r = Scenario2.charge(steady(500000, 0), PV_ONLY, EXPORT_FIRST, GRID_SETTING, 0, false, 20000);
		assertEquals(GRID_SETTING + 20000, r.pvLimitW());
	}

	@Test
	public void testChargePvAndGrid() {
		// PV 0.4 MW: battery 1.0, import 0.6
		var r = Scenario2.charge(steady(400000, -MAX_CHARGE), PV_AND_GRID, BATTERY_FIRST, GRID_SETTING, 2 * MW, false,
				20000);
		assertEquals(-MAX_CHARGE, r.bessW());
		// import limit 0.3 MW: battery 0.4 PV + 0.3 grid
		r = Scenario2.charge(steady(400000, -700000), PV_AND_GRID, BATTERY_FIRST, GRID_SETTING, 300000, false, 20000);
		assertEquals(-700000, r.bessW());
	}

	@Test
	public void testChargeGridOnlyAndSocLimit() {
		// GRID_ONLY: import 0.8 MW, PV curtailed
		var r = Scenario2.charge(steady(1200000, -800000), GRID_ONLY, BATTERY_FIRST, 800000, 2 * MW, false, 20000);
		assertEquals(-800000, r.bessW());
		assertEquals(0, r.pvLimitW());
		// GRID_ONLY: the import limit caps the grid setting
		r = Scenario2.charge(steady(0, -500000), GRID_ONLY, BATTERY_FIRST, 800000, 500000, false, 20000);
		assertEquals(-500000, r.bessW());
		// SoC limit reached: battery stops, export capped at the grid setting
		r = Scenario2.charge(steady(1800000, 0), PV_ONLY, BATTERY_FIRST, GRID_SETTING, 0, true, 20000);
		assertEquals(0, r.bessW());
		assertEquals(GRID_SETTING, r.pvLimitW());
	}

	@Test
	public void testDischarge() {
		// PV 0.5 MW, export 2.0 MW: battery 1.5 MW, PV may rise by the headroom
		var r = Scenario2.discharge(steady(500000, 1500000), 2 * MW, false, 20000);
		assertEquals(1500000, r.bessW());
		assertEquals(520000, r.pvLimitW());
		// PV above the export setting: battery 0, PV curtailed to exactly the export
		r = Scenario2.discharge(steady(1800000, 0), GRID_SETTING, false, 20000);
		assertEquals(0, r.bessW());
		assertEquals(GRID_SETTING, r.pvLimitW());
		// SoC limit reached
		r = Scenario2.discharge(steady(0, 0), 2 * MW, true, 20000);
		assertEquals(0, r.bessW());
	}

	@Test
	public void testBalance() {
		assertEquals(-MAX_CHARGE, Scenario2.balanceW(steady(1800000, 0), GRID_SETTING)); // wants 1.3 MW, may take 1.0
		assertEquals(GRID_SETTING, Scenario2.balanceW(steady(0, 0), GRID_SETTING));
	}
}
