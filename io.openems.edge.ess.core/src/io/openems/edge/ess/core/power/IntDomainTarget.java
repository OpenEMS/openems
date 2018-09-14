package io.openems.edge.ess.core.power;

import java.util.Map;

import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.variables.IntVar;

// This is largely copied from org.chocosolver.solver.search.strategy.selectors.values.IntDomainMiddle

public class IntDomainTarget implements IntValueSelector {

	// VARIABLES
	public final Map<IntVar, Integer> targets;

	public IntDomainTarget(Map<IntVar, Integer> targets) {
		this.targets = targets;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int selectValue(IntVar var) {
		Integer target = targets.get(var);
		if (target == null) {
			throw new IllegalArgumentException("IntVar [" + var + "] was not added to IntDomainTarget");
		}
		if (var.hasEnumeratedDomain()) {
			if (!var.contains(target)) {
				int a = var.previousValue(target);
				int b = var.nextValue(target);
//				 System.out.println("select " + var.getName() + " for [" + a + " < " + target
//				 + " < " + b + "]; boundaries [" + var.getLB() + ", " + var.getUB() + "]");
				if (a == Integer.MIN_VALUE) {
					return b;
				} else if (b == Integer.MAX_VALUE) {
					return a;
				} else if (target - a < b - target) {
					return a;
				} else if (target - a > b - target) {
					return b;
				} else { // tie break
					return a;
				}
			}
		}
		return target;
	}
}
