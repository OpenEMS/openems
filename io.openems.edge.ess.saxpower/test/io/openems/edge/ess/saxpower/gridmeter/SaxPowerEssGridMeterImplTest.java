package io.openems.edge.ess.saxpower.gridmeter;

import static io.openems.common.types.MeterType.GRID;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.openems.common.channel.Level;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.ElectricityMeter;

public class SaxPowerEssGridMeterImplTest {

    @Test
    public void testFeedToGrid() throws Exception {
        new ComponentTest(new SaxPowerEssGridMeterImpl()) //
                .addReference("setModbus", new DummyModbusBridge("modbus0") //
                        .withRegisters(40072, //
                                150, 150, 0, 0)) //
                .activate(MyConfig.create() //
                        .setId("meter0") //
                        .setModbusId("modbus0") //
                        .setModbusUnitId(100) //
                        .setType(GRID) //
                        .build()) //
                .next(new TestCase() //
                        .activateStrictMode() //
                        .output(OpenemsComponent.ChannelId.STATE, Level.OK) //
                        .output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //
                        .output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, null) //
                        .output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, null) //
                        .output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, null) //
                        .output(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, null) //
                        .output(ElectricityMeter.ChannelId.ACTIVE_POWER, -1500) //
                        .output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, -1500) //
                        .output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
                        .output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
                        .output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, null) //
                        .output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, null) //
                        .output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, null) //
                        .output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, null) //
                        .output(ElectricityMeter.ChannelId.CURRENT, null) //
                        .output(ElectricityMeter.ChannelId.CURRENT_L1, null) //
                        .output(ElectricityMeter.ChannelId.CURRENT_L2, null) //
                        .output(ElectricityMeter.ChannelId.CURRENT_L3, null) //
                        .output(ElectricityMeter.ChannelId.FREQUENCY, null) //
                        .output(ElectricityMeter.ChannelId.REACTIVE_POWER, null) //
                        .output(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, null) //
                        .output(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, null) //
                        .output(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, null) //
                        .output(ElectricityMeter.ChannelId.VOLTAGE, null) //
                        .output(ElectricityMeter.ChannelId.VOLTAGE_L1, null) //
                        .output(ElectricityMeter.ChannelId.VOLTAGE_L2, null) //
                        .output(ElectricityMeter.ChannelId.VOLTAGE_L3, null)) //
                .deactivate();
    }

    @Test
    public void testBuyFromGrid() throws Exception {
        new ComponentTest(new SaxPowerEssGridMeterImpl()) //
                .addReference("setModbus", new DummyModbusBridge("modbus0") //
                        .withRegisters(40072, //
                                -80, -80, 0, 0)) //
                .activate(MyConfig.create() //
                        .setId("meter0") //
                        .setModbusId("modbus0") //
                        .setModbusUnitId(100) //
                        .setType(GRID) //
                        .build()) //
                .next(new TestCase() //
                        .output(ElectricityMeter.ChannelId.ACTIVE_POWER, 800) //
                        .output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 800) //
                        .output(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
                        .output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0)) //
                .deactivate();
    }

    @Test
    public void testDebugLog() throws Exception {
        var sut = new SaxPowerEssGridMeterImpl();
        new ComponentTest(sut) //
                .addReference("setModbus", new DummyModbusBridge("modbus0")) //
                .activate(MyConfig.create() //
                        .setId("meter0") //
                        .setModbusId("modbus0") //
                        .setModbusUnitId(100) //
                        .setType(GRID) //
                        .build()) //
                .next(new TestCase() //
                        .input(ElectricityMeter.ChannelId.ACTIVE_POWER, 1500) //
                        .input(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 500) //
                        .input(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 500) //
                        .input(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 500)) //
                .deactivate();

        assertEquals("L:1500 W|L1:500 W|L2:500 W|L3:500 W", sut.debugLog());
    }
}
