package io.openems.edge.predictor.lstm.util;

import io.openems.edge.predictor.lstm.common.HyperParameters;

//import io.openems.edge.predictor.lstm.common.HyperParameters;

public class AdaptiveLearningRate {
	/**
	 * Adjusts the learning rate based on the given percentage.
	 *
	 * @param perc            The percentage of the current iteration relative to
	 *                        the total iterations.
	 * @param hyperParameters An instance of class HyperParameter
	 * @return The adapted learning rate calculated using a cosine annealing
	 *         strategy.
	 */

	public double scheduler(double perc, HyperParameters hyperParameters) {
		double maximum = hyperParameters.getLearningRateLowerLimit();
		double minimum = hyperParameters.getLearningRateUpperLimit();
		double tCurByTmax = perc;
		double cosineValue = Math.cos(tCurByTmax * Math.PI);
		double learningRate = (minimum + 0.5 * (maximum - minimum) * (1 + cosineValue));
		return learningRate;

	}

	/**
	 * Performs the Adagrad optimization step to adjust the learning rate based on
	 * the gradient information.
	 *
	 * @param globalLearningRate The global learning rate for the optimization
	 *                           process.
	 * @param localLearningRate  The local learning rate, which is dynamically
	 *                           adjusted during the optimization at previous
	 *                           iteration.
	 * @param gradient           The gradient value computed during the
	 *                           optimization.
	 * @param i                  The iteration number, used to determine if this is
	 *                           the first iteration.
	 * @return The adapted learning rate based on the Adagrad optimization strategy.
	 */
	double adagradOptimizer(double globalLearningRate, double localLearningRate, double gradient, int i) {

		if (i == 0) {
			localLearningRate = globalLearningRate / Math.pow(Math.pow(gradient, 2), 0.5);
			if (Math.pow(Math.pow(gradient, 2), 0.5) == 0) {

				return globalLearningRate;

			}

			return localLearningRate;

		} else {
			double temp1 = globalLearningRate / localLearningRate;
			double temp2 = Math.pow(temp1, 2);
			double temp3 = temp2 + Math.pow(gradient, 2);
			if (localLearningRate == 0) {

				return localLearningRate;

			}
			localLearningRate = globalLearningRate / Math.pow(temp3, 0.5);
			if (temp3 < 0) {

				return globalLearningRate;

			}

			return localLearningRate;
		}

	}

}