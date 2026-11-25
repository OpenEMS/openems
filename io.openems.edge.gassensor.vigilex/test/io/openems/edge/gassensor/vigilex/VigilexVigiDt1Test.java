package io.openems.edge.gassensor.vigilex;

import org.junit.jupiter.api.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.gassensor.vigilex.VigilexVigiDt1.ChannelId;

public class VigilexVigiDt1Test {

	@Test
	public void testActivate() throws Exception {
		new ComponentTest(new VigilexVigiDt1Impl()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("gasSensor0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.build()) //
				.next(new TestCase() //
						// all configured setpoints are written to the device on every cycle
						.output(ChannelId.HYDROGEN_RE1_LOW_SETPOINT, 500) //
						.output(ChannelId.HYDROGEN_RE1_HIGH_SETPOINT, 750) //
						.output(ChannelId.HYDROGEN_RE2_LOW_SETPOINT, 1250) //
						.output(ChannelId.HYDROGEN_RE2_HIGH_SETPOINT, 2000) //
						.output(ChannelId.VOC_RE1_LOW_SETPOINT, 150) //
						.output(ChannelId.VOC_RE1_HIGH_SETPOINT, 200) //
						.output(ChannelId.VOC_RE2_LOW_SETPOINT, 250) //
						.output(ChannelId.VOC_RE2_HIGH_SETPOINT, 400) //
						// relay delays are not written from config
						.output(ChannelId.RE1_DELAY, null) //
						.output(ChannelId.RE2_DELAY, null)) //
				.deactivate();
	}

	@Test
	public void testInvalidSetpoints() throws Exception {
		new ComponentTest(new VigilexVigiDt1Impl()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("gasSensor0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setHydrogenRe1LowSetpoint(1000) // low >= high -> invalid pair, not written
						.setHydrogenRe1HighSetpoint(500) //
						.setHydrogenRe2LowSetpoint(100) // out of range 500...2500 -> not written
						.setHydrogenRe2HighSetpoint(200) //
						.build()) //
				.next(new TestCase() //
						// invalid H2 RE1 pair (low >= high) is skipped
						.output(ChannelId.HYDROGEN_RE1_LOW_SETPOINT, null) //
						.output(ChannelId.HYDROGEN_RE1_HIGH_SETPOINT, null) //
						// out-of-range H2 RE2 pair is skipped
						.output(ChannelId.HYDROGEN_RE2_LOW_SETPOINT, null) //
						.output(ChannelId.HYDROGEN_RE2_HIGH_SETPOINT, null) //
						// valid VOC pairs are still written
						.output(ChannelId.VOC_RE1_LOW_SETPOINT, 150) //
						.output(ChannelId.VOC_RE1_HIGH_SETPOINT, 200) //
						.output(ChannelId.VOC_RE2_LOW_SETPOINT, 250) //
						.output(ChannelId.VOC_RE2_HIGH_SETPOINT, 400)) //
				.deactivate();
	}
}
