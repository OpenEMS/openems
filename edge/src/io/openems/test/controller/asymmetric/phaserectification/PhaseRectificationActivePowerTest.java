package io.openems.test.controller.asymmetric.phaserectification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.api.exception.WriteChannelException;
import io.openems.impl.controller.asymmetric.phaserectification.Ess;
import io.openems.impl.controller.asymmetric.phaserectification.Meter;
import io.openems.impl.controller.asymmetric.phaserectification.PhaseRectificationActivePowerController;
import io.openems.test.utils.devicenatures.UnitTestAsymmetricEssNature;
import io.openems.test.utils.devicenatures.UnitTestAsymmetricMeterNature;

public class PhaseRectificationActivePowerTest {

	private static PhaseRectificationActivePowerController controller;
	private static UnitTestAsymmetricEssNature ess;
	private static UnitTestAsymmetricMeterNature meter;
	private static Ess essThingMap;
	private static Meter meterThingMap;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ess = new UnitTestAsymmetricEssNature("ess0");
		meter = new UnitTestAsymmetricMeterNature("meter0");
		controller = new PhaseRectificationActivePowerController();
		essThingMap = new Ess(ess);
		meterThingMap = new Meter(meter);
		controller.ess.updateValue(essThingMap, true);
		controller.meter.updateValue(meterThingMap, true);
	}

	@Before
	public void beforeTest() {
		ess.setActivePowerL1.shadowCopyAndReset();
		ess.setActivePowerL2.shadowCopyAndReset();
		ess.setActivePowerL3.shadowCopyAndReset();
		ess.setReactivePowerL1.shadowCopyAndReset();
		ess.setReactivePowerL2.shadowCopyAndReset();
		ess.setReactivePowerL3.shadowCopyAndReset();
		ess.setWorkState.shadowCopyAndReset();
		ess.activePowerL1.setValue(0L);
		ess.activePowerL2.setValue(0L);
		ess.activePowerL3.setValue(0L);
		meter.activePowerL1.setValue(0L);
		meter.activePowerL2.setValue(0L);
		meter.activePowerL3.setValue(0L);
		ess.soc.setValue(35L);
		ess.minSoc.setValue(15);
		ess.chargeSoc.setValue(10);
		ess.allowedApparent.setValue(100000L);
		ess.allowedCharge.setValue(0L);
		ess.allowedDischarge.setValue(0L);
	}

	@Test
	public void test1() {
		ess.activePowerL1.setValue(1000L);
		ess.activePowerL2.setValue(100L);
		ess.activePowerL3.setValue(750L);
		meter.activePowerL1.setValue(-500L);
		meter.activePowerL2.setValue(-500L);
		meter.activePowerL3.setValue(-500L);
		try {
			ess.setActivePowerL1.pushWriteMax(33333L);
			ess.setActivePowerL1.pushWriteMin(-33333L);
			ess.setActivePowerL2.pushWriteMax(33333L);
			ess.setActivePowerL2.pushWriteMin(-33333L);
			ess.setActivePowerL3.pushWriteMax(33333L);
			ess.setActivePowerL3.pushWriteMin(-33333L);
			controller.run();
			long setActivePowerL1 = ess.setActivePowerL1.getWriteValue().get();
			long setActivePowerL2 = ess.setActivePowerL2.getWriteValue().get();
			long setActivePowerL3 = ess.setActivePowerL3.getWriteValue().get();
			assertEquals(384L, setActivePowerL1);
			assertEquals(-516L, setActivePowerL2);
			assertEquals(134L, setActivePowerL3);
		} catch (WriteChannelException e) {
			fail("unexpected WriteChannelException" + e.getMessage());
		}
	}

}
