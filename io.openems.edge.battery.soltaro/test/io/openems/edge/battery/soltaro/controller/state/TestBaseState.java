package io.openems.edge.battery.soltaro.controller.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.edge.battery.soltaro.controller.helper.Creator;
import io.openems.edge.battery.soltaro.controller.helper.DummyBattery;
import io.openems.edge.battery.soltaro.controller.helper.DummyComponentManager;
import io.openems.edge.battery.soltaro.controller.helper.DummyEss;

public class TestBaseState {

	private BaseState sut;
	private static DummyComponentManager componentManager;
	private DummyEss ess;
	private DummyBattery bms;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		componentManager = new DummyComponentManager();
	}

	@Before
	public void setUp() throws Exception {
		// Always create ess newly to have an ess in "normal" situation that does
		// nothing
		componentManager.initEss();
		ess = componentManager.getComponent(Creator.ESS_ID);
		bms = componentManager.getComponent(Creator.BMS_ID);
		sut = Creator.createBaseState(ess, bms);
	}

	@Test
	public final void testBaseState() {
		assertNotNull(sut.getEss());
	}

	@Test
	public final void testDenyCharge() {
		((DummyEss) sut.getEss()).setCurrentActivePower((-1 * DummyEss.MAXIMUM_POWER));
		sut.denyCharge();
		assertTrue(((DummyEss) sut.getEss()).getCurrentActivePower() >= 0);
	}

	@Test
	public final void testDenyDischarge() {
		((DummyEss) sut.getEss()).setCurrentActivePower(DummyEss.MAXIMUM_POWER);
		sut.denyDischarge();
		assertTrue(((DummyEss) sut.getEss()).getCurrentActivePower() <= 0);
	}

	@Test
	public final void testChargeEssWithPercentOfMaxPower() {
		int percent = 20;
		sut.chargeEssWithPercentOfMaxPower(percent);
		assertEquals(percent * DummyEss.MAXIMUM_POWER * (-1) / 100, ((DummyEss) sut.getEss()).getCurrentActivePower());
	}

	@Test
	public final void testIsNextStateUndefined() {
		assertFalse(sut.isNextStateUndefined());

		bms.setMaximalCellTemperatureToUndefined();
		assertTrue(sut.isNextStateUndefined());

		bms.setMaximalCellTemperature(DummyBattery.DEFAULT_MAX_CELL_TEMPERATURE);
		assertFalse(sut.isNextStateUndefined());

		bms.setMinimalCellTemperatureToUndefined();
		assertTrue(sut.isNextStateUndefined());

		bms.setMinimalCellTemperature(DummyBattery.DEFAULT_MIN_CELL_TEMPERATURE);
		assertFalse(sut.isNextStateUndefined());

		bms.setMaximalCellVoltageToUndefined();
		assertTrue(sut.isNextStateUndefined());

		bms.setMaximalCellVoltage(DummyBattery.DEFAULT_MAX_CELL_VOLTAGE);
		assertFalse(sut.isNextStateUndefined());

		bms.setMinimalCellVoltageToUndefined();
		assertTrue(sut.isNextStateUndefined());

		bms.setMinimalCellVoltage(DummyBattery.DEFAULT_MIN_CELL_VOLTAGE);
		assertFalse(sut.isNextStateUndefined());

		bms.setSocToUndefined();
		assertTrue(sut.isNextStateUndefined());

		bms.setSoc(DummyBattery.DEFAULT_SOC);
		assertFalse(sut.isNextStateUndefined());

		sut = Creator.createBaseState(null, null);
		assertTrue(sut.isNextStateUndefined());

	}

	@Test
	public final void testGetBmsSoC() {
		assertEquals(DummyBattery.DEFAULT_SOC, sut.getBmsSoC());
	}

	@Test
	public final void testGetBmsMinCellTemperature() {
		assertEquals(DummyBattery.DEFAULT_MIN_CELL_TEMPERATURE, sut.getBmsMinCellTemperature());
	}

	@Test
	public final void testGetBmsMaxCellTemperature() {
		assertEquals(DummyBattery.DEFAULT_MAX_CELL_TEMPERATURE, sut.getBmsMaxCellTemperature());
	}

	@Test
	public final void testGetBmsMinCellVoltage() {
		assertEquals(DummyBattery.DEFAULT_MIN_CELL_VOLTAGE, sut.getBmsMinCellVoltage());
	}

	@Test
	public final void testGetBmsMaxCellVoltage() {
		assertEquals(DummyBattery.DEFAULT_MAX_CELL_VOLTAGE, sut.getBmsMaxCellVoltage());
	}

}
