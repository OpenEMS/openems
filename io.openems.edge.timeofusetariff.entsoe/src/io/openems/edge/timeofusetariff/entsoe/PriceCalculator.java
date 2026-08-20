package io.openems.edge.timeofusetariff.entsoe;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import com.fathzer.soft.javaluator.StaticVariableSet;

public abstract class PriceCalculator {

	private final String expression;
	private final DoubleEvaluator evaluator = new DoubleEvaluator();

	public PriceCalculator(String expression) {
		this.expression = expression;
	}

	protected final double evaluate(StaticVariableSet<Double> vars) {
		return this.evaluator.evaluate(this.expression, vars);
	}
}
