package io.openems.edge.predictor.lstmmodel.util;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;

public class AdaptiveLearningRate {
	/**
	 * Adjusts the learning rate based on the given percentage. * the total
	 * iterations.
	 * 
	 * @param hyperParameters An instance of class HyperParameter
	 * @return The adapted learning rate calculated using a cosine annealing
	 *         strategy.
	 */

	public double scheduler(HyperParameters hyperParameters) {
		var maximum = hyperParameters.getLearningRateUpperLimit();
		var minimum = hyperParameters.getLearningRateLowerLimit();
		var tCurByTmax = (double) hyperParameters.getEpochTrack() / hyperParameters.getEpoch();
		var cosineValue = Math.cos(tCurByTmax * Math.PI);
		return (minimum + 0.5 * (maximum - minimum) * (1 + cosineValue));
	}

	/**
	 * Performs the Adagrad optimization step to adjust the learning rate based on
	 * the gradient information.
	 *
	 * @param globalLearningRate The global learning rate for the optimization
	 *                           process.
	 * @param localLearningRate  The local learning rate, which is dynamically
	 *                           adjusted during the optimization.
	 * @param gradient           The gradient value computed during the
	 *                           optimization.
	 * @param iteration          The iteration number, used to determine if this is
	 *                           the first iteration.
	 * @return The adapted learning rate based on the Adagrad optimization strategy.
	 */
	public double adagradOptimizer(double globalLearningRate, double localLearningRate, double gradient,
			int iteration) {
		if (iteration == 0 || localLearningRate == 0 || (globalLearningRate == 0 && gradient == 0)) {
			return globalLearningRate;
		}

		double adjustedRate = Math.pow(globalLearningRate / localLearningRate, 2) //
				+ Math.pow(gradient, 2);
		return globalLearningRate / Math.sqrt(adjustedRate);
	}
}
