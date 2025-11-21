package io.openems.edge.kostal.plenticore.gridmeter;

import static io.openems.edge.kostal.plenticore.gridmeter.KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_POWER;
import static io.openems.edge.kostal.plenticore.gridmeter.KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_POWER_L1;
import static io.openems.edge.kostal.plenticore.gridmeter.KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_POWER_L2;
import static io.openems.edge.kostal.plenticore.gridmeter.KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_POWER_L3;
import static io.openems.edge.kostal.plenticore.gridmeter.KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_REACTIVE_POWER_L1;
import static io.openems.edge.kostal.plenticore.gridmeter.KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_REACTIVE_POWER_L2;
import static io.openems.edge.kostal.plenticore.gridmeter.KostalGridMeter.ChannelId.ACTIVE_CONSUMPTION_REACTIVE_POWER_L3;
import static io.openems.edge.kostal.plenticore.gridmeter.KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_POWER;
import static io.openems.edge.kostal.plenticore.gridmeter.KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_POWER_L1;
import static io.openems.edge.kostal.plenticore.gridmeter.KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_POWER_L2;
import static io.openems.edge.kostal.plenticore.gridmeter.KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_POWER_L3;
import static io.openems.edge.kostal.plenticore.gridmeter.KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_REACTIVE_POWER_L1;
import static io.openems.edge.kostal.plenticore.gridmeter.KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_REACTIVE_POWER_L2;
import static io.openems.edge.kostal.plenticore.gridmeter.KostalGridMeter.ChannelId.ACTIVE_PRODUCTION_REACTIVE_POWER_L3;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L1;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L2;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L3;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.CURRENT_L1;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.CURRENT_L2;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.CURRENT_L3;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.FREQUENCY;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.REACTIVE_POWER_L1;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.REACTIVE_POWER_L2;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.REACTIVE_POWER_L3;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.VOLTAGE_L1;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.VOLTAGE_L2;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.VOLTAGE_L3;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class KostalGridMeterImplTest {

	private static final String METER_ID = "meter0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void testActivateDeactivate() throws Exception {
		new ComponentTest(new KostalGridMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(1) //
						.setType(MeterType.GRID) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

	@Test
	public void testDirectReadModePowerCalculation() throws Exception {
		new ComponentTest(new KostalGridMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(1) //
						.setType(MeterType.GRID) //
						.setViaInverter(false) //
						.setWordwrap(false) //
						.build()) //
				.next(new TestCase("Consumption exceeds production") //
						.input(ACTIVE_CONSUMPTION_POWER, 5000) //
						.input(ACTIVE_PRODUCTION_POWER, 2000) //
						.output(ACTIVE_POWER, 3000)) //
				.next(new TestCase("Production exceeds consumption") //
						.input(ACTIVE_CONSUMPTION_POWER, 1000) //
						.input(ACTIVE_PRODUCTION_POWER, 4000) //
						.output(ACTIVE_POWER, -3000)) //
				.next(new TestCase("Balanced consumption and production") //
						.input(ACTIVE_CONSUMPTION_POWER, 3000) //
						.input(ACTIVE_PRODUCTION_POWER, 3000) //
						.output(ACTIVE_POWER, 0)) //
				.deactivate();
	}

	@Test
	public void testThreePhasePowerCalculation() throws Exception {
		new ComponentTest(new KostalGridMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(1) //
						.setType(MeterType.GRID) //
						.setViaInverter(false) //
						.setWordwrap(false) //
						.build()) //
				.next(new TestCase("Three-phase active power") //
						.input(ACTIVE_CONSUMPTION_POWER_L1, 1000) //
						.input(ACTIVE_PRODUCTION_POWER_L1, 500) //
						.input(ACTIVE_CONSUMPTION_POWER_L2, 2000) //
						.input(ACTIVE_PRODUCTION_POWER_L2, 0) //
						.input(ACTIVE_CONSUMPTION_POWER_L3, 1500) //
						.input(ACTIVE_PRODUCTION_POWER_L3, 1000) //
						.output(ACTIVE_POWER_L1, 500) //
						.output(ACTIVE_POWER_L2, 2000) //
						.output(ACTIVE_POWER_L3, 500)) //
				.next(new TestCase("Three-phase reactive power") //
						.input(ACTIVE_CONSUMPTION_REACTIVE_POWER_L1, 300) //
						.input(ACTIVE_PRODUCTION_REACTIVE_POWER_L1, 100) //
						.input(ACTIVE_CONSUMPTION_REACTIVE_POWER_L2, 200) //
						.input(ACTIVE_PRODUCTION_REACTIVE_POWER_L2, 50) //
						.input(ACTIVE_CONSUMPTION_REACTIVE_POWER_L3, 400) //
						.input(ACTIVE_PRODUCTION_REACTIVE_POWER_L3, 200) //
						.output(REACTIVE_POWER_L1, 200) //
						.output(REACTIVE_POWER_L2, 150) //
						.output(REACTIVE_POWER_L3, 200)) //
				.deactivate();
	}

	@Test
	public void testViaInverterMode() throws Exception {
		new ComponentTest(new KostalGridMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(71) //
						.setType(MeterType.GRID) //
						.setViaInverter(true) //
						.setWordwrap(false) //
						.build()) //
				.next(new TestCase("Read via inverter - all phases") //
						.input(FREQUENCY, 50000) //
						.input(VOLTAGE_L1, 230000) //
						.input(VOLTAGE_L2, 230000) //
						.input(VOLTAGE_L3, 230000) //
						.input(CURRENT_L1, 10000) //
						.input(CURRENT_L2, 12000) //
						.input(CURRENT_L3, 11000) //
						.input(ACTIVE_POWER, 7500) //
						.input(ACTIVE_POWER_L1, 2500) //
						.input(ACTIVE_POWER_L2, 2800) //
						.input(ACTIVE_POWER_L3, 2200)) //
				.deactivate();
	}

	@Test
	public void testWordwrapEncoding() throws Exception {
		new ComponentTest(new KostalGridMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(1) //
						.setType(MeterType.GRID) //
						.setViaInverter(false) //
						.setWordwrap(true) //
						.build()) //
				.next(new TestCase("LSWMSW word order") //
						.input(ACTIVE_CONSUMPTION_POWER, 3000) //
						.input(ACTIVE_PRODUCTION_POWER, 1000) //
						.output(ACTIVE_POWER, 2000)) //
				.deactivate();
	}

	@Test
	public void testMeterType() throws Exception {
		var sut = new KostalGridMeterImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(1) //
						.setType(MeterType.PRODUCTION) //
						.build());
		assertEquals(MeterType.GRID, sut.getMeterType());
	}
}
