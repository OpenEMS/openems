package io.openems.edge.bridge.modbus.api.task;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class UtilsTest {

	@Test
	public void testToBooleanArray() {

		byte[] bs = { (byte) 0xAA, (byte) 0xAA };

		var bools = Utils.toBooleanArray(bs);

		assertEquals(true, bools[0]);
		assertEquals(false, bools[1]);
		assertEquals(true, bools[2]);
		assertEquals(false, bools[3]);
		assertEquals(true, bools[4]);
		assertEquals(false, bools[5]);
		assertEquals(true, bools[6]);
		assertEquals(false, bools[7]);
		assertEquals(true, bools[8]);
		assertEquals(false, bools[9]);
		assertEquals(true, bools[10]);
		assertEquals(false, bools[11]);
		assertEquals(true, bools[12]);
		assertEquals(false, bools[13]);
		assertEquals(true, bools[14]);
		assertEquals(false, bools[15]);
	}

}
