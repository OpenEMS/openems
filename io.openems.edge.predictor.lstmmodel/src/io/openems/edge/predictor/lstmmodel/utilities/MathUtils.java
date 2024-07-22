package io.openems.edge.predictor.lstmmodel.utilities;

//import org.apache.commons.math3.analysis.function.Sigmoid;

public class MathUtils {

	/**
	 * Returns the hyperbolic tangent of a double value.
	 * 
	 * @param val double value
	 * @return The hyperbolic tangent of double value
	 */
	public static double tanh(double val) {
		return Math.tanh(val);
	}

	/**
	 * Returns the sigmoid of a double value.
	 * 
	 * @param val double value
	 * @return The sigmoid of a double value
	 */
	public static double sigmoid(double val) {
		return 1 / (1 + Math.pow(Math.E, -val));
	}

	/**
	 * Returns the sigmoid derivative of a double value.
	 * 
	 * @param val double value
	 * @return The sigmoid derivative of a double value
	 */
	public static double sigmoidDerivative(double val) {
		return sigmoid(val) * (1 - sigmoid(val));
	}

	/**
	 * Returns the tanh derivative of a double value.
	 * 
	 * @param val double value
	 * @return The tanh derivative of a double value
	 */
	public static double tanhDerivative(double val) {
		return 1 - Math.pow(tanh(val), 2);
	}

}
