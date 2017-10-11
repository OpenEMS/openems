package io.openems.test.core.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import io.openems.api.exception.WriteChannelException;
import io.openems.core.utilities.AsymmetricPower;
import io.openems.core.utilities.AsymmetricPower.ReductionType;
import io.openems.test.utils.devicenatures.UnitTestAsymmetricEssNature;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AsymmetricPowerTest {

	private static AsymmetricPower power;
	private static UnitTestAsymmetricEssNature ess;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ess = new UnitTestAsymmetricEssNature("ess0");
		power = new AsymmetricPower(ess.allowedDischarge(), ess.allowedCharge(), ess.allowedApparent(),
				ess.setActivePowerL1(), ess.setActivePowerL2(), ess.setActivePowerL3(), ess.setReactivePowerL1(),
				ess.setReactivePowerL2(), ess.setReactivePowerL3());
	}

	@Before
	public void reset() {
		ess.setActivePowerL1.shadowCopyAndReset();
		ess.setActivePowerL2.shadowCopyAndReset();
		ess.setActivePowerL3.shadowCopyAndReset();
		ess.setReactivePowerL1.shadowCopyAndReset();
		ess.setReactivePowerL2.shadowCopyAndReset();
		ess.setReactivePowerL3.shadowCopyAndReset();
	}

	@Test
	public void test1() {
		ess.allowedDischarge.setValue(10000L);
		ess.allowedCharge.setValue(-10000L);
		ess.allowedApparent.setValue(10000L);
		power.setActivePower(2000L, 3000L, 500L);
		power.setReactivePower(500, -300, 700);
		try {
			power.writePower(ReductionType.PERPHASE);
		} catch (WriteChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long activePowerL1 = ess.setActivePowerL1.peekWrite().get();
		long activePowerL2 = ess.setActivePowerL2.peekWrite().get();
		long activePowerL3 = ess.setActivePowerL3.peekWrite().get();
		long reactivePowerL1 = ess.setReactivePowerL1.peekWrite().get();
		long reactivePowerL2 = ess.setReactivePowerL2.peekWrite().get();
		long reactivePowerL3 = ess.setReactivePowerL3.peekWrite().get();
		assertEquals(2000L, activePowerL1);
		assertEquals(3000L, activePowerL2);
		assertEquals(500L, activePowerL3);
		assertEquals(500L, reactivePowerL1);
		assertEquals(-300, reactivePowerL2);
		assertEquals(700, reactivePowerL3);
	}

	@Test
	public void test2() {
		ess.allowedDischarge.setValue(10000L);
		ess.allowedCharge.setValue(-10000L);
		ess.allowedApparent.setValue(3000L);
		power.setActivePower(2000L, 3000L, 500L);
		power.setReactivePower(500, -300, 700);
		try {
			power.writePower(ReductionType.PERPHASE);
		} catch (WriteChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long activePowerL1 = ess.setActivePowerL1.peekWrite().get();
		long activePowerL2 = ess.setActivePowerL2.peekWrite().get();
		long activePowerL3 = ess.setActivePowerL3.peekWrite().get();
		long reactivePowerL1 = ess.setReactivePowerL1.peekWrite().get();
		long reactivePowerL2 = ess.setReactivePowerL2.peekWrite().get();
		long reactivePowerL3 = ess.setReactivePowerL3.peekWrite().get();
		assertEquals(866L, activePowerL1);
		assertEquals(953L, activePowerL2);
		assertEquals(500L, activePowerL3);
		assertEquals(500L, reactivePowerL1);
		assertEquals(-300, reactivePowerL2);
		assertEquals(700, reactivePowerL3);
	}

	@Test
	public void test3() {
		ess.allowedDischarge.setValue(10000L);
		ess.allowedCharge.setValue(-10000L);
		ess.allowedApparent.setValue(3000L);
		power.setActivePower(2000L, 800L, 500L);
		try {
			power.writePower(ReductionType.PERPHASE);
		} catch (WriteChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		power.setReactivePower(500, -900, 700);
		try {
			power.writePower(ReductionType.PERPHASE);
		} catch (WriteChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long activePowerL1 = ess.setActivePowerL1.peekWrite().get();
		long activePowerL2 = ess.setActivePowerL2.peekWrite().get();
		long activePowerL3 = ess.setActivePowerL3.peekWrite().get();
		long reactivePowerL1 = ess.setReactivePowerL1.peekWrite().get();
		long reactivePowerL2 = ess.setReactivePowerL2.peekWrite().get();
		long reactivePowerL3 = ess.setReactivePowerL3.peekWrite().get();
		assertEquals(1000, activePowerL1);
		assertEquals(800, activePowerL2);
		assertEquals(500, activePowerL3);
		assertEquals(0, reactivePowerL1);
		assertEquals(-600, reactivePowerL2);
		assertEquals(700, reactivePowerL3);
	}

	@Test
	public void test4() {
		ess.allowedDischarge.setValue(10000L);
		ess.allowedCharge.setValue(-800L);
		ess.allowedApparent.setValue(10000L);
		power.setActivePower(-900L, -700L, 500L);
		power.setReactivePower(500, -900, 700);
		try {
			power.writePower(ReductionType.PERPHASE);
		} catch (WriteChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long activePowerL1 = ess.setActivePowerL1.peekWrite().get();
		long activePowerL2 = ess.setActivePowerL2.peekWrite().get();
		long activePowerL3 = ess.setActivePowerL3.peekWrite().get();
		long reactivePowerL1 = ess.setReactivePowerL1.peekWrite().get();
		long reactivePowerL2 = ess.setReactivePowerL2.peekWrite().get();
		long reactivePowerL3 = ess.setReactivePowerL3.peekWrite().get();
		assertEquals(-800, activePowerL1);
		assertEquals(-600, activePowerL2);
		assertEquals(600, activePowerL3);
		assertEquals(500, reactivePowerL1);
		assertEquals(-900, reactivePowerL2);
		assertEquals(700, reactivePowerL3);
	}

	@Test
	public void test5() {
		ess.allowedDischarge.setValue(1000L);
		ess.allowedCharge.setValue(-10000L);
		ess.allowedApparent.setValue(10000L);
		power.setActivePower(1300L, -700L, 900L);
		power.setReactivePower(500, -900, 700);
		try {
			power.writePower(ReductionType.PERPHASE);
		} catch (WriteChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long activePowerL1 = ess.setActivePowerL1.peekWrite().get();
		long activePowerL2 = ess.setActivePowerL2.peekWrite().get();
		long activePowerL3 = ess.setActivePowerL3.peekWrite().get();
		long reactivePowerL1 = ess.setReactivePowerL1.peekWrite().get();
		long reactivePowerL2 = ess.setReactivePowerL2.peekWrite().get();
		long reactivePowerL3 = ess.setReactivePowerL3.peekWrite().get();
		assertEquals(1134, activePowerL1);
		assertEquals(-866, activePowerL2);
		assertEquals(734, activePowerL3);
		assertEquals(500, reactivePowerL1);
		assertEquals(-900, reactivePowerL2);
		assertEquals(700, reactivePowerL3);
	}

	@Test
	public void test6() throws WriteChannelException {
		ess.allowedDischarge.setValue(10000L);
		ess.allowedCharge.setValue(-10000L);
		ess.allowedApparent.setValue(10000L);
		ess.setActivePowerL1.pushWriteMax(200L);
		power.setActivePower(1300L, -700L, 900L);
		power.setReactivePower(500, -900, 700);
		power.writePower(ReductionType.PERPHASE);
		long activePowerL1 = ess.setActivePowerL1.peekWrite().get();
		long activePowerL2 = ess.setActivePowerL2.peekWrite().get();
		long activePowerL3 = ess.setActivePowerL3.peekWrite().get();
		long reactivePowerL1 = ess.setReactivePowerL1.peekWrite().get();
		long reactivePowerL2 = ess.setReactivePowerL2.peekWrite().get();
		long reactivePowerL3 = ess.setReactivePowerL3.peekWrite().get();
		assertEquals(200, activePowerL1);
		assertEquals(-700, activePowerL2);
		assertEquals(900, activePowerL3);
		assertEquals(500, reactivePowerL1);
		assertEquals(-900, reactivePowerL2);
		assertEquals(700, reactivePowerL3);
	}

	@Test
	public void test7() throws WriteChannelException {
		ess.allowedDischarge.setValue(10000L);
		ess.allowedCharge.setValue(-10000L);
		ess.allowedApparent.setValue(10000L);
		ess.setActivePowerL1.pushWriteMax(-300L);
		power.setActivePower(1300L, -700L, 900L);
		power.setReactivePower(500, -900, 700);
		power.writePower(ReductionType.PERPHASE);
		long activePowerL1 = ess.setActivePowerL1.peekWrite().get();
		long activePowerL2 = ess.setActivePowerL2.peekWrite().get();
		long activePowerL3 = ess.setActivePowerL3.peekWrite().get();
		long reactivePowerL1 = ess.setReactivePowerL1.peekWrite().get();
		long reactivePowerL2 = ess.setReactivePowerL2.peekWrite().get();
		long reactivePowerL3 = ess.setReactivePowerL3.peekWrite().get();
		assertEquals(-300, activePowerL1);
		assertEquals(-700, activePowerL2);
		assertEquals(900, activePowerL3);
		assertEquals(500, reactivePowerL1);
		assertEquals(-900, reactivePowerL2);
		assertEquals(700, reactivePowerL3);
	}

	@Test
	public void test8() throws WriteChannelException {
		ess.allowedDischarge.setValue(10000L);
		ess.allowedCharge.setValue(-10000L);
		ess.allowedApparent.setValue(10000L);
		ess.setActivePowerL1.pushWriteMin(200L);
		power.setActivePower(-1300L, -700L, 900L);
		power.setReactivePower(500, -900, 700);
		power.writePower(ReductionType.PERPHASE);
		long activePowerL1 = ess.setActivePowerL1.peekWrite().get();
		long activePowerL2 = ess.setActivePowerL2.peekWrite().get();
		long activePowerL3 = ess.setActivePowerL3.peekWrite().get();
		long reactivePowerL1 = ess.setReactivePowerL1.peekWrite().get();
		long reactivePowerL2 = ess.setReactivePowerL2.peekWrite().get();
		long reactivePowerL3 = ess.setReactivePowerL3.peekWrite().get();
		assertEquals(200, activePowerL1);
		assertEquals(-700, activePowerL2);
		assertEquals(900, activePowerL3);
		assertEquals(500, reactivePowerL1);
		assertEquals(-900, reactivePowerL2);
		assertEquals(700, reactivePowerL3);
	}

	@Test
	public void test9() throws WriteChannelException {
		ess.allowedDischarge.setValue(10000L);
		ess.allowedCharge.setValue(-10000L);
		ess.allowedApparent.setValue(10000L);
		ess.setActivePowerL1.pushWriteMin(-300L);
		power.setActivePower(-1300L, -700L, 900L);
		power.setReactivePower(500, -900, 700);
		power.writePower(ReductionType.PERPHASE);
		long activePowerL1 = ess.setActivePowerL1.peekWrite().get();
		long activePowerL2 = ess.setActivePowerL2.peekWrite().get();
		long activePowerL3 = ess.setActivePowerL3.peekWrite().get();
		long reactivePowerL1 = ess.setReactivePowerL1.peekWrite().get();
		long reactivePowerL2 = ess.setReactivePowerL2.peekWrite().get();
		long reactivePowerL3 = ess.setReactivePowerL3.peekWrite().get();
		assertEquals(-300, activePowerL1);
		assertEquals(-700, activePowerL2);
		assertEquals(900, activePowerL3);
		assertEquals(500, reactivePowerL1);
		assertEquals(-900, reactivePowerL2);
		assertEquals(700, reactivePowerL3);
	}
}
