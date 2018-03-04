package io.openems.test.controller;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.thing.Thing;
import io.openems.common.utils.Log;
import io.openems.impl.controller.thermalpowerstation.Ess;
import io.openems.impl.controller.thermalpowerstation.Meter;
import io.openems.impl.controller.thermalpowerstation.ThermalPowerStationController;
import io.openems.test.utils.channel.UnitTestWriteChannel;
import io.openems.test.utils.devicenatures.UnitTestAsymmetricEssNature;
import io.openems.test.utils.devicenatures.UnitTestSymmetricMeterNature;

public class ThermalPowerStationTest {

	private static ThermalPowerStationController controller;
	private static UnitTestAsymmetricEssNature ess;
	private static UnitTestWriteChannel<Boolean> outputChannel;
	private static UnitTestSymmetricMeterNature meter;
	private static Ess essThingMap;
	private static Meter meterThingMap;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ess = new UnitTestAsymmetricEssNature("ess0");
		meter = new UnitTestSymmetricMeterNature("meter0");
		controller = new ThermalPowerStationController();
		outputChannel = new UnitTestWriteChannel<>("0", new Thing() {

			@Override
			public String id() {
				return "output0";
			}

			@Override
			public ThingStateChannels getStateChannel() {
				return null;
			}
		});
		essThingMap = new Ess(ess);
		meterThingMap = new Meter(meter);
		controller.ess.updateValue(essThingMap, true);
		List<Meter> meters = new ArrayList<>();
		meters.add(meterThingMap);
		controller.meters.updateValue(meters, true);
		controller.outputChannelOpt = Optional.of(outputChannel);
		controller.productionLimit.updateValue(1000L, true);
		controller.limitTimeRange.updateValue(1L, true);
		controller.minSoc.updateValue(15L, true);
		controller.maxSoc.updateValue(95L, true);
		controller.invertOutput.updateValue(false, true);
	}

	@Before
	public void beforeTest() {
		outputChannel.shadowCopyAndReset();
	}

	@Test
	public void test1() {
		ess.soc.setValue(15L);
		meter.activePower.setValue(0L);
		outputChannel.setValue(false);
		controller.run();
		assertEquals(false, outputChannel.getWriteValue().isPresent());
	}

	@Test
	public void test2() {
		ess.soc.setValue(15L);
		meter.activePower.setValue(0L);
		outputChannel.setValue(true);
		outputChannel.shadowCopyAndReset();
		// OFF
		controller.run();
		assertEquals(false, outputChannel.getWriteValue().isPresent());
		outputChannel.shadowCopyAndReset();
		// SWITCH OFF
		controller.run();
		assertEquals(true, outputChannel.getWriteValue().isPresent());
		boolean output = outputChannel.getWriteValue().get();
		assertEquals(false, output);
		outputChannel.setValue(false);
		outputChannel.shadowCopyAndReset();
		// SWITCH OFF
		controller.run();
		assertEquals(false, outputChannel.getWriteValue().isPresent());
		outputChannel.shadowCopyAndReset();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// OFF
		controller.run();
		assertEquals(false, outputChannel.getWriteValue().isPresent());
		outputChannel.shadowCopyAndReset();
		// SWITCH ON
		controller.run();
		assertEquals(true, outputChannel.getWriteValue().isPresent());
		output = outputChannel.getWriteValue().get();
		assertEquals(true, output);
	}

	@Test
	public void test3() {
		ess.soc.setValue(15L);
		meter.activePower.setValue(5000L);
		outputChannel.setValue(true);
		outputChannel.shadowCopyAndReset();
		// OFF
		controller.run();
		assertEquals(false, outputChannel.getWriteValue().isPresent());
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Log.error(e.getMessage());
		}
		controller.run();
		outputChannel.shadowCopyAndReset();
		assertEquals(false, outputChannel.getWriteValue().isPresent());
		outputChannel.shadowCopyAndReset();
		controller.run();
		assertEquals(true, outputChannel.getWriteValue().isPresent());
		boolean output = outputChannel.getWriteValue().get();
		assertEquals(false, output);
	}

}
