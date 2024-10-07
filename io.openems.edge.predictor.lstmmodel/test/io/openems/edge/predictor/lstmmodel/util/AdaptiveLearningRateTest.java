package io.openems.edge.predictor.lstmmodel.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;

public class AdaptiveLearningRateTest {

	@Test
	public void scheduleTest() {
		AdaptiveLearningRate obj = new AdaptiveLearningRate();
		HyperParameters hyperParameter = new HyperParameters();

		hyperParameter.setLearningRateUpperLimit(0.2);
		hyperParameter.setLearningRateLowerLimit(0.05);
		hyperParameter.setEpochTrack(10);

		double perc = (double) hyperParameter.getEpochTrack() / hyperParameter.getEpoch();
		double lr = obj.scheduler(hyperParameter);
		double val = hyperParameter.getLearningRateLowerLimit()
				+ 0.5 * (hyperParameter.getLearningRateUpperLimit() - hyperParameter.getLearningRateLowerLimit())
						* (1 + Math.cos(perc * Math.PI));
		assertEquals(lr, val, 0.0001);

	}

	@Test

	public void adagradOptimizerTest() {
		AdaptiveLearningRate obj = new AdaptiveLearningRate();
		double globalLearningRate = 0.001;
		double localLearningRate = 0.1;

		// Test case; i = 0, gradient =0
		int i = 0;
		double gradient = 0.0;
		double lr = obj.adagradOptimizer(globalLearningRate, localLearningRate, gradient, i);
		assertEquals(lr, globalLearningRate, 0.0001);

		// Test Case i>0 gradient =! 0
		i = 0;
		gradient = 10;
		lr = obj.adagradOptimizer(globalLearningRate, localLearningRate, gradient, i);
		// double expected = globalLearningRate / gradient;
		assertEquals(lr, globalLearningRate, 0.0001);

		// Test Case 3 i > 0 gradient = 0 , local learning rate = 0
		i = 1;
		gradient = 0;
		localLearningRate = 0;
		lr = obj.adagradOptimizer(globalLearningRate, localLearningRate, gradient, i);
		assertEquals(lr, globalLearningRate, 0.0001);

		// Test Case 3 i > 0 gradient = 0 , local learning rate =! 0

		i = 1;
		gradient = 0;
		localLearningRate = 0.001;
		lr = obj.adagradOptimizer(globalLearningRate, localLearningRate, gradient, i);
		double temp1 = globalLearningRate / localLearningRate;
		double temp2 = Math.pow(temp1, 2);
		double temp3 = temp2 + Math.pow(gradient, 2);
		double expected = globalLearningRate / Math.pow(temp3, 0.5);
		assertEquals(lr, expected, 0.0001);

	}

	@Test
	// To test if the learning rate decreases with epoch

	public void adagradTestWithScheduler() {
		HyperParameters hyperParameters = new HyperParameters();
		AdaptiveLearningRate obj = new AdaptiveLearningRate();
		double localLearningRate = 0.1;
		double globalLearningRate = 0.1;
		double gradient = 1;
		double previousLocalLearningRate = localLearningRate;
		double previousGlobalLearningRate = globalLearningRate;
		hyperParameters.setLearningRateUpperLimit(0.1);
		hyperParameters.setLearningRateLowerLimit(0.0005);
		for (int i = 1; i < hyperParameters.getEpoch(); i++) {
			hyperParameters.setEpochTrack(i);
			localLearningRate = obj.scheduler(hyperParameters);
			// System.out.println("Local Learning Rate: " + localLearningRate);
			gradient = gradient + gradient * (hyperParameters.getEpochTrack() / hyperParameters.getEpoch());
			localLearningRate = obj.adagradOptimizer(globalLearningRate, localLearningRate, gradient, i);
			globalLearningRate = localLearningRate;
			assert (previousLocalLearningRate > localLearningRate);
			assert (previousGlobalLearningRate > globalLearningRate);

			previousLocalLearningRate = localLearningRate;
			previousGlobalLearningRate = globalLearningRate;

		}

	}

}
