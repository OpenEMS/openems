package io.openems.edge.meter.chint.ddsu666;

import static io.openems.common.types.MeterType.GRID;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.CURRENT_L1;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.FREQUENCY;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.VOLTAGE_L1;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;

public class MeterChintDdsu666ImplTest {

	private ComponentTest testBasis;

	@Before
	public void setup() throws OpenemsException, Exception {
		this.testBasis = new ComponentTest(new MeterChintDdsu666Impl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withRegisters(0x2000, //
								0x42F4, 0x0000, //
								0x40A0, 0x0000, //
								0x3FA0, 0x0000, //
								0x0000, 0x0000, 0x0000, 0x0000) //
						.withRegisters(0x200A, //
								0x0000, 0x0000, 0x0000, 0x0000, //
								0x4248, 0x0000, //
								0x0000, 0x0000) //
						.withRegisters(0x4000, //
								0x447A, 0x0000, //
								0x0000, 0x0000));
	}

	@Test
	public void testNonInvert() throws Exception {
		this.testBasis //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(2) //
						.setInvert(false) //
						.setType(GRID) //
						.build()) //
				.next(new TestCase() //
						.output(ACTIVE_POWER, 1250) //
						.output(FREQUENCY, 50000) //
						.output(VOLTAGE_L1, 122000) //
						.output(CURRENT_L1, 5000) //
						.output(ACTIVE_PRODUCTION_ENERGY, 1000000L) //
						.output(MeterChintDdsu666.ChannelId.ACTIVE_IMPORT_ENERGY, 1000000)) //
				.deactivate();
	}

	@Test
	public void testInvert() throws Exception {
		this.testBasis //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(2) //
						.setInvert(true) //
						.setType(GRID) //
						.build()) //
				.next(new TestCase() //
						.output(ACTIVE_POWER, -1250) //
						.output(ACTIVE_CONSUMPTION_ENERGY, 1000000L) //
						.output(MeterChintDdsu666.ChannelId.ACTIVE_IMPORT_ENERGY, 1000000)) //
				.deactivate();
	}
}
