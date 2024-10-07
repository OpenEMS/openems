package io.openems.edge.predictor.lstmmodel.train;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;

import org.junit.Test;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;

public class MakeModelTest {

	@Test
	public void testGenerateInitialWeightMatrixOriginal() {
		// Result should be
		// [
		// [1.0, 1.0, 1.0],
		// [1.0, 1.0, 1.0],
		// [1.0, 1.0, 1.0],
		// [-1.0, -1.0, -1.0],
		// [-1.0, -1.0, -1.0],
		// [-1.0, -1.0, -1.0],
		// [0.0, 0.0, 0.0],
		// [0.0, 0.0, 0.0]
		// ]

		int windowSize = 3;
		ArrayList<ArrayList<Double>> result = MakeModel.generateInitialWeightMatrix(windowSize,  new HyperParameters());

		assertNotNull(result);
		assertEquals(8, result.size());
		assertEquals(windowSize, result.get(0).size());
	}
}
