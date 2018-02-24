package io.openems.test.controller.supplybusswitch;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import io.openems.api.channel.WriteChannel;
import io.openems.api.exception.InvalidValueException;
import io.openems.impl.controller.supplybusswitch.Ess;
import io.openems.impl.controller.supplybusswitch.Supplybus;
import io.openems.test.utils.channel.UnitTestWriteChannel;
import io.openems.test.utils.devicenatures.UnitTestSymmetricEssNature;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SupplyBusTest {

	private static Supplybus sb;
	private static UnitTestSymmetricEssNature ess1;
	private static UnitTestSymmetricEssNature ess2;
	private static UnitTestSymmetricEssNature ess3;
	private static UnitTestSymmetricEssNature ess4;
	private static Ess essMap1;
	private static Ess essMap2;
	private static Ess essMap3;
	private static Ess essMap4;
	private static UnitTestWriteChannel<Boolean> output1;
	private static UnitTestWriteChannel<Boolean> output2;
	private static UnitTestWriteChannel<Boolean> output3;
	private static UnitTestWriteChannel<Boolean> output4;
	private static UnitTestWriteChannel<Long> sbOnIndication;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		HashMap<Ess, WriteChannel<Boolean>> essSet = new HashMap<>();
		ess1 = new UnitTestSymmetricEssNature("ess0");
		essMap1 = new Ess(ess1);
		output1 = new UnitTestWriteChannel<>("output", "0");
		essSet.put(essMap1, output1);
		ess2 = new UnitTestSymmetricEssNature("ess1");
		essMap2 = new Ess(ess2);
		output2 = new UnitTestWriteChannel<>("output", "1");
		essSet.put(essMap2, output2);
		ess3 = new UnitTestSymmetricEssNature("ess2");
		essMap3 = new Ess(ess3);
		output3 = new UnitTestWriteChannel<>("output", "2");
		essSet.put(essMap3, output3);
		ess4 = new UnitTestSymmetricEssNature("ess3");
		essMap4 = new Ess(ess4);
		output4 = new UnitTestWriteChannel<>("output", "3");
		essSet.put(essMap4, output4);
		sbOnIndication = new UnitTestWriteChannel<>("custom", "sb1On");
		sb = new Supplybus(essSet, "sb1", essMap1, 1000L, sbOnIndication, new ArrayList<>());
	}

	@Before
	public void beforeTest() {
		ess1.setActivePower.shadowCopyAndReset();
		ess1.setReactivePower.shadowCopyAndReset();
		ess1.setWorkState.shadowCopyAndReset();
		ess1.activePower.setValue(0L);
		ess1.soc.setValue(35L);
		ess1.minSoc.setValue(15);
		ess1.chargeSoc.setValue(10);
		ess1.allowedApparent.setValue(40000L);
		ess1.allowedCharge.setValue(-40000L);
		ess1.allowedDischarge.setValue(40000L);
		ess1.gridMode.setValue(0L);
		ess2.setActivePower.shadowCopyAndReset();
		ess2.setReactivePower.shadowCopyAndReset();
		ess2.setWorkState.shadowCopyAndReset();
		ess2.activePower.setValue(0L);
		ess2.soc.setValue(35L);
		ess2.minSoc.setValue(15);
		ess2.chargeSoc.setValue(10);
		ess2.allowedApparent.setValue(40000L);
		ess2.allowedCharge.setValue(-40000L);
		ess2.allowedDischarge.setValue(40000L);
		ess2.gridMode.setValue(0L);
		ess3.setActivePower.shadowCopyAndReset();
		ess3.setReactivePower.shadowCopyAndReset();
		ess3.setWorkState.shadowCopyAndReset();
		ess3.activePower.setValue(0L);
		ess3.soc.setValue(35L);
		ess3.minSoc.setValue(15);
		ess3.chargeSoc.setValue(10);
		ess3.allowedApparent.setValue(40000L);
		ess3.allowedCharge.setValue(-40000L);
		ess3.allowedDischarge.setValue(40000L);
		ess3.gridMode.setValue(0L);
		ess4.setActivePower.shadowCopyAndReset();
		ess4.setReactivePower.shadowCopyAndReset();
		ess4.setWorkState.shadowCopyAndReset();
		ess4.activePower.setValue(0L);
		ess4.soc.setValue(35L);
		ess4.minSoc.setValue(15);
		ess4.chargeSoc.setValue(10);
		ess4.allowedApparent.setValue(40000L);
		ess4.allowedCharge.setValue(-40000L);
		ess4.allowedDischarge.setValue(40000L);
		ess4.gridMode.setValue(0L);
	}

	@Test
	public void test1() {
		// start and connect with most charged ess
		ess3.soc.setValue(49L);
		ess1.soc.setValue(60L);
		output1.setValue(false);
		output2.setValue(false);
		output3.setValue(false);
		output4.setValue(false);
		// Unknown
		try {
			sb.run();
		} catch (InvalidValueException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		output1.shadowCopyAndReset();
		output2.shadowCopyAndReset();
		output3.shadowCopyAndReset();
		output3.shadowCopyAndReset();
		sbOnIndication.shadowCopyAndReset();
		// Disconnecting
		try {
			sb.run();
		} catch (InvalidValueException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		output1.shadowCopyAndReset();
		output2.shadowCopyAndReset();
		output3.shadowCopyAndReset();
		output3.shadowCopyAndReset();
		sbOnIndication.shadowCopyAndReset();
		// Disconnected
		try {
			sb.run();
		} catch (InvalidValueException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(sbOnIndication.getWriteValue().isPresent(), true);
		assertEquals((long) sbOnIndication.getWriteValue().get(), 0);
		output1.shadowCopyAndReset();
		output2.shadowCopyAndReset();
		output3.shadowCopyAndReset();
		output3.shadowCopyAndReset();
		sbOnIndication.shadowCopyAndReset();
		// Connecting 1
		try {
			sb.run();
		} catch (InvalidValueException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// output not set switchdelay not expired
		assertEquals(output3.getWriteValue().isPresent(), false);
		output1.shadowCopyAndReset();
		output2.shadowCopyAndReset();
		output3.shadowCopyAndReset();
		output3.shadowCopyAndReset();
		sbOnIndication.shadowCopyAndReset();
		// Sleep until switchdelay expired
		try {
			Thread.sleep(1000L);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		output1.shadowCopyAndReset();
		output2.shadowCopyAndReset();
		output3.shadowCopyAndReset();
		output3.shadowCopyAndReset();
		sbOnIndication.shadowCopyAndReset();
		// Connecting 2
		try {
			sb.run();
		} catch (InvalidValueException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(output3.getWriteValue().isPresent(), true);
		assertEquals(output3.getWriteValue().get(), true);
		output3.setValue(true);
		output1.shadowCopyAndReset();
		output2.shadowCopyAndReset();
		output3.shadowCopyAndReset();
		output3.shadowCopyAndReset();
		sbOnIndication.shadowCopyAndReset();
		// Connecting 3
		try {
			sb.run();
		} catch (InvalidValueException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		output1.shadowCopyAndReset();
		output2.shadowCopyAndReset();
		output3.shadowCopyAndReset();
		output3.shadowCopyAndReset();
		sbOnIndication.shadowCopyAndReset();
		// Connected
		try {
			sb.run();
		} catch (InvalidValueException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertEquals(sbOnIndication.getWriteValue().isPresent(), true);
		assertEquals((long) sbOnIndication.getWriteValue().get(), 1L);
		output1.shadowCopyAndReset();
		output2.shadowCopyAndReset();
		output3.shadowCopyAndReset();
		output3.shadowCopyAndReset();
		sbOnIndication.shadowCopyAndReset();
	}

}
