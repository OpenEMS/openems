package io.openems.edge.controller.io.heating.room;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.controller.io.heating.room.Mode.AUTOMATIC;
import static io.openems.edge.controller.io.heating.room.Mode.MANUAL_LOW;
import static io.openems.edge.controller.io.heating.room.Mode.OFF;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import io.openems.common.function.ThrowingRunnable;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.test.DummyThermometer;

public class ControllerIoRoomHeatingImplTest {

	@Test
	public void testLow() throws Exception {
		final var clock = createDummyClock();
		final var io = new DummyInputOutput("io0");
		final var sut = new ControllerIoRoomHeatingImpl(clock);
		new ControllerTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("floorThermometer", new DummyThermometer("temp0")) //
				.addReference("ambientThermometer", new DummyThermometer("temp1")) //
				.addReference("floorRelayComponents", List.of(io)) //
				.addReference("infraredRelayComponents", List.of(io)) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMode(MANUAL_LOW) //
						.setSchedule("") //
						.setLowFloorTemperature(150) //
						.setLowAmbientTemperature(160) //
						.setHighFloorTemperature(210) //
						.setHighAmbientTemperature(220) //
						.setFloorThermometerId("temp0") //
						.setAmbientThermometerId("temp1") //
						.setFloorRelays("io0/InputOutput0", "io0/InputOutput1") //
						.setInfraredRelays("io0/InputOutput2", "io0/InputOutput3") //
						.setFloorPower(2600) //
						.setInfraredPower(2000) //
						.setHasExternalAmbientHeating(true) //
						.build())
				.next(new TestCase() //
						.onAfterProcessImage(assertLog(sut,
								"Manual LOW|Floor:UNDEFINED->UNDEFINED|Ambient:UNDEFINED->UNDEFINED|UNDEFINED")) //
						.input("temp0", Thermometer.ChannelId.TEMPERATURE, 160) //
						.input("temp1", Thermometer.ChannelId.TEMPERATURE, 160) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT0, false) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT1, false) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, false) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, false) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 0)) //

				.next(new TestCase() //
						.timeleap(clock, 3, MINUTES) //
						.onAfterProcessImage(assertLog(sut, "Manual LOW|Floor:160->150|Ambient:160->160|0 W")) //
						.input("temp0", Thermometer.ChannelId.TEMPERATURE, 140) //
						.input("temp1", Thermometer.ChannelId.TEMPERATURE, 140) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT0, true) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT1, true) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, true) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, true)) //

				.next(new TestCase() //
						.timeleap(clock, 3, MINUTES) //
						.onAfterProcessImage(assertLog(sut, "Manual LOW|Floor:140->150|Ambient:140->160|0 W")) //
						.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT0, true) //
						.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT1, true) //
						.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, true) //
						.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, true) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 4600)) //

				.next(new TestCase() //
						.timeleap(clock, 3, MINUTES) //
						.onAfterProcessImage(assertLog(sut, "Manual LOW|Floor:140->150|Ambient:140->160|4600 W")) //
						.input("temp0", Thermometer.ChannelId.TEMPERATURE, 150) //
						.input("temp1", Thermometer.ChannelId.TEMPERATURE, 150) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT0, false) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT1, false) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, true) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, true)) //

				.next(new TestCase() //
						.timeleap(clock, 3, MINUTES) //
						.onAfterProcessImage(assertLog(sut, "Manual LOW|Floor:150->150|Ambient:150->160|4600 W")) //
						.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT0, false) //
						.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT1, false) //
						.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, true) //
						.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, true) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 2000)) //

				.next(new TestCase() //
						.timeleap(clock, 3, MINUTES) //
						.onAfterProcessImage(assertLog(sut, "Manual LOW|Floor:150->150|Ambient:150->160|2000 W")) //
						.input("temp0", Thermometer.ChannelId.TEMPERATURE, 160) //
						.input("temp1", Thermometer.ChannelId.TEMPERATURE, 170) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT0, false) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT1, false) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, false) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, false)) //

				.next(new TestCase() //
						.timeleap(clock, 3, MINUTES) //
						.onAfterProcessImage(assertLog(sut, "Manual LOW|Floor:160->150|Ambient:170->160|2000 W")) //
						.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT0, false) //
						.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT1, false) //
						.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, false) //
						.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, false) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 0)) //

				.next(new TestCase() //
						.timeleap(clock, 3, MINUTES) //
						.onAfterProcessImage(assertLog(sut, "Manual LOW|Floor:160->150|Ambient:170->160|0 W"))) //

				.deactivate();
	}

	@Test
	public void testAuto() throws Exception {
		final var clock = createDummyClock();
		final var io = new DummyInputOutput("io0");
		final var sut = new ControllerIoRoomHeatingImpl(clock);
		new ControllerTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("floorThermometer", new DummyThermometer("temp0")) //
				.addReference("ambientThermometer", new DummyThermometer("temp1")) //
				.addReference("floorRelayComponents", List.of(io)) //
				.addReference("infraredRelayComponents", List.of(io)) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMode(AUTOMATIC) //
						.setSchedule("""
								[
								   {
								      "@type":"Task",
								      "start":"01:00:00",
								      "duration":"PT1H",
								      "recurrenceRules":[
								         {
								            "frequency":"daily"
								         }
								      ]
								   }
								]""") //
						.setLowFloorTemperature(150) //
						.setLowAmbientTemperature(160) //
						.setHighFloorTemperature(210) //
						.setHighAmbientTemperature(220) //
						.setFloorThermometerId("temp0") //
						.setAmbientThermometerId("temp1") //
						.setFloorRelays("io0/InputOutput0") //
						.setInfraredRelays("io0/InputOutput2") //
						.setFloorPower(2600) //
						.setInfraredPower(2000) //
						.setHasExternalAmbientHeating(true) //
						.build())
				.next(new TestCase() //
						.onAfterProcessImage(assertLog(sut, //
								"Auto|LOW|NextHigh:2020-01-01T01:00:00|Floor:UNDEFINED->UNDEFINED|Ambient:UNDEFINED->UNDEFINED|UNDEFINED"))) //

				.next(new TestCase() //
						.timeleap(clock, 1, HOURS) //
						.onAfterProcessImage(assertLog(sut, //
								"Auto|HIGH|Till:2020-01-01T02:00:00|Floor:UNDEFINED->150|Ambient:UNDEFINED->160|0 W"))) //

				.next(new TestCase() //
						.timeleap(clock, 61, MINUTES) //
						.onAfterProcessImage(assertLog(sut, //
								"Auto|LOW|NoSchedule|Floor:UNDEFINED->210|Ambient:UNDEFINED->220|0 W")));
	}

	@Test
	public void testAutoWrong() throws Exception {
		final var clock = createDummyClock();
		final var io = new DummyInputOutput("io0");
		final var sut = new ControllerIoRoomHeatingImpl(clock);
		new ControllerTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("floorThermometer", new DummyThermometer("temp0")) //
				.addReference("ambientThermometer", new DummyThermometer("temp1")) //
				.addReference("floorRelayComponents", List.of(io)) //
				.addReference("infraredRelayComponents", List.of(io)) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMode(AUTOMATIC) //
						.setSchedule("") //
						.setFloorThermometerId("temp0") //
						.setAmbientThermometerId("temp1") //
						.setFloorRelays("io0/InputOutput0") //
						.setInfraredRelays("io0/InputOutput2") //
						.build())
				.next(new TestCase() //
						.onAfterProcessImage(assertLog(sut, //
								"Auto|LOW|NoSchedule|Floor:UNDEFINED->UNDEFINED|Ambient:UNDEFINED->UNDEFINED|UNDEFINED")));
	}

	@Test
	public void testOff() throws Exception {
		final var clock = createDummyClock();
		final var io = new DummyInputOutput("io0");
		final var sut = new ControllerIoRoomHeatingImpl(clock);
		new ControllerTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("floorThermometer", new DummyThermometer("temp0")) //
				.addReference("ambientThermometer", new DummyThermometer("temp1")) //
				.addReference("floorRelayComponents", List.of(io)) //
				.addReference("infraredRelayComponents", List.of(io)) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMode(OFF) //
						.setSchedule("") //
						.setFloorThermometerId("temp0") //
						.setAmbientThermometerId("temp1") //
						.setFloorRelays("io0/InputOutput0") //
						.setInfraredRelays("io0/InputOutput2") //
						.build())
				.next(new TestCase() //
						.onAfterProcessImage(assertLog(sut, "Off")));
	}

	private static ThrowingRunnable<Exception> assertLog(ControllerIoRoomHeatingImpl sut, String message) {
		return () -> assertEquals(message, sut.debugLog());
	}
}
