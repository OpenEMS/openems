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
		//Always create ess newly to have an ess in "normal" situation that does nothing
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
		((DummyEss)sut.getEss()).setCurrentActivePower( (-1 * DummyEss.MAXIMUM_POWER));
		sut.denyCharge();
		assertTrue(((DummyEss)sut.getEss()).getCurrentActivePower() >= 0 );
	}

	@Test
	public final void testDenyDischarge() {
		((DummyEss)sut.getEss()).setCurrentActivePower( (DummyEss.MAXIMUM_POWER));
		sut.denyDischarge();
		assertTrue(((DummyEss)sut.getEss()).getCurrentActivePower() <= 0 );
	}

	@Test
	public final void testChargeEssWithPercentOfMaxPower() {
		int percent = 20;
		sut.chargeEssWithPercentOfMaxPower(percent);
		assertEquals(percent * DummyEss.MAXIMUM_POWER * (-1) / 100, ((DummyEss)sut.getEss()).getCurrentActivePower());
	}

	@Test
	public final void testIsNextStateUndefined() {
		assertFalse(sut.isNextStateUndefined());
		
		ess.setMaximalCellTemperatureToUndefined();
		assertTrue(sut.isNextStateUndefined());
		
		ess.setMaximalCellTemperature(DummyEss.DEFAULT_MAX_CELL_TEMPERATURE);
		assertFalse(sut.isNextStateUndefined());
		
		ess.setMinimalCellTemperatureToUndefined();
		assertTrue(sut.isNextStateUndefined());
		
		ess.setMinimalCellTemperature(DummyEss.DEFAULT_MIN_CELL_TEMPERATURE);
		assertFalse(sut.isNextStateUndefined());
		
		ess.setMaximalCellVoltageToUndefined();
		assertTrue(sut.isNextStateUndefined());
		
		ess.setMaximalCellVoltage(DummyEss.DEFAULT_MAX_CELL_VOLTAGE);
		assertFalse(sut.isNextStateUndefined());
		
		ess.setMinimalCellVoltageToUndefined();
		assertTrue(sut.isNextStateUndefined());
		
		ess.setMinimalCellVoltage(DummyEss.DEFAULT_MIN_CELL_VOLTAGE);
		assertFalse(sut.isNextStateUndefined());
		
		ess.setSocToUndefined();
		assertTrue(sut.isNextStateUndefined());
		
		ess.setSoc(DummyEss.DEFAULT_SOC);
		assertFalse(sut.isNextStateUndefined());

		sut = Creator.createBaseState(null, null);
		assertTrue(sut.isNextStateUndefined());
		
	}

	@Test
	public final void testGetEssSoC() {
		assertEquals(DummyEss.DEFAULT_SOC, sut.getEssSoC());
	}

	@Test
	public final void testGetEssMinCellTemperature() {
		assertEquals(DummyEss.DEFAULT_MIN_CELL_TEMPERATURE, sut.getEssMinCellTemperature());
	}

	@Test
	public final void testGetEssMaxCellTemperature() {
		assertEquals(DummyEss.DEFAULT_MAX_CELL_TEMPERATURE, sut.getEssMaxCellTemperature());
	}

	@Test
	public final void testGetEssMinCellVoltage() {
		assertEquals(DummyEss.DEFAULT_MIN_CELL_VOLTAGE, sut.getEssMinCellVoltage());
	}

	@Test
	public final void testGetEssMaxCellVoltage() {
		assertEquals(DummyEss.DEFAULT_MAX_CELL_VOLTAGE, sut.getEssMaxCellVoltage());
	}

}
