package io.openems.edge.edge2edge.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.edge.edge2edge.ess.Edge2EdgeEssImpl;

public class AbstractEdge2EdgeTest {

	@Test
	public void testIsHashEqual() {
		assertTrue(Edge2EdgeEssImpl.isHashEqual(0x6201, "OpenEMS"));
		assertTrue(Edge2EdgeEssImpl.isHashEqual(0xb3dc, "OpenemsComponent"));
		assertFalse(Edge2EdgeEssImpl.isHashEqual(null, "_sum"));
		assertFalse(Edge2EdgeEssImpl.isHashEqual(0x6201, "foobar"));
	}
}
