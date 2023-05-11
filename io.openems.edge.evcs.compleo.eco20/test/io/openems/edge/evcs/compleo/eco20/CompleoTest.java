package io.openems.edge.evcs.compleo.eco20;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.core.timer.DummyTimerManager;
import io.openems.edge.core.timer.TimerManager;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.evcs.api.PhaseRotation;

public class CompleoTest {

    private static final String EVCS_ID = "evcs0";
    private static final String MODBUS_ID = "modbus0";
    private DummyComponentManager cpm;

    @Test
    public void test() throws Exception {
	this.cpm = new DummyComponentManager(new TimeLeapClock(Instant.ofEpochSecond(1577836800), ZoneOffset.UTC));
	TimerManager tm = new DummyTimerManager(this.cpm);
	new ComponentTest(new CompleoEco20Impl()) //
		.addReference("cm", new DummyConfigurationAdmin()) //
		.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
		.addReference("timerManager", tm).activate(MyConfig.create() //
			.setId(EVCS_ID) //
			.setModbusId(MODBUS_ID) //
			.setModbusUnitId(255) //
			.setModel(Model.COMPLEO_ECO_20) //
			.setMinHwCurrent(6000) //
			.setMaxHwCurrent(16000) //
			.setPhaseRotation(PhaseRotation.L1_L2_L3) //
			.setStartStopDelay(120) //
			.setHasIntegratedMeter(true) //
			.setRestartPilotSignal(false) //
			.setDebugMode(false) //
			.build()) //
		.next(new TestCase());
    }

}
