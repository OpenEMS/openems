package io.openems.edge.ess.api;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.openems.edge.ess.core.power.ChocoPower;
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

		ChocoPower power = new ChocoPower();
		ess0.addToPower(power);

		ess0.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS, 1000);

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

		ChocoPower power = new ChocoPower();
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

		ChocoPower power = new ChocoPower();
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

		ChocoPower power = new ChocoPower();
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
				assertEquals(500, activePower);
				assertEquals(250, reactivePower);
				totalActivePower.addAndGet(activePower);
				totalReactivePower.addAndGet(reactivePower);
			}
		}.maxApparentPower(9999).allowedCharge(-9999).allowedDischarge(9999);

		ManagedAsymmetricEssDummy ess2 = new ManagedAsymmetricEssDummy() {
			@Override
			public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
					int activePowerL3, int reactivePowerL3) {
				assertEquals(166, activePowerL1);
				assertEquals(166, activePowerL2);
				assertEquals(167, activePowerL3);
				assertEquals(83, reactivePowerL1);
				assertEquals(83, reactivePowerL2);
				assertEquals(83, reactivePowerL3);
				assertEquals(499 /* caused by rounding errors */, activePowerL1 + activePowerL2 + activePowerL3);
				assertEquals(249 /* caused by rounding errors */, reactivePowerL1 + reactivePowerL2 + reactivePowerL3);
				totalActivePower.addAndGet(activePowerL1 + activePowerL2 + activePowerL3);
				totalReactivePower.addAndGet(reactivePowerL1 + reactivePowerL2 + reactivePowerL3);
			}
		}.maxApparentPower(9999).allowedCharge(-9999).allowedDischarge(9999);

		EssClusterDummy ess0 = new EssClusterDummy(ess1, ess2);

		ChocoPower power = new ChocoPower();
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

		ChocoPower power = new ChocoPower();
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

		ChocoPower power = new ChocoPower();
		ess0.addToPower(power);

		assertEquals(-1000, ess0.getPower().getMaxActivePower());
	}
}
