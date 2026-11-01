package io.openems.edge.controller.io.heatingelement;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.LEVEL;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.LEGIONELLEN_PROTECTION_FAILED;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.WATER_ACTUAL;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.WATER_THERMOMETER_NOT_AVAILABLE;
import static io.openems.edge.thermometer.api.Thermometer.ChannelId.TEMPERATURE;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;
import io.openems.edge.thermometer.test.DummyThermometer;

public class ControllerHeatingElementImplTest11 {

	private static final String THERMOMETER_ID = "thermometer0";

	private static ControllerTest prepareTest(TimeLeapClock clock, MyConfig.Builder config, DummyThermometer thermometer)
			throws Exception {
		return new ControllerTest(new ControllerIoHeatingElementImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addReference("waterThermometer", thermometer) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(config //
						.setId("ctrl0") //
						.setOutputChannelPhaseL1("io0/InputOutput0") //
						.setOutputChannelPhaseL2("io0/InputOutput1") //
						.setOutputChannelPhaseL3("io0/InputOutput2") //
						.setPowerOfPhase(2000) //
						.setWorkMode(WorkMode.NONE) //
						.setDefaultLevel(Level.LEVEL_1) //
						.setMinTime(1) //
						.setEndTime("00:00") //
						.setEndTimeWithMeter("00:00") //
						.setMinimumSwitchingTime(60) //
						.setMinEnergylimit(0) //
						.setMeterid("") //
						.setScheduler("") //
						.setHasWaterThermometer(true) //
						.setWaterThermometerId(THERMOMETER_ID) //
						.build());
	}

	@Test
	public void testWithoutThermometerBehavesLikeBefore() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerIoHeatingElementImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addReference("waterThermometer", new DummyThermometer(THERMOMETER_ID).withTemperature(900)) //
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
						.setMinTime(1) //
						.setEndTime("00:00") //
						.setEndTimeWithMeter("00:00") //
						.setMinimumSwitchingTime(60) //
						.setMinEnergylimit(0) //
						.setMeterid("") //
						.setScheduler("") //
						.setHasWaterThermometer(false) //
						.build())
				// The 90 °C of the thermometer are ignored
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, -6500) //
						.output(LEVEL, Level.LEVEL_3)) //
				.deactivate();
	}

	@Test
	public void testTemperatureLimits() throws Exception {
		final var clock = createDummyClock();
		prepareTest(clock, MyConfig.create().setMode(Mode.AUTOMATIC).setWaterTemperatures(45, 60, 85),
				new DummyThermometer(THERMOMETER_ID)) //
				// Between low and high: heating with excess power
				.next(new TestCase() //
						.input(THERMOMETER_ID, TEMPERATURE, 500) //
						.input(GRID_ACTIVE_POWER, -2500) //
						.output(LEVEL, Level.LEVEL_1) //
						.output(WATER_ACTUAL, 50) //
						.output(WATER_THERMOMETER_NOT_AVAILABLE, false)) //
				// Below low: heating with all phases without excess power
				.next(new TestCase() //
						.timeleap(clock, 61, SECONDS) //
						.input(THERMOMETER_ID, TEMPERATURE, 400) //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_3)) //
				// Above high: off, even with excess power
				.next(new TestCase() //
						.input(THERMOMETER_ID, TEMPERATURE, 650) //
						.input(GRID_ACTIVE_POWER, -6500) //
						.output(LEVEL, Level.LEVEL_0)) //
				// Above maximum: off, even with excess power
				.next(new TestCase() //
						.input(THERMOMETER_ID, TEMPERATURE, 860) //
						.input(GRID_ACTIVE_POWER, -6500) //
						.output(LEVEL, Level.LEVEL_0)) //
				.deactivate();
	}

	/**
	 * Tests that the configured sensor error level is applied if the thermometer
	 * has no temperature.
	 *
	 * @param sensorErrorLevel the configured sensor error {@link Level}
	 * @throws Exception on error
	 */
	@ParameterizedTest
	@EnumSource(Level.class)
	public void testSensorError(Level sensorErrorLevel) throws Exception {
		final var clock = createDummyClock();
		// The thermometer has no temperature
		prepareTest(clock, MyConfig.create().setMode(Mode.AUTOMATIC).setSensorErrorLevel(sensorErrorLevel),
				new DummyThermometer(THERMOMETER_ID)) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, -6500) //
						.output(LEVEL, sensorErrorLevel) //
						.output(WATER_THERMOMETER_NOT_AVAILABLE, true)) //
				.deactivate();
	}

	@Test
	public void testLegionellenProtection() throws Exception {
		final var clock = createDummyClock();
		prepareTest(clock, MyConfig.create().setMode(Mode.AUTOMATIC) //
				.setWaterTemperatures(45, 60, 85) //
				.setLegionellenProtection(true, 65, 1, 1, 6), //
				new DummyThermometer(THERMOMETER_ID)) //
				// Interval is not over yet
				.next(new TestCase() //
						.input(THERMOMETER_ID, TEMPERATURE, 550) //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_0)) //
				// Interval is over: heating with all phases without excess power
				.next(new TestCase() //
						.timeleap(clock, 25, HOURS) //
						.input(THERMOMETER_ID, TEMPERATURE, 550) //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_3)) //
				// Legionella temperature is reached, but not held for the minimum time yet
				.next(new TestCase() //
						.input(THERMOMETER_ID, TEMPERATURE, 660) //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_3)) //
				// Temperature drops below the Legionella temperature: the time starts again
				.next(new TestCase() //
						.timeleap(clock, 30, MINUTES) //
						.input(THERMOMETER_ID, TEMPERATURE, 550) //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_3)) //
				.next(new TestCase() //
						.input(THERMOMETER_ID, TEMPERATURE, 660) //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_3)) //
				.next(new TestCase() //
						.timeleap(clock, 45, MINUTES) //
						.input(THERMOMETER_ID, TEMPERATURE, 660) //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_3)) //
				// Held for the minimum time: protection is done, but the water is above the
				// high limit
				.next(new TestCase() //
						.timeleap(clock, 20, MINUTES) //
						.input(THERMOMETER_ID, TEMPERATURE, 660) //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_0)) //
				.deactivate();
	}

	@Test
	public void testLegionellenTemperatureBand() throws Exception {
		final var clock = createDummyClock();
		prepareTest(clock, MyConfig.create().setMode(Mode.AUTOMATIC) //
				.setWaterTemperatures(45, 60, 85) //
				.setLegionellenProtection(true, 65, 1, 1, 6), //
				new DummyThermometer(THERMOMETER_ID)) //
				.next(new TestCase() //
						.timeleap(clock, 25, HOURS) //
						.input(THERMOMETER_ID, TEMPERATURE, 550) //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_3)) //
				// More than 2 K above the Legionella temperature: no further heating
				.next(new TestCase() //
						.timeleap(clock, 61, SECONDS) //
						.input(THERMOMETER_ID, TEMPERATURE, 680) //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_0)) //
				.deactivate();
	}

	@Test
	public void testLegionellenHeatingAborted() throws Exception {
		final var clock = createDummyClock();
		prepareTest(clock, MyConfig.create().setMode(Mode.AUTOMATIC) //
				.setWaterTemperatures(45, 60, 85) //
				.setLegionellenProtection(true, 65, 1, 1, 2), //
				new DummyThermometer(THERMOMETER_ID)) //
				.next(new TestCase() //
						.timeleap(clock, 25, HOURS) //
						.input(THERMOMETER_ID, TEMPERATURE, 550) //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_3) //
						.output(LEGIONELLEN_PROTECTION_FAILED, false)) //
				// The temperature was not reached in time: heating is aborted. The 6000 W
				// grid consumption are caused by the heating element itself, i.e. there is no
				// excess power.
				.next(new TestCase() //
						.timeleap(clock, 3, HOURS) //
						.input(THERMOMETER_ID, TEMPERATURE, 550) //
						.input(GRID_ACTIVE_POWER, 6000) //
						.output(LEVEL, Level.LEVEL_0) //
						.output(LEGIONELLEN_PROTECTION_FAILED, true)) //
				// Retry after one day
				.next(new TestCase() //
						.timeleap(clock, 25, HOURS) //
						.input(THERMOMETER_ID, TEMPERATURE, 550) //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_3) //
						.output(LEGIONELLEN_PROTECTION_FAILED, false)) //
				.deactivate();
	}

	@Test
	public void testManualOn() throws Exception {
		final var clock = createDummyClock();
		prepareTest(clock, MyConfig.create().setMode(Mode.MANUAL_ON).setWaterTemperatures(45, 60, 85),
				new DummyThermometer(THERMOMETER_ID)) //
				.next(new TestCase() //
						.input(THERMOMETER_ID, TEMPERATURE, 500) //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_1)) //
				.next(new TestCase() //
						.input(THERMOMETER_ID, TEMPERATURE, 650) //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_0)) //
				.deactivate();
	}
}