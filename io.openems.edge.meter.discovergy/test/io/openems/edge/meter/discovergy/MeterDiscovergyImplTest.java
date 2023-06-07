package io.openems.edge.meter.discovergy;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.meter.api.MeterType;

public class MeterDiscovergyImplTest {

	private static final String COMPONENT_ID = "meter0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new MeterDiscovergyImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setType(MeterType.GRID) //
						.setPassword("xxx") //
						.setEmail("x@y.z") //
						.setSerialNumber("12345678") //
						.setFullSerialNumber("1ESY1234567890") //
						.setMeterId("0123456789ABCDEF0123456789ABCDEF") //
						.build()) //
		;
	}
}
