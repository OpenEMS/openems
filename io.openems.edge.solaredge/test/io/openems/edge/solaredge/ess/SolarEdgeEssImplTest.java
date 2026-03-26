package io.openems.edge.solaredge.ess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.function.ThrowingRunnable;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyCycle;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.solaredge.charger.SolarEdgeChargerImpl;
import io.openems.edge.solaredge.enums.ControlMode;

public class SolarEdgeEssImplTest {

	private static final int CYCLE_TIME = 1000;

	@Test
	public void testCalculateAndSetActualPvPower() throws Exception {
		final ThrowingRunnable<Exception> sleep = () -> Thread.sleep(CYCLE_TIME);

		var charger = new SolarEdgeChargerImpl();
		new ComponentTest(charger) //
		.addReference("cm", new DummyConfigurationAdmin()) //
		.addReference("essInverter", new SolarEdgeEssImpl())
		.activate(io.openems.edge.solaredge.charger.MyConfig.create() //
				.setId("charger0") //
				.setEssInverterId("ess0") //
				.build());

		var ess = new SolarEdgeEssImpl();
		ess.addCharger(charger);
		final var componentTest = new ComponentTest(ess) //
				.addReference("cycle", new DummyCycle(CYCLE_TIME)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(charger) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setControlMode(ControlMode.SMART) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPhase(SingleOrAllPhase.L1) //
						.build());

		// Cycle 1
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.INVERTER_ACTIVE_DC_POWER, 1500);
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.BATTERY1_ACTUAL_POWER, 400);

		componentTest.next(new TestCase() //
				.onAfterProcessImage(sleep));

		// pvProduction of 1500+400 = 1900 added to pvProductionAverageCalculator -> pvProduction average = 1900
		assertEquals(1900, charger.getActualPowerChannel().getNextValue().get().intValue());


		// Cycle 2
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.INVERTER_ACTIVE_DC_POWER, 3500);
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.BATTERY1_ACTUAL_POWER, 600);

		componentTest.next(new TestCase() //
				.onAfterProcessImage(sleep));

		// pvProduction of 1500+600 = 2100 added to pvProductionAverageCalculator -> pvProduction average = 2000
		assertEquals(2000, charger.getActualPowerChannel().getNextValue().get().intValue());


		// Cycle 3
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.INVERTER_ACTIVE_DC_POWER, 2200);
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.BATTERY1_ACTUAL_POWER, 300);

		componentTest.next(new TestCase() //
				.onAfterProcessImage(sleep));

		// pvProduction of 3500+300 = 3800 added to pvProductionAverageCalculator -> pvProduction average = 2600
		assertEquals(2600, charger.getActualPowerChannel().getNextValue().get().intValue());


		// Cycle 4 (add pvProduction of 2500+2000 = 4500 to pvProductionAverageCalculator)
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.INVERTER_ACTIVE_DC_POWER, 2800);
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.BATTERY1_ACTUAL_POWER, 900);

		componentTest.next(new TestCase() //
				.onAfterProcessImage(sleep));

		// pvProduction of 2200+900 = 3100 added to pvProductionAverageCalculator -> pvProduction average = 2725
		assertEquals(2725, charger.getActualPowerChannel().getNextValue().get().intValue());

		componentTest.deactivate();
	}

	@Test
	public void testOnSunSpecInitializationCompleted() throws Exception {
		var ess = new SolarEdgeEssImpl();
		final StateChannel warningWrongPhaseConfigured = ess.channel(SolarEdgeEss.ChannelId.WRONG_PHASE_CONFIGURED);

		// Test SinglePhase Inverter with SingleOrAllPhase.L1 configuration
		new ComponentTest(ess) //
				.addReference("cycle", new DummyCycle(CYCLE_TIME)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setControlMode(ControlMode.SMART) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPhase(SingleOrAllPhase.L1) //
						.build()) //
				.next(new TestCase().also(t -> {
					ess.addBlock(1, SolarEdgeEssImpl.S_101_WITHOUT_EVENTS, Priority.HIGH);
					ess.onSunSpecInitializationCompleted();
					assertFalse(warningWrongPhaseConfigured.getNextValue().get());
				}))
				.deactivate();

		// Test SinglePhase Inverter with SingleOrAllPhase.ALL configuration
		new ComponentTest(ess) //
				.addReference("cycle", new DummyCycle(CYCLE_TIME)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setControlMode(ControlMode.SMART) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPhase(SingleOrAllPhase.ALL) //
						.build()) //
				.next(new TestCase().also(t -> {
							ess.addBlock(1, SolarEdgeEssImpl.S_101_WITHOUT_EVENTS, Priority.HIGH);
							ess.onSunSpecInitializationCompleted();
							assertTrue(warningWrongPhaseConfigured.getNextValue().get());
				}))
				.deactivate();

		// Test SinglePhase Inverter with SingleOrAllPhase.L2 configuration
		new ComponentTest(ess) //
				.addReference("cycle", new DummyCycle(CYCLE_TIME)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setControlMode(ControlMode.SMART) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPhase(SingleOrAllPhase.L2) //
						.build()) //
				.next(new TestCase().also(t -> {
					ess.addBlock(1, SolarEdgeEssImpl.S_101_WITHOUT_EVENTS, Priority.HIGH);
					ess.onSunSpecInitializationCompleted();
					assertFalse(warningWrongPhaseConfigured.getNextValue().get());
				}))
				.deactivate();

		// Test SplitPhase Inverter with SingleOrAllPhase.L3 configuration (SplitPhase not supported -> Warning are expected)
		new ComponentTest(ess) //
				.addReference("cycle", new DummyCycle(CYCLE_TIME)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setControlMode(ControlMode.SMART) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPhase(SingleOrAllPhase.L3) //
						.build()) //
				.next(new TestCase().also(t -> {
					ess.addBlock(1, SolarEdgeEssImpl.S_102_WITHOUT_EVENTS, Priority.HIGH);
					ess.onSunSpecInitializationCompleted();
					assertTrue(warningWrongPhaseConfigured.getNextValue().get());
				}))
				.deactivate();

		// Test ThreePhase Inverter with SingleOrAllPhase.ALL configuration
		new ComponentTest(ess) //
				.addReference("cycle", new DummyCycle(CYCLE_TIME)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setControlMode(ControlMode.SMART) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPhase(SingleOrAllPhase.ALL) //
						.build()) //
				.next(new TestCase().also(t -> {
					ess.addBlock(1, SolarEdgeEssImpl.S_103_WITHOUT_EVENTS, Priority.HIGH);
					ess.onSunSpecInitializationCompleted();
					assertFalse(warningWrongPhaseConfigured.getNextValue().get());
				}))
				.deactivate();
	}

	@Test
	public void testIgnoreImpossibleMinPower() {
		assertEquals(0, (int) SolarEdgeEssImpl.ignoreImpossibleMinPower(25));
		assertEquals(75, (int) SolarEdgeEssImpl.ignoreImpossibleMinPower(75));
	}

	@Test
	public void testGetPowerPrecision() throws Exception {
		var ess = new SolarEdgeEssImpl();
		assertEquals(1, ess.getPowerPrecision());
	}

	@Test
	public void testGetPhaseL1() throws Exception {
		var ess = new SolarEdgeEssImpl();
		new ComponentTest(ess) //
				.addReference("cycle", new DummyCycle(CYCLE_TIME)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setControlMode(ControlMode.SMART) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPhase(SingleOrAllPhase.L1) //
						.build()) //
				.next(new TestCase().also(t -> {
					assertEquals(SinglePhase.L1, ess.getPhase());
				}))
				.deactivate();
	}

	@Test
	public void testGetPhaseL2() throws Exception {
		var ess = new SolarEdgeEssImpl();
		new ComponentTest(ess) //
				.addReference("cycle", new DummyCycle(CYCLE_TIME)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setControlMode(ControlMode.SMART) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPhase(SingleOrAllPhase.L2) //
						.build()) //
				.next(new TestCase().also(t -> {
					assertEquals(SinglePhase.L2, ess.getPhase());
				}))
				.deactivate();
	}

	@Test
	public void testGetPhaseL3() throws Exception {
		var ess = new SolarEdgeEssImpl();
		new ComponentTest(ess) //
				.addReference("cycle", new DummyCycle(CYCLE_TIME)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setControlMode(ControlMode.SMART) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPhase(SingleOrAllPhase.L3) //
						.build()) //
				.next(new TestCase().also(t -> {
					assertEquals(SinglePhase.L3, ess.getPhase());
				}))
				.deactivate();
	}

	@Test
	public void testGetPhaseAll() throws Exception {
		var ess = new SolarEdgeEssImpl();
		new ComponentTest(ess) //
				.addReference("cycle", new DummyCycle(CYCLE_TIME)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setControlMode(ControlMode.SMART) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPhase(SingleOrAllPhase.ALL) //
						.build()) //
				.next(new TestCase().also(t -> {
					assertNull(ess.getPhase());
				}))
				.deactivate();
	}

	@Test
	public void testSurplusPower() throws Exception {
		var charger = new SolarEdgeChargerImpl();
		new ComponentTest(charger) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("essInverter", new SolarEdgeEssImpl())
				.activate(io.openems.edge.solaredge.charger.MyConfig.create() //
						.setId("charger0") //
						.setEssInverterId("ess0") //
						.build());

		var ess = new SolarEdgeEssImpl();
		ess.addCharger(charger);

		// Test null on SOC < 99
		TestUtils.withValue(ess, SymmetricEss.ChannelId.SOC, 50);
		assertNull(ess.getSurplusPower());

		// Test null on productionPower null
		TestUtils.withValue(ess, SymmetricEss.ChannelId.SOC, 99);
		assertNull(ess.getSurplusPower());

		// Test null on productionPower < 100
		TestUtils.withValue(charger, EssDcCharger.ChannelId.ACTUAL_POWER, 75); // pvProduction
		assertNull(ess.getSurplusPower());

		// Test productionPower on productionPower >= 100
		TestUtils.withValue(charger, EssDcCharger.ChannelId.ACTUAL_POWER, 100); // pvProduction
		assertEquals(100, (int) ess.getSurplusPower());
	}

	@Test
	public void testApplyPvExportLimit() throws Exception {
		var ess = new SolarEdgeEssImpl();
		final var componentTest = new ComponentTest(ess) //
				.addReference("cycle", new DummyCycle(CYCLE_TIME)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setControlMode(ControlMode.SMART) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPhase(SingleOrAllPhase.L1) //
						.setPvExportLimit(true) //
						.build()) //
				.next(new TestCase());

		final IntegerWriteChannel activeExportPowerLimitChannel = ess.channel(SolarEdgeEss.ChannelId.ACTIVE_EXPORT_POWER_LIMIT);
		final FloatWriteChannel wMaxLimPwrChannel = ess.channel(SolarEdgeEss.ChannelId.EXPORT_CONTROL_SITE_LIMIT);
		final StateChannel warningPvExportLimitDisabled = ess.channel(SolarEdgeEss.ChannelId.DISABLED_PV_EXPORT_LIMIT_FAILED);
		final StateChannel warningPvExportLimitFailed = ess.channel(SolarEdgeEss.ChannelId.PV_EXPORT_LIMIT_FAILED);

		activeExportPowerLimitChannel.setNextWriteValue(5000);
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.EXPORT_CONTROL_MODE, 1);
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.ADVANCED_PWR_CONTROL_EN, 1);

		ess.applyPvExportLimit();
		assertFalse(warningPvExportLimitDisabled.getNextValue().get());
		assertFalse(warningPvExportLimitFailed.getNextValue().get());
		assertEquals(5000, wMaxLimPwrChannel.getNextWriteValue().get().intValue());

		componentTest.deactivate();
	}

	@Test
	public void testApplyPvExportLimitClampsNegativeValuesToZero() throws Exception {
		var ess = new SolarEdgeEssImpl();
		final var componentTest = new ComponentTest(ess) //
				.addReference("cycle", new DummyCycle(CYCLE_TIME)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setControlMode(ControlMode.SMART) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPhase(SingleOrAllPhase.L1) //
						.setPvExportLimit(true) //
						.build()) //
				.next(new TestCase());

		final IntegerWriteChannel activeExportPowerLimitChannel = ess.channel(SolarEdgeEss.ChannelId.ACTIVE_EXPORT_POWER_LIMIT);
		final FloatWriteChannel wMaxLimPwrChannel = ess.channel(SolarEdgeEss.ChannelId.EXPORT_CONTROL_SITE_LIMIT);
		final StateChannel warningPvExportLimitDisabled = ess.channel(SolarEdgeEss.ChannelId.DISABLED_PV_EXPORT_LIMIT_FAILED);
		final StateChannel warningPvExportLimitFailed = ess.channel(SolarEdgeEss.ChannelId.PV_EXPORT_LIMIT_FAILED);

		activeExportPowerLimitChannel.setNextWriteValue(-5000);
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.EXPORT_CONTROL_MODE, 1);
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.ADVANCED_PWR_CONTROL_EN, 1);

		ess.applyPvExportLimit();
		assertFalse(warningPvExportLimitDisabled.getNextValue().get());
		assertFalse(warningPvExportLimitFailed.getNextValue().get());
		assertEquals(0, wMaxLimPwrChannel.getNextWriteValue().get().intValue());

		componentTest.deactivate();
	}

	@Test
	public void testApplyPvExportLimitFailed() throws Exception {
		var ess = new SolarEdgeEssImpl();
		final var componentTest = new ComponentTest(ess) //
				.addReference("cycle", new DummyCycle(CYCLE_TIME)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setControlMode(ControlMode.SMART) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPhase(SingleOrAllPhase.L1) //
						.setPvExportLimit(true) //
						.build()) //
				.next(new TestCase());

		final IntegerWriteChannel activeExportPowerLimitChannel = ess.channel(SolarEdgeEss.ChannelId.ACTIVE_EXPORT_POWER_LIMIT);
		final StateChannel warningPvExportLimitDisabled = ess.channel(SolarEdgeEss.ChannelId.DISABLED_PV_EXPORT_LIMIT_FAILED);
		final StateChannel warningPvExportLimitFailed = ess.channel(SolarEdgeEss.ChannelId.PV_EXPORT_LIMIT_FAILED);

		activeExportPowerLimitChannel.setNextWriteValue(5000);
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.EXPORT_CONTROL_MODE, 1);
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.ADVANCED_PWR_CONTROL_EN, 0);

		ess.applyPvExportLimit();
		assertFalse(warningPvExportLimitDisabled.getNextValue().get());
		assertTrue(warningPvExportLimitFailed.getNextValue().get());

		componentTest.deactivate();
	}

	@Test
	public void testApplyPvExportLimitDisabled() throws Exception {
		var ess = new SolarEdgeEssImpl();
		final var componentTest = new ComponentTest(ess) //
				.addReference("cycle", new DummyCycle(CYCLE_TIME)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setControlMode(ControlMode.SMART) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setPhase(SingleOrAllPhase.L1) //
						.setPvExportLimit(false) //
						.build()) //
				.next(new TestCase());

		final IntegerWriteChannel activeExportPowerLimitChannel = ess.channel(SolarEdgeEss.ChannelId.ACTIVE_EXPORT_POWER_LIMIT);
		final StateChannel warningPvExportLimitDisabled = ess.channel(SolarEdgeEss.ChannelId.DISABLED_PV_EXPORT_LIMIT_FAILED);
		final StateChannel warningPvExportLimitFailed = ess.channel(SolarEdgeEss.ChannelId.PV_EXPORT_LIMIT_FAILED);

		activeExportPowerLimitChannel.setNextWriteValue(5000);
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.EXPORT_CONTROL_MODE, 1);
		TestUtils.withValue(ess, SolarEdgeEss.ChannelId.ADVANCED_PWR_CONTROL_EN, 1);

		ess.applyPvExportLimit();
		assertTrue(warningPvExportLimitDisabled.getNextValue().get());
		assertFalse(warningPvExportLimitFailed.getNextValue().get());

		componentTest.deactivate();
	}

	@Test
	public void testDebugLog() throws Exception {
		var ess = new SolarEdgeEssImpl();
		assertNotNull(ess.debugLog());
	}
}
