package io.openems.edge.ess.mr.gridcon.ongrid.state;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import io.openems.edge.ess.mr.gridcon.state.gridconstate.GridconState;
import io.openems.edge.ess.mr.gridcon.state.gridconstate.Undefined;

public class TestUndefined {

	private static Undefined sut;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		sut = new Undefined(null, null, null, null, null, null);
	}

	@Test
	public final void testUndefined() {
		assertNotNull(sut);
	}

	@Test
	public final void testGetStateUndefined() {
	}

	@Test
	public final void testGetNextState() {
		// without changes next state should be Undefined
		assertEquals(GridconState.UNDEFINED, sut.getNextState());

	}

	@Test
	public final void testAct() {
		try {
			// nothing should happen
			sut.act(null);
		} catch (Exception e) {
			fail();
		}
	}

}
