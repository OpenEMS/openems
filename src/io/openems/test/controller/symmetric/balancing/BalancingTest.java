package io.openems.test.controller.symmetric.balancing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.api.exception.WriteChannelException;
import io.openems.impl.controller.symmetric.balancing.BalancingController;
import io.openems.impl.controller.symmetric.balancing.Ess;
import io.openems.impl.controller.symmetric.balancing.Meter;
import io.openems.test.utils.devicenatures.UnitTestSymmetricEssNature;
import io.openems.test.utils.devicenatures.UnitTestSymmetricMeterNature;

public class BalancingTest {

	private static BalancingController controller;
	private static UnitTestSymmetricEssNature ess;
	private static UnitTestSymmetricMeterNature meter;
	private static Ess essThingMap;
	private static Meter meterThingMap;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ess = new UnitTestSymmetricEssNature("ess0");
		meter = new UnitTestSymmetricMeterNature("meter0");
		controller = new BalancingController();
		essThingMap = new Ess(ess);
		meterThingMap = new Meter(meter);
		List<Ess> essSet = new ArrayList<Ess>();
		essSet.add(essThingMap);
		controller.esss.updateValue(essSet, true);
		controller.meter.updateValue(meterThingMap, true);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {}

	@Test
	public void test() {
		ess.activePower.setValue(1000L);
		meter.activePower.setValue(-500L);
		ess.soc.setValue(35L);
		ess.minSoc.setValue(15);
		ess.chargeSoc.setValue(10);
		ess.allowedApparent.setValue(40000L);
		ess.allowedCharge.setValue(-40000L);
		ess.allowedDischarge.setValue(40000L);
		try {
			ess.setActivePower.pushWriteMax(40000L);

			ess.setActivePower.pushWriteMin(-40000L);
			controller.run();
			long setActivePower = ess.setActivePower.getWriteValue().get();
			assertEquals(400L, setActivePower);
		} catch (WriteChannelException e) {
			fail("unexpected WriteChannelException" + e.getMessage());
		}
	}

}
