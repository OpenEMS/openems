package io.openems.edge.meter.chint.ddsu666;

import static io.openems.common.types.MeterType.GRID;
import static io.openems.edge.common.type.Phase.SinglePhase.L1;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.ElectricityMeter;

public class MeterChintDdsu666ImplTest {

	private ComponentTest testBasis;

	@BeforeEach
	public void setup() throws OpenemsException, Exception {
		this.testBasis = new ComponentTest(new MeterChintDdsu666Impl()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withRegisters(0x0006, //
								0x0002) //
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
						.setPhase(L1) //
						.setType(GRID) //
						.build()) //
				.next(new TestCase() //
						.activateStrictMode() //
						.output(OpenemsComponent.ChannelId.STATE, Level.OK) //
						.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //
						.output(MeterChintDdsu666.ChannelId.COMMUNICATION_ADDRESS, 2) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 1000000L) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 1250) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1250) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, null) //
						.output(ElectricityMeter.ChannelId.CURRENT, 5000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 5000) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, null) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, null) //
						.output(ElectricityMeter.ChannelId.FREQUENCY, 50000) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 122000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 122000) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, null)) //
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
						.setPhase(L1) //
						.setType(GRID) //
						.build()) //
				.next(new TestCase() //
						.output(MeterChintDdsu666.ChannelId.COMMUNICATION_ADDRESS, 2) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, -1250) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 1000000L)) //
				.deactivate();
	}
}
