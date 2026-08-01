package io.openems.edge.timeofusetariff.entsoe;

import static io.openems.common.utils.StringUtils.isNullOrBlank;

import com.fathzer.soft.javaluator.DoubleEvaluator;
import com.fathzer.soft.javaluator.StaticVariableSet;

public class PriceCalculator {

	private final String expression;
	private final DoubleEvaluator evaluator;
	private final StaticVariableSet<Double> vars = new StaticVariableSet<>();

	public PriceCalculator(String expression) {
		this.expression = expression;
		this.evaluator = isNullOrBlank(expression) //
				? null //
				: new DoubleEvaluator();
	}

	/**
	 * Executes the price calculation.
	 * 
	 * @param x the variable 'x'
	 * @param y the variable 'y'
	 * @return the result
	 */
	public double calculate(double x, double y) {
		if (this.evaluator == null) {
			return x + y;
		} else {
			this.vars.set("x", x);
			this.vars.set("y", y);
			return this.evaluator.evaluate(this.expression, this.vars);
		}
	}

}
