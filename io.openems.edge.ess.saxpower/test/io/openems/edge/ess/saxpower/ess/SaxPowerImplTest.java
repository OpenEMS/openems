package io.openems.edge.ess.saxpower.ess;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import org.junit.Test;

public class SaxPowerImplTest {

    @Test
    public void test() throws Exception {
        new ComponentTest(new SaxPowerImpl())
                .addReference("cm", new DummyConfigurationAdmin())
                .addReference("setModbus", new DummyModbusBridge("modbus0"))
                .activate(MyConfig.create()
                        .setId("ess0")
                        .setModbusId("modbus0")
                        .setModbusUnitId(64)
                        .setCapacity(7700)
                        .build()
                )
                .next(new TestCase())
                .deactivate();
    }
}
