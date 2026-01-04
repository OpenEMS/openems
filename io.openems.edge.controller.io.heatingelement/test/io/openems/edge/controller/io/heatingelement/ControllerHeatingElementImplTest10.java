package io.openems.edge.controller.io.heatingelement;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.LEVEL;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.STATUS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.Status;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

/**
 * A test to simulate three days with the configuration of a task scheduler in
 * the workmode Time.
 */
public class ControllerHeatingElementImplTest10 {

	private static final TimeLeapClock CLOCK = createDummyClock();

    private static ControllerTest prepareTest() throws OpenemsError.OpenemsNamedException, Exception {
        return new ControllerTest(new ControllerIoHeatingElementImpl()) //
                .addReference("componentManager", new DummyComponentManager(CLOCK)) //
                .addReference("sum", new DummySum()) //
                .addReference("cm", new DummyConfigurationAdmin()) //
                .addComponent(new DummyInputOutput("io0")) //
                .activate(MyConfig.create() //
                        .setId("ctrl0") //
                        .setOutputChannelPhaseL1("io0/InputOutput0") //
                        .setOutputChannelPhaseL2("io0/InputOutput1") //
                        .setOutputChannelPhaseL3("io0/InputOutput2") //
                        .setPowerOfPhase(2000) //
                        .setMode(Mode.AUTOMATIC) //
                        .setWorkMode(WorkMode.TIME) //
                        .setDefaultLevel(Level.LEVEL_1) //
                        .setMeterid("") //
                        .setEndTime("00:00") //
                        .setMinTime(2) //
                        .setMinimumSwitchingTime(60) //
                        .setMinEnergylimit(0) //
                        .setEndTimeWithMeter("00:00") //
                        .setScheduler("""
								[
									{
										"@type":"Task",
										"start":"08:00:00",
										"duration": "PT6H",
										"recurrenceRules":[
											{
												"frequency":"daily"
											}
										]
									}
								]
								""") //
						.build()); //
	}

	@Test
	public void testWithSchedule() throws Exception {
		var test = prepareTest();
		for (int i = 0; i < 3; i++) {
			this.daySimulation(test);
		}
		test.deactivate();
	}

	private void daySimulation(ControllerTest test) throws Exception {
		test //
				.next(new AbstractComponentTest.TestCase() //
						.timeleap(CLOCK, 8, HOURS) //
						.input(GRID_ACTIVE_POWER, -3000) //
						.output(LEVEL, Level.LEVEL_1) //
						.output(STATUS, Status.ACTIVE)) //

				.next(new AbstractComponentTest.TestCase() //
						.timeleap(CLOCK, 1, SECONDS) //
						.input(GRID_ACTIVE_POWER, -3000) //
						.output(LEVEL, Level.LEVEL_1) //
						.output(STATUS, Status.ACTIVE)) //

				.next(new AbstractComponentTest.TestCase() //
						.timeleap(CLOCK, 3600 - 1, SECONDS) //
						.input(GRID_ACTIVE_POWER, 1000) //
						.output(LEVEL, Level.LEVEL_0) //
						.output(STATUS, Status.INACTIVE)) //

				.next(new AbstractComponentTest.TestCase() //
						.timeleap(CLOCK, 3600 * 4 + 1, SECONDS) //
						.input(GRID_ACTIVE_POWER, 1000) //
						.output(LEVEL, Level.LEVEL_1) //
						.output(STATUS, Status.ACTIVE_FORCED)) //

				.next(new AbstractComponentTest.TestCase() //
						.timeleap(CLOCK, 1, HOURS) //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_0) //
						.output(STATUS, Status.INACTIVE)) //

				.next(new AbstractComponentTest.TestCase() //
						.timeleap(CLOCK, 3600 * 10, SECONDS) //
						.input(GRID_ACTIVE_POWER, -2000) //
						.output(LEVEL, Level.LEVEL_0) //
						.output(STATUS, Status.INACTIVE)) //
		;
	}
}
