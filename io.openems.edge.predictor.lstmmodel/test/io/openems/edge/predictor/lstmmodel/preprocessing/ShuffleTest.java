package io.openems.edge.predictor.lstmmodel.preprocessing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class ShuffleTest {

	@Test
	public void testShuffle() {
		double[][] originalData = { //
				{ 1.0, 2.0, 3.0 }, //
				{ 4.0, 5.0, 6.0 }, //
				{ 7.0, 8.0, 9.0 }, //
				{ 10.0, 11.0, 12.0 }, //
				{ 13.0, 14.0, 15.0 }//
		};

		double[] originalTarget = { 10.0, 20.0, 30.0, 40.0, 50.0 };

		Shuffle shuffle = new Shuffle(originalData, originalTarget);

		double[][] shuffledData = shuffle.getData();
		double[] shuffledTarget = shuffle.getTarget();

		assertNotEquals(originalData, shuffledData);
		assertNotEquals(originalTarget, shuffledTarget);

		assertEquals(originalData.length, shuffledData.length);
		assertEquals(originalTarget.length, shuffledTarget.length);

	}

}
