package io.openems.edge.ess.generic.symmetric;

import static io.openems.edge.battery.api.Battery.ChannelId.CHARGE_MAX_CURRENT;
import static io.openems.edge.battery.api.Battery.ChannelId.CHARGE_MAX_VOLTAGE;
import static io.openems.edge.battery.api.Battery.ChannelId.DISCHARGE_MAX_CURRENT;
import static io.openems.edge.battery.api.Battery.ChannelId.DISCHARGE_MIN_VOLTAGE;
import static io.openems.edge.battery.api.Battery.ChannelId.SOC;
import static io.openems.edge.battery.api.Battery.ChannelId.VOLTAGE;
import static io.openems.edge.ess.generic.symmetric.EssProtection.ChannelId.EP_CHARGE_MAX_CURRENT;
import static io.openems.edge.ess.generic.symmetric.EssProtection.ChannelId.EP_DISCHARGE_MAX_CURRENT;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.batteryinverter.test.DummyManagedSymmetricBatteryInverter;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;

public class EssProtectionTest {

	@Test
	public void testEssProtection() throws Exception {
		final var ess = new EssGenericManagedSymmetricImpl();
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final var batteryInverter = new DummyManagedSymmetricBatteryInverter("batteryInverter0")//
				.withStartStop(StartStop.START) //
				.withMaxApparentPower(92000)//
				.withDcMaxVoltage(1315)//
				.withDcMinVoltage(650);
		final var battery = new DummyBattery("battery0")//
				.withStartStop(StartStop.START) //
				.withSoc(80)//
				.withChargeMaxCurrent(169)//
				.withDischargeMaxCurrent(169)//
				.withInnerResistence(200)//
				.withVoltage(700)//
				.withCurrent(0)//
				.withChargeMaxVoltage(800)//
				.withDischargeMinVoltage(593);
		var sutManaged = new ManagedSymmetricEssTest(ess) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("batteryInverter", batteryInverter) //
				.addReference("battery", battery) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setStartStopConfig(StartStopConfig.START) //
						.setBatteryInverterId("batteryInverter0") //
						.setBatteryId("battery0") //
						.build()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> clock.leap(1, MINUTES)), 10)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 700)//
						.input("battery0", SOC, 80)//
						.input("battery0", CHARGE_MAX_CURRENT, 169)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594))//
				.next(new TestCase() //
						.onBeforeProcessImage(() -> clock.leap(1, MINUTES)), 10)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 700)//
						.input("battery0", SOC, 80)//
						.input("battery0", CHARGE_MAX_CURRENT, 169)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594))//
				.next(new TestCase() //
						.onBeforeProcessImage(() -> clock.leap(1, MINUTES)), 10)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 700)//
						.input("battery0", SOC, 80)//
						.input("battery0", CHARGE_MAX_CURRENT, 169)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594))//
		;//
		assertEquals("Started|SoC:80 %|L:0 W|Allowed:-92000;92000", ess.debugLog());
		sutManaged//
				.next(new TestCase()//
						.timeleap(clock, 1, MINUTES))//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 700)//
						.input("battery0", SOC, 60)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 595)//
				)//
				.next(new TestCase()//
						.timeleap(clock, 1, SECONDS))//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 700)//
						.input("battery0", SOC, 60)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 595)//
				)//
				.next(new TestCase()//
						.timeleap(clock, 1, SECONDS))//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 700)//
						.input("battery0", SOC, 60)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 595)//
				)//
		;//
		assertEquals("Started|SoC:60 %|L:0 W|Allowed:-92000;92000", ess.debugLog());

		// Force charge
		sutManaged//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 645)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 0)//
						.output(EP_DISCHARGE_MAX_CURRENT, 167)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 645)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", CHARGE_MAX_CURRENT, 169)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 0)//
						.output(EP_DISCHARGE_MAX_CURRENT, 112)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 645)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", CHARGE_MAX_CURRENT, 169)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 0)//
						.output(EP_DISCHARGE_MAX_CURRENT, 75)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 645)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", CHARGE_MAX_CURRENT, 169)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 0)//
						.output(EP_DISCHARGE_MAX_CURRENT, 49)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 645)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", CHARGE_MAX_CURRENT, 169)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 0)//
						.output(EP_DISCHARGE_MAX_CURRENT, 32)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 645)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", CHARGE_MAX_CURRENT, 169)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 0)//
						.output(EP_DISCHARGE_MAX_CURRENT, 20)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 645)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", CHARGE_MAX_CURRENT, 169)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 0)//
						.output(EP_DISCHARGE_MAX_CURRENT, 12)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 645)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", CHARGE_MAX_CURRENT, 169)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 0)//
						.output(EP_DISCHARGE_MAX_CURRENT, 6)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 645)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", CHARGE_MAX_CURRENT, 169)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 0)//
						.output(EP_DISCHARGE_MAX_CURRENT, 3)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 645)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", CHARGE_MAX_CURRENT, 169)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 0)//
						.output(EP_DISCHARGE_MAX_CURRENT, 0)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 645)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 0)//
						.output(EP_DISCHARGE_MAX_CURRENT, 0)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 645)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 0)//
						.output(EP_DISCHARGE_MAX_CURRENT, -1)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 645)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 0)//
						.output(EP_DISCHARGE_MAX_CURRENT, -2)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 650)//
						.input("battery0", SOC, 0)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.output(EP_DISCHARGE_MAX_CURRENT, -2)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 650)//
						.input("battery0", SOC, 0)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, -2)//
						.output(EP_DISCHARGE_MAX_CURRENT, -1)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 650)//
						.input("battery0", SOC, 0)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, -2)//
						.output(EP_DISCHARGE_MAX_CURRENT, -1)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 650)//
						.input("battery0", SOC, 0)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, -2)//
						.output(EP_DISCHARGE_MAX_CURRENT, 0)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 650)//
						.input("battery0", SOC, 0)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, -2)//
						.output(EP_DISCHARGE_MAX_CURRENT, 0)//
				)//
		;//
		assertEquals("Started|SoC:0 %|L:-1235 W|Allowed:-92000;-1235", ess.debugLog());

		// normal condition
		sutManaged//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 700)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 169)//
						.output(EP_CHARGE_MAX_CURRENT, 654)//
				)//
				.next(new TestCase()//
						.timeleap(clock, 1, MINUTES))//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 700)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 169)//
						.output(EP_CHARGE_MAX_CURRENT, 599)//
				)//
				.next(new TestCase()//
						.timeleap(clock, 1, MINUTES))//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 700)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 169)//
						.output(EP_CHARGE_MAX_CURRENT, 561)//
				)//
				.next(new TestCase()//
						.timeleap(clock, 1, MINUTES))//

		;//

		// Force discharge
		sutManaged//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 796)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 0)//
						.output(EP_CHARGE_MAX_CURRENT, 383)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 796)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 0)//
						.output(EP_CHARGE_MAX_CURRENT, 261)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 796)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 0)//
						.output(EP_CHARGE_MAX_CURRENT, 178)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 796)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 0)//
						.output(EP_CHARGE_MAX_CURRENT, 122)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 796)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 0)//
						.output(EP_CHARGE_MAX_CURRENT, 83)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 796)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 0)//
						.output(EP_CHARGE_MAX_CURRENT, 57)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 796)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 0)//
						.output(EP_CHARGE_MAX_CURRENT, 38)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 796)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 0)//
						.output(EP_CHARGE_MAX_CURRENT, 26)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 796)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 0)//
						.output(EP_CHARGE_MAX_CURRENT, 18)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 796)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 0)//
						.output(EP_CHARGE_MAX_CURRENT, 12)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 796)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 0)//
						.output(EP_CHARGE_MAX_CURRENT, 8)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 796)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 0)//
						.output(EP_CHARGE_MAX_CURRENT, 5)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 796)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 0)//
						.output(EP_CHARGE_MAX_CURRENT, 3)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 798)//
						.input("battery0", SOC, 100)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 0)//
						.output(EP_CHARGE_MAX_CURRENT, 1)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 798)//
						.input("battery0", SOC, 100)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 0)//
						.output(EP_CHARGE_MAX_CURRENT, 0)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 798)//
						.input("battery0", SOC, 100)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 0)//
						.output(EP_CHARGE_MAX_CURRENT, -2)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 796)//
						.input("battery0", SOC, 100)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 0)//
						.output(EP_CHARGE_MAX_CURRENT, -1)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 796)//
						.input("battery0", SOC, 100)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 0)//
						.output(EP_CHARGE_MAX_CURRENT, -1)//
				)//
				.next(new TestCase()//
						.input("battery0", VOLTAGE, 796)//
						.input("battery0", SOC, 100)//
						.input("battery0", CHARGE_MAX_VOLTAGE, 800)//
						.input("battery0", DISCHARGE_MIN_VOLTAGE, 594)//
						.input("battery0", DISCHARGE_MAX_CURRENT, 169)//
						.input("battery0", CHARGE_MAX_CURRENT, 0)//
						.output(EP_CHARGE_MAX_CURRENT, 0)//
				)//
		;//
		assertEquals("Started|SoC:100 %|L:796 W|Allowed:796;92000", ess.debugLog());
	}
}
