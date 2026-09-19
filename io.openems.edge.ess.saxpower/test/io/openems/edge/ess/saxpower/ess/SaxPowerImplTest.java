package io.openems.edge.ess.saxpower.ess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.openems.common.channel.AccessMode;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.ess.api.SymmetricEss;

public class SaxPowerImplTest {

	@Test
	public void testTimeoutIsPushedToDevice() throws Exception {
		var sut = new SaxPowerImpl();
		new ComponentTest(sut) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(100) //
						.setTimeout(120) //
						.build())
				.next(new TestCase() //
						.input(SaxPower.ChannelId.REFERENCE_MAXIMUM_POWER, 4600));

		sut.applyPower(0, 0);

		assertEquals(Integer.valueOf(120), sut.getTimeoutChannel().getNextWriteValue().get());
		assertEquals(Integer.valueOf(1), sut.getControlModeChannel().getNextWriteValue().get());
		sut.deactivate();
	}

	@Test
	public void testApplyPower() throws Exception {
		var sut = new SaxPowerImpl();
		new ComponentTest(sut) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(100) //
						.build()) //
				.next(new TestCase() //
						.input(SaxPower.ChannelId.REFERENCE_MAXIMUM_POWER, 4600));

		sut.applyPower(1000, 0);
		assertEquals(Integer.valueOf(2173), sut.getTargetPowerChannel().getNextWriteValue().get());

		sut.applyPower(4600, 0);
		assertEquals(Integer.valueOf(10000), sut.getTargetPowerChannel().getNextWriteValue().get());

		sut.applyPower(-1000, 0);
		assertEquals(Integer.valueOf(-2173), sut.getTargetPowerChannel().getNextWriteValue().get());

		sut.deactivate();
	}

	@Test
	public void testDebugLog() throws Exception {
		var sut = new SaxPowerImpl();
		new ComponentTest(sut) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(100) //
						.build()) //
				.next(new TestCase() //
						.input(SymmetricEss.ChannelId.SOC, 80) //
						.input(SymmetricEss.ChannelId.ACTIVE_POWER, 1500));

		assertEquals("SoC:80 %|L:1500 W", sut.debugLog());

		sut.deactivate();
	}

	@Test
	public void testGetModbusSlaveTable() throws Exception {
		var sut = new SaxPowerImpl();
		new ComponentTest(sut) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(100) //
						.build());

		assertNotNull(sut.getModbusSlaveTable(AccessMode.READ_ONLY));
		assertNotNull(sut.getModbusSlaveTable(AccessMode.READ_WRITE));
		assertNotNull(sut.getModbusSlaveTable(AccessMode.WRITE_ONLY));

		sut.deactivate();
	}
}
