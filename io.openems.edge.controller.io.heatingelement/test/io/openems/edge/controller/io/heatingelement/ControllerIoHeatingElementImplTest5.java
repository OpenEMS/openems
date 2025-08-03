package io.openems.edge.controller.io.heatingelement;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.LEVEL;

import org.junit.Test;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.test.DummyElectricityMeter;

/**
 * A test to check if the meter is correct referred to the heating element.
 */
public class ControllerIoHeatingElementImplTest5 {
    
    @Test
    public void test() throws Exception {
        final var clock = createDummyClock();
        new ControllerTest(new ControllerIoHeatingElementImpl()) //
                .addReference("componentManager", new DummyComponentManager(clock)) //
                .addReference("sum", new DummySum()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
                .addReference("meter", new DummyElectricityMeter("meter3")) //
                .addComponent(new DummyInputOutput("io0")) //
                .activate(MyConfig.create() //
                        .setId("ctrl0") //
                        .setOutputChannelPhaseL1("io0/InputOutput0") //
                        .setOutputChannelPhaseL2("io0/InputOutput1") //
                        .setOutputChannelPhaseL3("io0/InputOutput2") //
                        .setPowerOfPhase(2000) //
                        .setMode(Mode.AUTOMATIC) //
                        .setWorkMode(WorkMode.NONE) //
                        .setDefaultLevel(Level.LEVEL_1) //
                        .setMeterid("meter3") //
                        .setEndTime("00:00") //
                        .setMinTime(1) //
                        .setMinimumSwitchingTime(60) //
                        .setMinEnergylimit(5000) //
                        .setEndTimeWithMeter("00:00") //
						.setScheduler("") // 
                        .build()) //
                .next(new TestCase() //
                        .input(GRID_ACTIVE_POWER, -6400) //
                        .input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
                        .input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L1, 0) //
                        .input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L2, 0) //
                        .input("meter3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 0) //
                        .output(LEVEL, Level.LEVEL_3)) //
                .deactivate();
        
    }
}