package io.openems.edge.controller.ess.activepowervoltagecharacteristic;

import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class CharacteristicImplTest {

	private static final ChannelAddress METER_VOLTAGE = new ChannelAddress("meter0", "Voltage");

	@Test
	public void test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-10-05T14:00:00.00Z"), ZoneOffset.UTC);
		new ControllerTest(new ControllerEssActivePowerVoltageCharacteristicImpl())//
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.addReference("ess", new DummyManagedSymmetricEss("ess1")) //
				.activate(MyConfig.create()//
						.setId("ctrlActivePowerVoltageCharacteristic0")//
						.setEssId("ess1")//
						.setMeterId("meter0")//
						.setNominalVoltage(240)//
						.setWaitForHysteresis(5)//
						.setPowerVoltConfig(buildJsonArray()//
								.add(buildJsonObject()//
										.addProperty("voltageRatio", 0.95) //
										.addProperty("power", 4000) //
										.build()) //
								.add(buildJsonObject()//
										.addProperty("voltageRatio", 0.98) //
										.addProperty("power", 1000) //
										.build()) //
								.add(buildJsonObject()//
										.addProperty("voltageRatio", 0.98001) //
										.addProperty("power", 0) //
										.build()) //
								.add(buildJsonObject()//
										.addProperty("voltageRatio", 1.02999) //
										.addProperty("power", 0) //
										.build()) //
								.add(buildJsonObject()//
										.addProperty("voltageRatio", 1.03) //
										.addProperty("power", -1000) //
										.build()) //
								.add(buildJsonObject()//
										.addProperty("voltageRatio", 1.05) //
										.addProperty("power", -4000) //
										.build() //
								).build().toString() //
						).build()) //
				.next(new TestCase("First Input") //
						.input(METER_VOLTAGE, 250_000) // [mV]
						.output("ess1", SET_ACTIVE_POWER_EQUALS, -2749)) //
				.next(new TestCase("Second Input, \"Power: -1500 \"") //
						.timeleap(clock, 5, ChronoUnit.SECONDS) //
						.input(METER_VOLTAGE, 248_000) // [mV]
						.output("ess1", SET_ACTIVE_POWER_EQUALS, -1499))//
				.next(new TestCase() //
						.input(METER_VOLTAGE, 240_200) // [mV]
						.output("ess1", SET_ACTIVE_POWER_EQUALS, null)) //
				.next(new TestCase("Third Input, \"Power: 0 \"") //
						.timeleap(clock, 5, ChronoUnit.SECONDS) //
						.input(METER_VOLTAGE, 238_100) // [mV]
						.output("ess1", SET_ACTIVE_POWER_EQUALS, 0)) //
				.next(new TestCase() //
						.input(METER_VOLTAGE, 240_000) // [mV]
						.output("ess1", SET_ACTIVE_POWER_EQUALS, null)) //
				.next(new TestCase() //
						.input(METER_VOLTAGE, 238_800)// [mV]
						.output("ess1", SET_ACTIVE_POWER_EQUALS, null)) //
				.next(new TestCase("Fourth Input, \"Power: 0 \"") //
						.timeleap(clock, 5, ChronoUnit.SECONDS) //
						.input(METER_VOLTAGE, 235_200) // [mV]
						.output("ess1", SET_ACTIVE_POWER_EQUALS, 998)) //
				.next(new TestCase() //
						.timeleap(clock, 2, ChronoUnit.SECONDS) //
						.input(METER_VOLTAGE, 235_600) // [mV]
						.output("ess1", SET_ACTIVE_POWER_EQUALS, null)) //
				.next(new TestCase() //
						.timeleap(clock, 2, ChronoUnit.SECONDS) //
						.input(METER_VOLTAGE, 234_000) // [mV]
						.output("ess1", SET_ACTIVE_POWER_EQUALS, null)) //
				.next(new TestCase("Fifth Input, \"Power: 1625 \"") //
						.timeleap(clock, 1, ChronoUnit.SECONDS) //
						.input(METER_VOLTAGE, 233_700) // [mV]
						.output("ess1", SET_ACTIVE_POWER_EQUALS, 1625)) //
				.next(new TestCase("Fourth Input, \"Power: 0 \"") //
						.timeleap(clock, 5, ChronoUnit.SECONDS) //
						.input(METER_VOLTAGE, 225_000) // [mV]
						.output("ess1", SET_ACTIVE_POWER_EQUALS, 4000)) //
				.next(new TestCase("Smaller then Min Key, \"Power: 0 \"") //
						.timeleap(clock, 5, ChronoUnit.SECONDS) //
						.input(METER_VOLTAGE, 255_000) // [mV]
						.output("ess1", SET_ACTIVE_POWER_EQUALS, -4000)) //
				.next(new TestCase("Bigger than Max Key, \"Power: 0 \"") //
						.timeleap(clock, 5, ChronoUnit.SECONDS) //
						.input(METER_VOLTAGE, 270_000) // [mV]
						.output("ess1", SET_ACTIVE_POWER_EQUALS, -4000)) //
				.deactivate();
	}
}
