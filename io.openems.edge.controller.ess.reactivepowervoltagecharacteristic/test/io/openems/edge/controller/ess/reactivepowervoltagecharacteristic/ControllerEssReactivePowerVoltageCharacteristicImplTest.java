package io.openems.edge.controller.ess.reactivepowervoltagecharacteristic;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class ControllerEssReactivePowerVoltageCharacteristicImplTest {

	private static final String CTRL_ID = "ctrlReactivePowerVoltageCharacteristic0";
	private static final String ESS_ID = "ess0";
	private static final String METER_ID = "meter0";
	private static final ChannelAddress ESS_REACTIVE_POWER = new ChannelAddress(ESS_ID, "SetReactivePowerEquals");
	private static final ChannelAddress METER_VOLTAGE = new ChannelAddress(METER_ID, "Voltage");
	private static final ChannelAddress MAX_APPARENT_POWER = new ChannelAddress(ESS_ID, "MaxApparentPower");

	@Test
	public void test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-10-05T14:00:00.00Z"), ZoneOffset.UTC);
		new ControllerTest(new ControllerEssReactivePowerVoltageCharacteristicImpl())//
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
										.addProperty("voltageRatio", 0.9) //
										.addProperty("percent", 60) //
										.build()) //
								.add(JsonUtils.buildJsonObject()//
										.addProperty("voltageRatio", 0.93) //
										.addProperty("percent", 0) //
										.build()) //
								.add(JsonUtils.buildJsonObject()//
										.addProperty("voltageRatio", 1.07) //
										.addProperty("percent", 0) //
										.build()) //
								.add(JsonUtils.buildJsonObject()//
										.addProperty("voltageRatio", 1.1) //
										.addProperty("percent", -60) //
										.build() //
								).build().toString() //
						).build()) //
				.next(new TestCase("First Input") //
						.input(METER_VOLTAGE, 240_000) // [mV]
						.input(MAX_APPARENT_POWER, 10_000)) // [VA]
				.next(new TestCase() //
						.output(ESS_REACTIVE_POWER, 0))//
				.next(new TestCase("Second Input") //
						.timeleap(clock, 5, ChronoUnit.SECONDS)//
						.input(METER_VOLTAGE, 216_000) // [mV]
						.input(MAX_APPARENT_POWER, 10_000) // [VA]
						.output(ESS_REACTIVE_POWER, 6000))//
				.next(new TestCase("Third Input")//
						.timeleap(clock, 5, ChronoUnit.SECONDS)//
						.input(METER_VOLTAGE, 220_000) // [mV]
						.input(MAX_APPARENT_POWER, 10_000) // [VA]
						.output(ESS_REACTIVE_POWER, 2600))//
				.next(new TestCase()//
						.timeleap(clock, 5, ChronoUnit.SECONDS)//
						.input(METER_VOLTAGE, 223_000) // [mV]
						.input(MAX_APPARENT_POWER, 10_000) // [VA]
						.output(ESS_REACTIVE_POWER, 100))//
				.next(new TestCase()//
						.timeleap(clock, 5, ChronoUnit.SECONDS)//
						.input(METER_VOLTAGE, 223_200) // [mV]
						.input(MAX_APPARENT_POWER, 10_000) // [VA]
						.output(ESS_REACTIVE_POWER, 0))//
				.next(new TestCase()//
						.timeleap(clock, 5, ChronoUnit.SECONDS)//
						.input(METER_VOLTAGE, 256_800) // [mV]
						.input(MAX_APPARENT_POWER, 10_000) // [VA]
						.output(ESS_REACTIVE_POWER, 0))//
				.next(new TestCase()//
						.timeleap(clock, 5, ChronoUnit.SECONDS)//
						.input(METER_VOLTAGE, 260_000) // [mV]
						.input(MAX_APPARENT_POWER, 10_000) // [VA]
						.output(ESS_REACTIVE_POWER, -2600))//
				.next(new TestCase()//
						.timeleap(clock, 5, ChronoUnit.SECONDS)//
						.input(METER_VOLTAGE, 264_000) // [mV]
						.input(MAX_APPARENT_POWER, 10_000) // [VA]
						.output(ESS_REACTIVE_POWER, -6000))//
		;
	}
}
