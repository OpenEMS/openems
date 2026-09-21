package io.openems.edge.timeofusetariff.entsoe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathzer.soft.javaluator.StaticVariableSet;
import com.google.common.annotations.VisibleForTesting;

/**
 * A {@link PriceCalculator} with variables 'X' and 'Y'.
 */
public class PriceCalculatorXY extends PriceCalculator {

	private static final Logger LOG = LoggerFactory.getLogger(PriceCalculatorXY.class);
	private static final String DEFAULT_EXPRESSION = "x + y";

	/**
	 * Builds a {@link PriceCalculatorXY} from a String expression. Validates the
	 * expression; if it fails, falls back to default.
	 * 
	 * @param expression the expression
	 * @return the {@link PriceCalculatorXY}
	 */
	public static PriceCalculatorXY fromExpression(String expression) {
		try {
			final var priceCalculator = new PriceCalculatorXY(expression);
			priceCalculator.calculate(1., 1.);
			return priceCalculator;
		} catch (Exception e) {
			LOG.warn("Calculate expression [" + expression + "] failed. Falling back to [" + DEFAULT_EXPRESSION + "].");
			return new PriceCalculatorXY(DEFAULT_EXPRESSION);
		}

	}

	final StaticVariableSet<Double> vars = new StaticVariableSet<>();

	@VisibleForTesting
	PriceCalculatorXY(String expression) {
		super(expression);
	}

	/**
	 * Executes the price calculation.
	 * 
	 * @param x the variable 'x'
	 * @param y the variable 'y'
	 * @return the result
	 */
	public synchronized double calculate(double x, double y) {
		this.vars.set("x", x);
		this.vars.set("y", y);
		return this.evaluate(this.vars);
	}
}
