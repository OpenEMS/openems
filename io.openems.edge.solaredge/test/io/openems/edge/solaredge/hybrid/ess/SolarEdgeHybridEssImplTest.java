package io.openems.edge.solaredge.hybrid.ess;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;


public class SolarEdgeHybridEssImplTest {

    private static final String ESS_ID = "solarEdgeHybridEss0";
    private static final String MODBUS_ID = "modbus0";
    private static final String METER_ID = "meter0";
    private static final ChannelAddress PV_POWER_SETPOINT = new ChannelAddress(ESS_ID, "PVPowerSetpoint");
    private static final ChannelAddress GRID_POWER = new ChannelAddress(ESS_ID, "GridPower");

    private static class MyComponentTest extends ComponentTest {

        public MyComponentTest(OpenemsComponent sut) throws OpenemsException {
            super(sut);
        }

        @Override
        protected void handleEvent(String topic) throws Exception {
            if (topic.equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS)) {
                ((SolarEdgeHybridEssImpl) this.getSut()).limitPvPower();
            }
            super.handleEvent(topic);
        }
    }

    @Test
    public void testLimitPvPower() throws Exception {
        final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800L), ZoneOffset.UTC);
        var sut = new SolarEdgeHybridEssImpl();

        // Set up and run the component test
        new MyComponentTest(sut) //
                .addReference("cm", new DummyConfigurationAdmin()) //
                .addReference("componentManager", new DummyComponentManager(clock)) //
                .addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
                .activate(MyConfig.create() //
                        .setId(ESS_ID) //
                        .setMeterId(METER_ID)
                        .setModbusId(MODBUS_ID) //
                        .setCoreTarget("Arsch") //
                        .build()) //
                .next(new TestCase("Initial Test") //
                        .input(GRID_POWER, -5000) // Example of grid power input, indicating 5000W is being fed to grid
                        .output(PV_POWER_SETPOINT, 0)) // Expected to cut down PV power output to 0W
                .next(new TestCase("Increase Grid Limit") //
                        .timeleap(clock, 5, ChronoUnit.SECONDS) //
                        .input(GRID_POWER, -4000) // Example grid power input under the feed limit
                        .output(PV_POWER_SETPOINT, 4000)); // Adjust PV power output appropriately

        // Add further test cases here as necessary
    }
}
