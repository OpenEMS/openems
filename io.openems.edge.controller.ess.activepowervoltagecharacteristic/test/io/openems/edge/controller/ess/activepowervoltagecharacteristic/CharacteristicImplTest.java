package io.openems.edge.controller.ess.activepowervoltagecharacteristic;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class CharacteristicImplTest {

	private static final String CTRL_ID = "ctrlActivePowerVoltageCharacteristic0";
	private static final String ESS_ID = "ess1";
	private static final String METER_ID = "meter0";
	private static final ChannelAddress ESS_ACTIVE_POWER = new ChannelAddress(ESS_ID, "SetActivePowerEquals");
	private static final ChannelAddress METER_VOLTAGE = new ChannelAddress(METER_ID, "Voltage");

	@Test
	public void test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-10-05T14:00:00.00Z"), ZoneOffset.UTC);
		new ControllerTest(new ActivePowerVoltageCharacteristicImpl())//
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("meter", new DummyElectricityMeter(METER_ID)) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.activate(MyConfig.create()//
						.setId(CTRL_ID)//
						.setEssId(ESS_ID)//
						.setMeterId(METER_ID)//
						.setNominalVoltage(240)//
						.setWaitForHysteresis(5)//
						.setPowerVoltConfig(JsonUtils.buildJsonArray()//
								.add(JsonUtils.buildJsonObject()//
										.addProperty("voltageRatio", 0.95) //
										.addProperty("power", 4000) //
										.build()) //
								.add(JsonUtils.buildJsonObject()//
										.addProperty("voltageRatio", 0.98) //
										.addProperty("power", 1000) //
										.build()) //
								.add(JsonUtils.buildJsonObject()//
										.addProperty("voltageRatio", 0.98001) //
										.addProperty("power", 0) //
										.build()) //
								.add(JsonUtils.buildJsonObject()//
										.addProperty("voltageRatio", 1.02999) //
										.addProperty("power", 0) //
										.build()) //
								.add(JsonUtils.buildJsonObject()//
										.addProperty("voltageRatio", 1.03) //
										.addProperty("power", -1000) //
										.build()) //
								.add(JsonUtils.buildJsonObject()//
										.addProperty("voltageRatio", 1.05) //
										.addProperty("power", -4000) //
										.build() //
								).build().toString() //
						).build()) //
				.next(new TestCase("First Input") //
						.input(METER_VOLTAGE, 250_000) // [mV]
						.output(ESS_ACTIVE_POWER, -2749)) //
				.next(new TestCase("Second Input, \"Power: -1500 \"") //
						.timeleap(clock, 5, ChronoUnit.SECONDS) //
						.input(METER_VOLTAGE, 248_000) // [mV]
						.output(ESS_ACTIVE_POWER, -1499))//
				.next(new TestCase() //
						.input(METER_VOLTAGE, 240_200) // [mV]
						.output(ESS_ACTIVE_POWER, null)) //
				.next(new TestCase("Third Input, \"Power: 0 \"") //
						.timeleap(clock, 5, ChronoUnit.SECONDS) //
						.input(METER_VOLTAGE, 238_100) // [mV]
						.output(ESS_ACTIVE_POWER, 0)) //
				.next(new TestCase() //
						.input(METER_VOLTAGE, 240_000) // [mV]
						.output(ESS_ACTIVE_POWER, null)) //
				.next(new TestCase() //
						.input(METER_VOLTAGE, 238_800)// [mV]
						.output(ESS_ACTIVE_POWER, null)) //
				.next(new TestCase("Fourth Input, \"Power: 0 \"") //
						.timeleap(clock, 5, ChronoUnit.SECONDS) //
						.input(METER_VOLTAGE, 235_200) // [mV]
						.output(ESS_ACTIVE_POWER, 998)) //
				.next(new TestCase() //
						.timeleap(clock, 2, ChronoUnit.SECONDS) //
						.input(METER_VOLTAGE, 235_600) // [mV]
						.output(ESS_ACTIVE_POWER, null)) //
				.next(new TestCase() //
						.timeleap(clock, 2, ChronoUnit.SECONDS) //
						.input(METER_VOLTAGE, 234_000) // [mV]
						.output(ESS_ACTIVE_POWER, null)) //
				.next(new TestCase("Fifth Input, \"Power: 1625 \"") //
						.timeleap(clock, 1, ChronoUnit.SECONDS) //
						.input(METER_VOLTAGE, 233_700) // [mV]
						.output(ESS_ACTIVE_POWER, 1625)) //
				.next(new TestCase("Fourth Input, \"Power: 0 \"") //
						.timeleap(clock, 5, ChronoUnit.SECONDS) //
						.input(METER_VOLTAGE, 225_000) // [mV]
						.output(ESS_ACTIVE_POWER, 4000)) //
				.next(new TestCase("Smaller then Min Key, \"Power: 0 \"") //
						.timeleap(clock, 5, ChronoUnit.SECONDS) //
						.input(METER_VOLTAGE, 255_000) // [mV]
						.output(ESS_ACTIVE_POWER, -4000)) //
				.next(new TestCase("Bigger than Max Key, \"Power: 0 \"") //
						.timeleap(clock, 5, ChronoUnit.SECONDS) //
						.input(METER_VOLTAGE, 270_000) // [mV]
						.output(ESS_ACTIVE_POWER, -4000)) //
		;
	}
}
