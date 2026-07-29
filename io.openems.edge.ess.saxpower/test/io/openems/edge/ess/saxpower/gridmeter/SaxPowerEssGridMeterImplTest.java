package io.openems.edge.ess.saxpower.gridmeter;


import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.common.test.ComponentTest;
import org.junit.jupiter.api.Test;

public class SaxPowerEssGridMeterImplTest {

    @Test
    public void test() throws Exception {
        new ComponentTest(new SaxPowerGridMeterImpl())
                .addReference("cm", new DummyConfigurationAdmin())
                .addReference("setModbus", new DummyModbusBridge("modbus0"))
                .activate(MyConfig.create()
                        .setId("ess0")
                        .setModbusId("modbus0")
                        .setType(MeterType.GRID)
                        .setModbusUnitId(64)
                        .build()
                )
                .next(new AbstractComponentTest.TestCase())
                .deactivate();
    }
}
