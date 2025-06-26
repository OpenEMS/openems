package io.openems.edge.io.revpi;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;

public class IoRevolutionPiDigitalIoImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new IoRevolutionPiDigitalIoImpl()) //
		// .activate(MyConfig.create() //
		// .setId(COMPONENT_ID) //
		// .setInitOutputFromHardware(false) //
		// .build()) //
		;
	}

}
