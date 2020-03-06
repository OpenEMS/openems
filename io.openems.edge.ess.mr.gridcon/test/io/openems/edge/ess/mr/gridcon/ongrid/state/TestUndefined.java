package io.openems.edge.ess.mr.gridcon.ongrid.state;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.edge.ess.mr.gridcon.ongrid.OnGridState;

public class TestUndefined {

	Undefined sut;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		sut = new Undefined(null, null, null, null, null);
	}

	@Test
	public final void testUndefined() {
		assertNotNull(sut);
	}

	@Test
	public final void testGetStateUndefined() {
		// without changes next state should be Undefined
		assertEquals(OnGridState.UNDEFINED, sut.getNextState());
	}

	@Test
	public final void testGetNextState() {
		assertEquals(OnGridState.UNDEFINED, sut.getState());
	}

	@Test
	public final void testAct() {
		try {
			//nothing should happen
			sut.act();
		} catch (Exception e) {
			fail();
		}
	}

}
