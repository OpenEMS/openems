package io.openems.edge.timeofusetariff.entsoe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.soft.javaluator.StaticVariableSet;
import com.google.common.annotations.VisibleForTesting;

/**
 * A {@link PriceCalculator} with variable 'X'.
 */
public class PriceCalculatorX extends PriceCalculator {

	private static final Logger LOG = LoggerFactory.getLogger(PriceCalculatorX.class);
	private static final String DEFAULT_EXPRESSION = "x";

	/**
	 * Builds a {@link PriceCalculatorX} from a String expression. Validates the
	 * expression; if it fails, falls back to default.
	 * 
	 * @param expression the expression
	 * @return the {@link PriceCalculatorX}
	 */
	public static PriceCalculatorX fromExpression(String expression) {
		try {
			final var priceCalculator = new PriceCalculatorX(expression);
			priceCalculator.calculate(1.);
			return priceCalculator;
		} catch (Exception e) {
			LOG.warn("Calculate expression [" + expression + "] failed. Falling back to [" + DEFAULT_EXPRESSION + "].");
			return new PriceCalculatorX(DEFAULT_EXPRESSION);
		}
	}

	final StaticVariableSet<Double> vars = new StaticVariableSet<>();

	@VisibleForTesting
	PriceCalculatorX(String expression) {
		super(expression);
	}

	/**
	 * Executes the price calculation.
	 * 
	 * @param x the variable 'x'
	 * @return the result
	 */
	public synchronized double calculate(double x) {
		this.vars.set("x", x);
		return this.evaluate(this.vars);
	}
}
