package io.openems.edge.controller.pvinverter.reversepowerrelay;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

public class ReversePowerRelayImplTest {

    private static final String CTRL_ID = "ctrl0";
    private static final String PV_INVERTER = "pvInverter0";

    private static final ChannelAddress INPUT_CHANNEL_0_PERCENT = new ChannelAddress("MyRelay", "Input1");
    private static final ChannelAddress INPUT_CHANNEL_30_PERCENT = new ChannelAddress("MyRelay", "Input2");
    private static final ChannelAddress INPUT_CHANNEL_60_PERCENT = new ChannelAddress("MyRelay", "Input3");
    private static final ChannelAddress INPUT_CHANNEL_100_PERCENT = new ChannelAddress("MyRelay", "Input4");

    private static final ChannelAddress PV_INVERTER_ACTIVE_POWER_LIMIT = new ChannelAddress(PV_INVERTER, "ActivePowerLimit");

    @Test
    public void testReversePowerRelay() throws Exception {
        new ControllerTest(new ReversePowerRelayImpl()) //
                .addReference("componentManager", new DummyComponentManager()) //
                .activate(MyConfig.create() //
                        .setId(CTRL_ID) //
                        .setAlias("TestAlias") // Set alias to a non-null value
                        .setEnabled(true) // Ensure enabled is set
                        .setPvInverterId(PV_INVERTER) //
                        .setInputChannelAddress0Percent("MyRelay/Input1") //
                        .setInputChannelAddress30Percent("MyRelay/Input2") //
                        .setInputChannelAddress60Percent("MyRelay/Input3") //
                        .setInputChannelAddress100Percent("MyRelay/Input4") //
                        .setPowerLimit30(300) //
                        .setPowerLimit60(600) //
                        .build())
                .next(new TestCase() //
                        .input(INPUT_CHANNEL_0_PERCENT, true) //
                        .input(INPUT_CHANNEL_30_PERCENT, false) //
                        .input(INPUT_CHANNEL_60_PERCENT, false) //
                        .input(INPUT_CHANNEL_100_PERCENT, false) //
                        .output(PV_INVERTER_ACTIVE_POWER_LIMIT, 0)) //
                .next(new TestCase() //
                        .input(INPUT_CHANNEL_0_PERCENT, false) //
                        .input(INPUT_CHANNEL_30_PERCENT, true) //
                        .input(INPUT_CHANNEL_60_PERCENT, false) //
                        .input(INPUT_CHANNEL_100_PERCENT, false) //
                        .output(PV_INVERTER_ACTIVE_POWER_LIMIT, 300)) //
                .next(new TestCase() //
                        .input(INPUT_CHANNEL_0_PERCENT, false) //
                        .input(INPUT_CHANNEL_30_PERCENT, false) //
                        .input(INPUT_CHANNEL_60_PERCENT, true) //
                        .input(INPUT_CHANNEL_100_PERCENT, false) //
                        .output(PV_INVERTER_ACTIVE_POWER_LIMIT, 600)) //
                .next(new TestCase() //
                        .input(INPUT_CHANNEL_0_PERCENT, false) //
                        .input(INPUT_CHANNEL_30_PERCENT, false) //
                        .input(INPUT_CHANNEL_60_PERCENT, false) //
                        .input(INPUT_CHANNEL_100_PERCENT, true) //
                        .output(PV_INVERTER_ACTIVE_POWER_LIMIT, null));
    }
}
