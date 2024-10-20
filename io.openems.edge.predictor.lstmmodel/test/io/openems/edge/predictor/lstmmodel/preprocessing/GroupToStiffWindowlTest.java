package io.openems.edge.predictor.lstmmodel.preprocessing;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.GroupToStiffWindowPipe;

public class GroupToStiffWindowlTest {
	@Test
	public void testGroupToStiffedWindow() {
		ArrayList<Double> inputValues = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0));
		int windowSize = 2;

		double[][] result = GroupToStiffWindowPipe.groupToStiffedWindow(inputValues, windowSize);

		double[][] expected = { { 1.0, 2.0 }, { 4.0, 5.0 } };
		assertArrayEquals("Windowing is incorrect", expected, result);
	}

	@Test
	public void testGroupToStiffedWindow1() {
		ArrayList<Double> inputValues = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0));
		int windowSize = 2;

		double[][] result = GroupToStiffWindowPipe.groupToStiffedWindow(inputValues, windowSize);
		// double[][] resultX =
		// GroupToStiffWindowPipe.groupToStiffedWindowX(inputValues, windowSize);

		double[][] expected = { { 1.0, 2.0 }, { 4.0, 5.0 }, { 7.0, 8.0 } };
		assertArrayEquals("Windowing is incorrect", expected, result);
	}

	@Test
	public void testGroupToStiffedWindowWithInvalidSize() {
		ArrayList<Double> inputValues = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0));
		int windowSize = 7;

		IllegalArgumentException exception = assertThrows(//
				IllegalArgumentException.class, () -> {
					GroupToStiffWindowPipe.groupToStiffedWindow(inputValues, windowSize);
				});

		assertEquals("Invalid window size", exception.getMessage());
	}

	@Test
	public void testGroupToStiffedTarget() {
		ArrayList<Double> inputValues = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0));
		int windowSize = 2;

		double[] result = GroupToStiffWindowPipe.groupToStiffedTarget(inputValues, windowSize);

		double[] expected = { 3.0, 6.0 };
		assertArrayEquals(expected, result, 0.001);
	}

	@Test
	public void testGroupToStiffedTargetWithInvalidSize() {
		ArrayList<Double> inputValues = new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0));
		int windowSize = 7;

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			GroupToStiffWindowPipe.groupToStiffedTarget(inputValues, windowSize);
		});

		assertEquals("Invalid window size", exception.getMessage());
	}

}
