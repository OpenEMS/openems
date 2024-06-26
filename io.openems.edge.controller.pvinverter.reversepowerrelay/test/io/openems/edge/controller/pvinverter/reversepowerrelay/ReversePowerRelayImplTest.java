package io.openems.edge.controller.pvinverter.reversepowerrelay;

import org.junit.Test;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;

public class ReversePowerRelayImplTest {

    private static final String CTRL_ID = "ctrl0";
    private static final String PV_INVERTER = "pvInverter0";

    @Test
    public void test() throws Exception {
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
                        .build()); //
    }
}
