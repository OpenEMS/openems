package io.openems.test.controller;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.api.thing.Thing;
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
		});
		essThingMap = new Ess(ess);
		meterThingMap = new Meter(meter);
		controller.ess.updateValue(essThingMap, true);
		List<Meter> meters = new ArrayList<>();
		meters.add(meterThingMap);
		controller.meters.updateValue(meters, true);
		controller.outputChannel = outputChannel;
		controller.productionLimit.updateValue(1000L, true);
		controller.limitTimeRange.updateValue(15L, true);
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
		controller.run();
		assertEquals(false, outputChannel.getWriteValue().isPresent());
		outputChannel.shadowCopyAndReset();
		controller.run();
		assertEquals(true, outputChannel.getWriteValue().isPresent());
		boolean output = outputChannel.getWriteValue().get();
		assertEquals(false, output);
		outputChannel.setValue(false);
		outputChannel.shadowCopyAndReset();
		controller.run();
		assertEquals(false, outputChannel.getWriteValue().isPresent());
		outputChannel.shadowCopyAndReset();
		controller.run();
		assertEquals(false, outputChannel.getWriteValue().isPresent());
		outputChannel.shadowCopyAndReset();
		controller.run();
		assertEquals(true, outputChannel.getWriteValue().isPresent());
		output = outputChannel.getWriteValue().get();
		assertEquals(true, output);
	}

}
