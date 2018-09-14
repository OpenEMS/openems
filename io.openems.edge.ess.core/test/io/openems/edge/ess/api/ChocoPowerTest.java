package io.openems.edge.ess.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.openems.edge.ess.core.power.ChocoPower;
import io.openems.edge.ess.core.power.PowerComponent;
import io.openems.edge.ess.power.api.ConstraintType;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class ChocoPowerTest {

	public static final double DELTA_IN_PERCENT = 10; //

	@Test
	public void testSymmetricActivePower() throws Exception {
		ManagedSymmetricEssDummy ess0 = new ManagedSymmetricEssDummy() {
			@Override
			public void applyPower(int activePower, int reactivePower) {
				assertEquals(1000, activePower);
				assertEquals(0, reactivePower);
			}
		}.maxApparentPower(9999).allowedCharge(-9999).allowedDischarge(9999);

		PowerComponent c = new PowerComponent();
		ChocoPower power = new ChocoPower(c);
		ess0.addToPower(power);

		ess0.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 1000);

		power.applyPower();
	}

	@Test
	public void testCommercial40DischargeSymmetricActivePower() throws Exception {
		AtomicInteger runNo = new AtomicInteger(0);

		ManagedSymmetricEssDummy ess0 = new ManagedSymmetricEssDummy() {
			@Override
			public void applyPower(int activePower, int reactivePower) {
				switch (runNo.get()) {
				case 0:
					assertEquals(700, activePower);
					assertEquals(0, reactivePower);
					break;
				case 1:
					assertEquals(500, activePower);
					assertEquals(0, reactivePower);
					break;
				case 2:
					assertEquals(-400, activePower);
					assertEquals(0, reactivePower);
					break;
				case 3:
					assertEquals(-300, activePower);
					assertEquals(0, reactivePower);
					break;
				}
			}
		}.maxApparentPower(40000).allowedCharge(-26000).allowedDischarge(40000).precision(100).soc(50);

		PowerComponent c = new PowerComponent();
		ChocoPower power = new ChocoPower(c);
		ess0.addToPower(power);

		ess0.addPowerConstraint(ConstraintType.STATIC, Phase.ALL, Pwr.REACTIVE, Relationship.GREATER_OR_EQUALS, -10000);
		ess0.addPowerConstraint(ConstraintType.STATIC, Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, 10000);

		ess0.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 610);
		ess0.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0);

		power.applyPower();

		power.initializeNextCycle();
		runNo.incrementAndGet();
		ess0.soc(49);

		ess0.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 590);
		ess0.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0);

		power.applyPower();

		power.initializeNextCycle();
		runNo.incrementAndGet();

		ess0.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -310);
		ess0.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0);

		power.applyPower();

		power.initializeNextCycle();
		runNo.incrementAndGet();
		ess0.soc(50);

		ess0.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, -310);
		ess0.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 0);

		power.applyPower();
	}

	@Test
	public void testSymmetricReactivePower() throws Exception {
		ManagedSymmetricEssDummy ess0 = new ManagedSymmetricEssDummy() {
			@Override
			public void applyPower(int activePower, int reactivePower) {
				assertEquals(0, activePower);
				assertEquals(1000, reactivePower);
			}
		}.maxApparentPower(9999).allowedCharge(-9999).allowedDischarge(9999);

		PowerComponent c = new PowerComponent();
		ChocoPower power = new ChocoPower(c);
		ess0.addToPower(power);

		ess0.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 1000);

		power.applyPower();
	}

	@Test
	public void testSymmetric() throws Exception {
		ManagedSymmetricEssDummy ess0 = new ManagedSymmetricEssDummy() {
			@Override
			public void applyPower(int activePower, int reactivePower) {
				assertEquals(1000, activePower);
				assertEquals(500, reactivePower);
			}
		}.maxApparentPower(9999).allowedCharge(-9999).allowedDischarge(9999);

		PowerComponent c = new PowerComponent();
		ChocoPower power = new ChocoPower(c);
		ess0.addToPower(power);

		ess0.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 1000);
		ess0.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 500);

		power.applyPower();
	}

	@Test
	public void testAsymmetric() throws Exception {
		ManagedAsymmetricEssDummy ess0 = new ManagedAsymmetricEssDummy() {
			@Override
			public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
					int activePowerL3, int reactivePowerL3) {
				assertEquals(999 /* caused by rounding errors */, activePowerL1 + activePowerL2 + activePowerL3);
				assertEquals(499 /* caused by rounding errors */, reactivePowerL1 + reactivePowerL2 + reactivePowerL3);
			}
		}.maxApparentPower(9999).allowedCharge(-9999).allowedDischarge(9999);

		PowerComponent c = new PowerComponent();
		ChocoPower power = new ChocoPower(c);
		ess0.addToPower(power);

		ess0.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 1000);
		ess0.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 500);

		power.applyPower();
	}

	@Test
	public void testCluster() throws Exception {
		AtomicInteger totalActivePower = new AtomicInteger();
		AtomicInteger totalReactivePower = new AtomicInteger();

		ManagedSymmetricEssDummy ess1 = new ManagedSymmetricEssDummy() {
			@Override
			public void applyPower(int activePower, int reactivePower) {
				assertTrue(activePower > 498 && activePower < 502);
				assertTrue(reactivePower > 248 && reactivePower < 252);
				totalActivePower.addAndGet(activePower);
				totalReactivePower.addAndGet(reactivePower);
			}
		}.maxApparentPower(9999).allowedCharge(-9999).allowedDischarge(9999);

		ManagedAsymmetricEssDummy ess2 = new ManagedAsymmetricEssDummy() {
			@Override
			public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
					int activePowerL3, int reactivePowerL3) {
				assertTrue(activePowerL1 > 165 && activePowerL1 < 168);
				assertTrue(activePowerL2 > 165 && activePowerL2 < 168);
				assertTrue(activePowerL3 > 165 && activePowerL3 < 168);
				assertTrue(reactivePowerL1 > 81 && reactivePowerL1 < 84);
				assertTrue(reactivePowerL2 > 81 && reactivePowerL2 < 84);
				assertTrue(reactivePowerL3 > 81 && reactivePowerL3 < 84);
				int pSum = activePowerL1 + activePowerL2 + activePowerL3;
				assertTrue(pSum > 497 && pSum < 501);
				int qSum = reactivePowerL1 + reactivePowerL2 + reactivePowerL3;
				assertTrue(qSum > 247 && qSum < 251);
				totalActivePower.addAndGet(pSum);
				totalReactivePower.addAndGet(qSum);
			}
		}.maxApparentPower(9999).allowedCharge(-9999).allowedDischarge(9999);

		EssClusterDummy ess0 = new EssClusterDummy(ess1, ess2);

		PowerComponent c = new PowerComponent();
		ChocoPower power = new ChocoPower(c);
		ess1.addToPower(power);
		ess2.addToPower(power);
		ess0.addToPower(power);

		ess0.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 1000);
		ess0.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.REACTIVE, Relationship.EQUALS, 500);

		power.applyPower();

		assertEquals(999 /* caused by rounding errors */, totalActivePower.get());
		assertEquals(499 /* caused by rounding errors */, totalReactivePower.get());
	}

	@Test
	public void testMaxActivePower() throws Exception {
		ManagedAsymmetricEssDummy ess0 = new ManagedAsymmetricEssDummy() {
			@Override
			public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
					int activePowerL3, int reactivePowerL3) {
			}
		}.maxApparentPower(2000).allowedCharge(-9999).allowedDischarge(1000);

		PowerComponent c = new PowerComponent();
		ChocoPower power = new ChocoPower(c);
		ess0.addToPower(power);

		assertEquals(999 /* caused by rounding errors */, ess0.getPower().getMaxActivePower());
	}

	@Test
	public void testMinActivePower() throws Exception {
		ManagedAsymmetricEssDummy ess0 = new ManagedAsymmetricEssDummy() {
			@Override
			public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
					int activePowerL3, int reactivePowerL3) {
			}
		}.maxApparentPower(2000).allowedCharge(-1000).allowedDischarge(9999);

		PowerComponent c = new PowerComponent();
		ChocoPower power = new ChocoPower(c);
		ess0.addToPower(power);

		int min = ess0.getPower().getMinActivePower();
		assertTrue(min > -1002 && min < -998);
	}
}
