package io.openems.edge.controller.ess.fastfrequencyreserve;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

//CHECKSTYLE:OFF
public class simpleTest {

	@Test
	public void test() {
		int input = 92000;
		int x = (int) (input * 0.18);
		// System.out.println(y);

		int y = 16560;
		assertEquals(x, y);
	}

}
//CHECKSTYLE:ON