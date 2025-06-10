package io.openems.edge.bridge.modbus.api.task;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ghgande.j2mod.modbus.util.BitVector;

public class AbstractReadDigitalInputsTaskTest {

	@Test
	public void testToBooleanArray() {
		var bv = new BitVector(5);
		bv.setBit(0, false);
		bv.setBit(1, true);
		bv.setBit(2, false);
		bv.setBit(3, true);
		bv.setBit(4, false);

		var bools = AbstractReadDigitalInputsTask.toBooleanArray(bv);
		assertEquals(false, bools[0]);
		assertEquals(true, bools[1]);
		assertEquals(false, bools[2]);
		assertEquals(true, bools[3]);
		assertEquals(false, bools[4]);
	}
}
