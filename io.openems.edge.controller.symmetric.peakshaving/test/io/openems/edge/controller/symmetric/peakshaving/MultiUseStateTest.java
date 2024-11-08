package io.openems.edge.controller.symmetric.peakshaving;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MultiUseStateTest {
	
	@Test
	public void testCorrectTransitionsFromNone() {
		assertEquals(MultiUseState.NONE, MultiUseState.NONE.getMultiUseBehavior(60, 80, 0));
		assertEquals(MultiUseState.NONE, MultiUseState.NONE.getMultiUseBehavior(79, 80, 0));
		assertEquals(MultiUseState.NONE, MultiUseState.NONE.getMultiUseBehavior(79, 78, 2));
		
		assertEquals(MultiUseState.PARALLEL, MultiUseState.NONE.getMultiUseBehavior(80, 80, 0));
		assertEquals(MultiUseState.PARALLEL, MultiUseState.NONE.getMultiUseBehavior(81, 80, 0));
		assertEquals(MultiUseState.PARALLEL, MultiUseState.NONE.getMultiUseBehavior(81, 78, 2));
		assertEquals(MultiUseState.PARALLEL, MultiUseState.NONE.getMultiUseBehavior(90, 80, 0));
	}
	
	@Test
	public void testCorrectTransitionsFromParallel() {
		assertEquals(MultiUseState.NONE, MultiUseState.PARALLEL.getMultiUseBehavior(60, 80, 0));
		assertEquals(MultiUseState.NONE, MultiUseState.PARALLEL.getMultiUseBehavior(79, 80, 0));
		assertEquals(MultiUseState.NONE, MultiUseState.PARALLEL.getMultiUseBehavior(80, 80, 0));
		
		assertEquals(MultiUseState.PARALLEL, MultiUseState.PARALLEL.getMultiUseBehavior(80, 78, 2));
		assertEquals(MultiUseState.PARALLEL, MultiUseState.PARALLEL.getMultiUseBehavior(79, 78, 2));
		assertEquals(MultiUseState.PARALLEL, MultiUseState.PARALLEL.getMultiUseBehavior(80, 78, 2));
		assertEquals(MultiUseState.PARALLEL, MultiUseState.PARALLEL.getMultiUseBehavior(90, 80, 0));
	}
}
