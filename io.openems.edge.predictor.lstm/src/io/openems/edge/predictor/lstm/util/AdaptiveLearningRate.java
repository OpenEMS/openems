package io.openems.edge.predictor.lstm.util;

public class AdaptiveLearningRate {

	public double scheduler(double perc) {

		double learningRate = 1.0;

		if (perc < 5) {

			learningRate = learningRate / 10000;
		} else if (5 < perc && perc < 30) {
			learningRate = learningRate / 10000;
		} else if (30 < perc && perc < 45) {
			learningRate = learningRate / 40000;
		} else if (45 < perc && perc < 60) {
			learningRate = learningRate / 40000;
		} else if (60 < perc && perc < 75) {
			learningRate = learningRate / 40000;
		} else {
			learningRate = learningRate / 40000;
		}
		return learningRate;

	}

	/**
	 * The idea is to record all the gradients---------> sum the square of
	 * gradients---> divide a global learning rate by square root of this sum Here,
	 * we will not record the gradient but compute the squared sum of gradient by
	 * dividing the global learning rate by previous local learning rate and square
	 * this ratio. learning rate=globalLearningRate/(sqrt(i**2) for i in gradients)
	 * we need current gradient, current learning rate,and global learning Rate
	 * 
	 * @param globalLearningRate
	 * @param localLearningRate
	 * @param gradient
	 * @param i
	 * @return
	 */
	double adagradOptimizer(double globalLearningRate, double localLearningRate, double gradient, int i) {

		if (i == 0) {
			localLearningRate = globalLearningRate / Math.pow(Math.pow(gradient, 2), 0.5);
			if (Math.pow(Math.pow(gradient, 2), 0.5) == 0) {
				// System.out.println("G");
				return globalLearningRate;
			}
			// System.out.println("l");
			return localLearningRate;

		}

		else {
			double temp1 = globalLearningRate / localLearningRate;
			double temp2 = Math.pow(temp1, 2);
			double temp3 = temp2 + Math.pow(gradient, 2);
			if (localLearningRate == 0) {
				// System.out.println("G");

				return globalLearningRate;

			}
			localLearningRate = globalLearningRate / Math.pow(temp3, 0.5);
			if (temp3 < 0) {
				// System.out.println("G");
				return globalLearningRate;
			}

			// System.out.println("l");
			return localLearningRate;
		}

	}
}