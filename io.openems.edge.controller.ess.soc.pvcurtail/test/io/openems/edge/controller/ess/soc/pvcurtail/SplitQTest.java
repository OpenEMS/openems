package io.openems.edge.controller.ess.soc.pvcurtail;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

public class SplitQTest {

	@Test
	public void testOnePcs() {
		assertArrayEquals(new int[] { 400 }, PvCurtailBySocImpl.splitQ(400, new int[] { 2_000_000 }));
	}

	@Test
	public void testEqualPcs() {
		assertArrayEquals(new int[] { 200, 200 }, PvCurtailBySocImpl.splitQ(400, new int[] { 2_000_000, 2_000_000 }));
		// odd total: the parts still add up
		assertArrayEquals(new int[] { -50, -51 }, PvCurtailBySocImpl.splitQ(-101, new int[] { 2_000_000, 2_000_000 }));
	}

	@Test
	public void testByRating() {
		assertArrayEquals(new int[] { 100, 200, 300 },
				PvCurtailBySocImpl.splitQ(600, new int[] { 1_000_000, 2_000_000, 3_000_000 }));
	}

	@Test
	public void testUnavailablePcs() {
		// a PCS that is not available (weight 0) gets nothing, the other takes all
		assertArrayEquals(new int[] { 0, 400 }, PvCurtailBySocImpl.splitQ(400, new int[] { 0, 2_000_000 }));
		assertArrayEquals(new int[] { 0, 0 }, PvCurtailBySocImpl.splitQ(400, new int[] { 0, 0 }));
	}
}
