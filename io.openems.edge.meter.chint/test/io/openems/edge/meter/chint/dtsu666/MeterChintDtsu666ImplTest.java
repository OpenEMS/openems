package io.openems.edge.meter.chint.dtsu666;

import static io.openems.common.types.MeterType.GRID;

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
import io.openems.edge.meter.api.PhaseRotation;

public class MeterChintDtsu666ImplTest {

	private ComponentTest testBasis;

	@BeforeEach
	public void setup() throws OpenemsException, Exception {
		this.testBasis = new ComponentTest(new MeterChintDtsu666Impl()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withInputRegisters(0x2006, //
								0x4510, 0x5000, //
								0x4510, 0x7000, //
								0x4510, 0x3000, //
								0x45A4, 0x1000, //
								0x4592, 0xE000, //
								0x4580, 0x2000, //
								0x46DE, 0xA800, //
								0x4623, 0xC000, //
								0x462A, 0x2800, //
								0x45DE, 0xD000) //
						.withInputRegisters(0x202A, //
								0x4466, 0x0000) //
						.withInputRegisters(0x2044, //
								0x459C, 0x4C00) //
						.withInputRegisters(0x4000, //
								0x45B1, 0x71DF));
	}

	@Test
	public void testNonInvert() throws Exception {
		this.testBasis //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setInvert(false) //
						.setPhaseRotation(PhaseRotation.L1_L2_L3) //
						.setType(GRID) //
						.build()) //
				.next(new TestCase() //
						.activateStrictMode() //
						.output(OpenemsComponent.ChannelId.STATE, Level.OK) //
						.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, 5678234L) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 2850) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 1048) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 1089) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 713) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, null) //
						.output(ElectricityMeter.ChannelId.CURRENT, 14050) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 5250) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 4700) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 4100) //
						.output(ElectricityMeter.ChannelId.FREQUENCY, 50015) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, null) //
						.output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, null) //
						.output(ElectricityMeter.ChannelId.VOLTAGE, 230900) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230900) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 231100) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 230700) //
						.output(MeterChintDtsu666.ChannelId.TOTAL_POWER_FACTOR, 920)) //
				.deactivate();
	}

	@Test
	public void testInvertAndPhaseRotation() throws Exception {
		this.testBasis //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setInvert(true) //
						.setPhaseRotation(PhaseRotation.L2_L3_L1) //
						.setType(GRID) //
						.build()) //
				.next(new TestCase() //
						.output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, -2850) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, -713) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, -1048) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, -1089) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 5678234L) //
						.output(ElectricityMeter.ChannelId.CURRENT_L1, 4100) //
						.output(ElectricityMeter.ChannelId.CURRENT_L2, 5250) //
						.output(ElectricityMeter.ChannelId.CURRENT_L3, 4700) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L1, 230700) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L2, 230900) //
						.output(ElectricityMeter.ChannelId.VOLTAGE_L3, 231100) //
						.output(MeterChintDtsu666.ChannelId.TOTAL_POWER_FACTOR, 920)) //
				.deactivate();
	}
}
