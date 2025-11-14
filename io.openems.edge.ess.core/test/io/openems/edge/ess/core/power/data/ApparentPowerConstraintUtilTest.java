package io.openems.edge.ess.core.power.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;

public class ApparentPowerConstraintUtilTest {

	private Coefficients coefficients;
	private String essId;
	private SingleOrAllPhase phase;

	@Before
	public void before() {
		this.coefficients = new Coefficients();
		Set<String> essIds = new HashSet<>();
		essIds.add("ess0");
		this.coefficients.initialize(true, essIds);
		this.essId = "ess0";
		this.phase = SingleOrAllPhase.ALL;
	}

	@Test
	public void testGenerateConstraintsWithPositiveApparentPower() throws OpenemsException {
		var apparentPower = 10000.0;
		List<Constraint> constraints = ApparentPowerConstraintUtil.generateConstraints(//
				this.coefficients, this.essId, this.phase, apparentPower);

		assertNotNull("Constraints should not be null", constraints);
		assertTrue("Should generate multiple constraints for circle sections", constraints.size() > 0);

		// Verify no constraints have NaN or Infinity values
		for (Constraint constraint : constraints) {
			assertNotNull("Constraint should not be null", constraint);
			assertTrue("Constraint value should be present", constraint.getValue().isPresent());
			double value = constraint.getValue().get();
			assertTrue("Constraint value should not be NaN: " + value, !Double.isNaN(value));
			assertTrue("Constraint value should not be Infinity: " + value, !Double.isInfinite(value));
		}
	}

	@Test
	public void testGenerateConstraintsWithZeroApparentPower() throws OpenemsException {
		var apparentPower = 0.0;
		List<Constraint> constraints = ApparentPowerConstraintUtil.generateConstraints(//
				this.coefficients, this.essId, this.phase, apparentPower);

		assertNotNull("Constraints should not be null", constraints);
		assertEquals("Should generate 2 constraints for zero apparent power", 2, constraints.size());
	}

	@Test
	public void testGenerateConstraintsWithNegativeApparentPower() throws OpenemsException {
		var apparentPower = -1000.0;
		List<Constraint> constraints = ApparentPowerConstraintUtil.generateConstraints(//
				this.coefficients, this.essId, this.phase, apparentPower);

		assertNotNull("Constraints should not be null", constraints);
		assertEquals("Should generate 2 constraints for negative apparent power", 2, constraints.size());
	}

	@Test
	public void testGenerateConstraintsHandlesVerticalLines() throws OpenemsException {
		// Test with a large apparent power to ensure we test edge cases
		// where points might have very similar x-coordinates
		var apparentPower = 50000.0;
		List<Constraint> constraints = ApparentPowerConstraintUtil.generateConstraints(//
				this.coefficients, this.essId, this.phase, apparentPower);

		assertNotNull("Constraints should not be null", constraints);
		assertTrue("Should generate constraints", constraints.size() > 0);

		// Verify all constraints are valid (no division by zero occurred)
		for (Constraint constraint : constraints) {
			assertTrue("Constraint value should be present", constraint.getValue().isPresent());
			double value = constraint.getValue().get();
			assertTrue("Constraint value should be finite: " + value, Double.isFinite(value));

			// Check coefficients are also finite
			var linearCoefficients = constraint.getCoefficients();
			for (var coeff : linearCoefficients) {
				assertTrue("Coefficient should be finite: " + coeff.getValue(), Double.isFinite(coeff.getValue()));
			}
		}
	}

	@Test
	public void testGenerateConstraintsWithSmallApparentPower() throws OpenemsException {
		// Test with very small apparent power
		var apparentPower = 0.1;
		List<Constraint> constraints = ApparentPowerConstraintUtil.generateConstraints(//
				this.coefficients, this.essId, this.phase, apparentPower);

		assertNotNull("Constraints should not be null", constraints);
		assertTrue("Should generate constraints for small apparent power", constraints.size() > 0);

		// Verify no division by zero issues with small values
		for (Constraint constraint : constraints) {
			assertTrue("Constraint value should be present", constraint.getValue().isPresent());
			double value = constraint.getValue().get();
			assertTrue("Constraint value should be finite: " + value, Double.isFinite(value));
		}
	}

	@Test
	public void testGenerateConstraintsWithLargeApparentPower() throws OpenemsException {
		// Test with very large apparent power
		var apparentPower = 1000000.0;
		List<Constraint> constraints = ApparentPowerConstraintUtil.generateConstraints(//
				this.coefficients, this.essId, this.phase, apparentPower);

		assertNotNull("Constraints should not be null", constraints);
		assertTrue("Should generate constraints for large apparent power", constraints.size() > 0);

		// Verify no overflow or precision issues
		for (Constraint constraint : constraints) {
			assertTrue("Constraint value should be present", constraint.getValue().isPresent());
			double value = constraint.getValue().get();
			assertTrue("Constraint value should be finite: " + value, Double.isFinite(value));
		}
	}
}
