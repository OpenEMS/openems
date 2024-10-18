package io.openems.edge.controller.ess.reactivepowervoltagecharacteristic;

import static io.openems.common.utils.JsonUtils.buildJsonArray;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.common.test.TestUtils.createDummyClock;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_REACTIVE_POWER_EQUALS;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.MAX_APPARENT_POWER;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.VOLTAGE;
import static java.time.temporal.ChronoUnit.SECONDS;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class ControllerEssReactivePowerVoltageCharacteristicImplTest {

	@Test
	public void test() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerEssReactivePowerVoltageCharacteristicImpl())//
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0")) //
				.activate(MyConfig.create()//
						.setId("ctrl0")//
						.setEssId("ess0")//
						.setMeterId("meter0")//
						.setNominalVoltage(240)//
						.setWaitForHysteresis(5)//
						.setPowerVoltConfig(buildJsonArray()//
								.add(buildJsonObject()//
										.addProperty("voltageRatio", 0.9) //
										.addProperty("percent", 60) //
										.build()) //
								.add(buildJsonObject()//
										.addProperty("voltageRatio", 0.93) //
										.addProperty("percent", 0) //
										.build()) //
								.add(buildJsonObject()//
										.addProperty("voltageRatio", 1.07) //
										.addProperty("percent", 0) //
										.build()) //
								.add(buildJsonObject()//
										.addProperty("voltageRatio", 1.1) //
										.addProperty("percent", -60) //
										.build() //
								).build().toString() //
						).build()) //
				.next(new TestCase("First Input") //
						.input("meter0", VOLTAGE, 240_000) // [mV]
						.input("ess0", MAX_APPARENT_POWER, 10_000)) // [VA]
				.next(new TestCase() //
						.output("ess0", SET_REACTIVE_POWER_EQUALS, 0))//
				.next(new TestCase("Second Input") //
						.timeleap(clock, 5, SECONDS)//
						.input("meter0", VOLTAGE, 216_000) // [mV]
						.input("ess0", MAX_APPARENT_POWER, 10_000) // [VA]
						.output("ess0", SET_REACTIVE_POWER_EQUALS, 6000))//
				.next(new TestCase("Third Input")//
						.timeleap(clock, 5, SECONDS)//
						.input("meter0", VOLTAGE, 220_000) // [mV]
						.input("ess0", MAX_APPARENT_POWER, 10_000) // [VA]
						.output("ess0", SET_REACTIVE_POWER_EQUALS, 2600))//
				.next(new TestCase()//
						.timeleap(clock, 5, SECONDS)//
						.input("meter0", VOLTAGE, 223_000) // [mV]
						.input("ess0", MAX_APPARENT_POWER, 10_000) // [VA]
						.output("ess0", SET_REACTIVE_POWER_EQUALS, 100))//
				.next(new TestCase()//
						.timeleap(clock, 5, SECONDS)//
						.input("meter0", VOLTAGE, 223_200) // [mV]
						.input("ess0", MAX_APPARENT_POWER, 10_000) // [VA]
						.output("ess0", SET_REACTIVE_POWER_EQUALS, 0))//
				.next(new TestCase()//
						.timeleap(clock, 5, SECONDS)//
						.input("meter0", VOLTAGE, 256_800) // [mV]
						.input("ess0", MAX_APPARENT_POWER, 10_000) // [VA]
						.output("ess0", SET_REACTIVE_POWER_EQUALS, 0))//
				.next(new TestCase()//
						.timeleap(clock, 5, SECONDS)//
						.input("meter0", VOLTAGE, 260_000) // [mV]
						.input("ess0", MAX_APPARENT_POWER, 10_000) // [VA]
						.output("ess0", SET_REACTIVE_POWER_EQUALS, -2600))//
				.next(new TestCase()//
						.timeleap(clock, 5, SECONDS)//
						.input("meter0", VOLTAGE, 264_000) // [mV]
						.input("ess0", MAX_APPARENT_POWER, 10_000) // [VA]
						.output("ess0", SET_REACTIVE_POWER_EQUALS, -6000)) //
				.deactivate();
	}
}
