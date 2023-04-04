package io.openems.edge.predictor.lstmmodel.util;

//import org.apache.commons.math3.analysis.function.Sigmoid;

public class MathUtils {

	public static double tanh(double val) {
		return Math.tanh(val);
	}

	public static  double sigmoid(double val) {
		return 1 / (1 + Math.pow(Math.E, -val));
	}

	public static double sigmoidDerivative(double val) {
		return sigmoid(val) * (1 - sigmoid(val));
	}

	public static double tanhDerivative(double val) {
		return 1 - Math.pow(tanh(val), 2);
	}

}
