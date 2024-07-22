package io.openems.edge.predictor.lstmmodel.validation;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.validator.ValidationSeasonalityModel;

public class FindOptimumIndexTest {

	/**
	 * testFindOptimumIndex.
	 */
	// @Test
	public void testFindOptimumIndex() {
		ArrayList<ArrayList<Double>> matrix = new ArrayList<>(//
				Arrays.asList(//
						new ArrayList<>(Arrays.asList(1.0, 2.0, 7.0)), //
						new ArrayList<>(Arrays.asList(4.0, 5.0, 8.0)), //
						new ArrayList<>(Arrays.asList(7.0, 8.0, 6.0))//
				)//
		);

		List<List<Integer>> result = ValidationSeasonalityModel.findOptimumIndex(matrix, "Test", new HyperParameters());

		assertEquals(Arrays.asList(Arrays.asList(2, 0), Arrays.asList(2, 1), Arrays.asList(1, 2)), result);
	}

}
